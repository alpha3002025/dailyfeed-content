package click.dailyfeed.content.domain.comment.service;

import click.dailyfeed.code.domain.activity.dto.MemberActivityDto;
import click.dailyfeed.code.domain.activity.type.MemberActivityType;
import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.content.comment.exception.*;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.global.feign.exception.FeignApiCommunicationFailException;
import click.dailyfeed.code.global.kafka.exception.KafkaNetworkErrorException;
import click.dailyfeed.code.global.system.properties.CommentProperties;
import click.dailyfeed.code.global.system.type.PublishType;
import click.dailyfeed.code.global.web.excecption.DailyfeedWebTooManyRequestException;
import click.dailyfeed.content.domain.comment.document.CommentDocument;
import click.dailyfeed.content.domain.comment.document.CommentLikeDocument;
import click.dailyfeed.content.domain.comment.entity.Comment;
import click.dailyfeed.content.domain.comment.mapper.CommentMapper;
import click.dailyfeed.content.domain.comment.repository.jpa.CommentRepository;
import click.dailyfeed.content.domain.comment.repository.mongo.CommentLikeMongoRepository;
import click.dailyfeed.content.domain.comment.repository.mongo.CommentMongoRepository;
import click.dailyfeed.content.domain.post.entity.Post;
import click.dailyfeed.content.domain.post.repository.jpa.PostRepository;
import click.dailyfeed.deadletter.domain.deadletter.service.FeignDeadLetterService;
import click.dailyfeed.deadletter.domain.deadletter.service.KafkaPublisherDeadLetterService;
import click.dailyfeed.feign.domain.activity.MemberActivityFeignHelper;
import click.dailyfeed.feign.domain.member.MemberFeignHelper;
import click.dailyfeed.kafka.domain.activity.publisher.MemberActivityKafkaPublisher;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentMongoRepository commentMongoRepository;
    private final CommentLikeMongoRepository commentLikeMongoRepository;

    private final FeignDeadLetterService feignDeadLetterService;
    private final KafkaPublisherDeadLetterService kafkaPublisherDeadLetterService;

    private final CommentMapper commentMapper;
    private final MemberFeignHelper memberFeignHelper;
    private final MemberActivityFeignHelper memberActivityFeignHelper;
    private final MemberActivityKafkaPublisher memberActivityKafkaPublisher;

    private static final int MAX_COMMENT_DEPTH = CommentProperties.MAX_COMMENT_DEPTH; // 최대 댓글 깊이 제한

    @Value("${dailyfeed.services.content.publish-type.comment-service}")
    private String publishType;

    // 댓글 작성
    public CommentDto.Comment createComment(MemberProfileDto.Summary member, String token, CommentDto.CreateCommentRequest request, HttpServletResponse httpResponse) {
        Long authorId = member.getId();

        // 게시글 존재 확인
        Post post = getPostByIdOrThrow(request.getPostId());

        Comment comment = Comment.commentBuilder()
                    .content(request.getContent())
                    .authorId(authorId)
                    .post(post)
                    .build();

        Comment savedComment = commentRepository.save(comment);

        // 응답 생성 및 작성자 정보 추가
        CommentDto.Comment commentDto = commentMapper.fromCommentNonRecursive(savedComment, member);

        // mongodb 에 본문 저장
        insertNewDocument(post, savedComment);

        if (PublishType.KAFKA.getCode().equals(publishType)) { /// kafka 를 사용할 경우 (케이스 A)
            kafkaPublishCommentEvent(member.getId(), comment, MemberActivityType.COMMENT_CREATE);
        } else { /// feign 을 사용할 경우 (케이스 B)
            feignPublishCommentEvent(member.getId(), comment, MemberActivityType.COMMENT_CREATE, token, httpResponse);
        }

        return commentDto;
    }

    public void insertNewDocument(Post post, Comment comment){
        CommentDocument document = CommentDocument
                .newComment(post.getId(), comment.getId(), comment.getContent(), comment.getCreatedAt(), comment.getUpdatedAt());
        commentMongoRepository.save(document);
    }

    // 댓글 수정
    public CommentDto.Comment updateComment(MemberDto.Member member, Long commentId, CommentDto.UpdateCommentRequest request, String token, HttpServletResponse httpResponse) {
        MemberProfileDto.Summary author = memberFeignHelper.getMemberSummaryById(member.getId(), token, httpResponse);
        Long authorId = author.getId();

        Comment comment = commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(CommentNotFoundException::new);

        // 작성자 권한 확인
        if (!comment.isOwnedBy(authorId)) {
            throw new CommentModificationPermissionDeniedException();
        }

        // 수정
        comment.updateContent(request.getContent());
        Comment updatedComment = commentRepository.save(comment);

        updateDocument(comment);

        // 응답 생성 및 작성자 정보 추가
        CommentDto.Comment commentUpdated = commentMapper.fromCommentNonRecursive(updatedComment, author);

        if (PublishType.KAFKA.getCode().equals(publishType)) { /// kafka 를 사용할 경우 (케이스 A)
            kafkaPublishCommentEvent(author.getId(), comment, MemberActivityType.COMMENT_UPDATE);
        } else { /// feign 을 사용할 경우 (케이스 B)
            feignPublishCommentEvent(author.getId(), comment, MemberActivityType.COMMENT_UPDATE, token, httpResponse);
        }

        // mongodb 에 본문 저장
        return commentUpdated;
    }

    // 본문 검색 용도의 컬렉션 'comments' 에 저장
    public void updateDocument(Comment comment){
        CommentDocument oldDocument = commentMongoRepository
                .findByCommentPkAndIsDeleted(comment.getId(), Boolean.FALSE)
                .orElseThrow(CommentNotFoundException::new);

        CommentDocument updatedPost = CommentDocument.newUpdatedComment(oldDocument, comment.getUpdatedAt());
        commentMongoRepository.delete(oldDocument);
        commentMongoRepository.save(updatedPost);
    }

    // 댓글 삭제 (소프트 삭제)
    public Boolean deleteComment(MemberDto.Member requestedMember, Long commentId, String token, HttpServletResponse httpResponse) {
        Long authorId = requestedMember.getId();

        Comment comment = commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(CommentNotFoundException::new);

        // 작성자 권한 확인
        if (!comment.isOwnedBy(authorId)) {
            throw new CommentDeletionPermissionDeniedException();
        }

        // 댓글과 모든 자식 댓글들을 소프트 삭제
        commentRepository.softDeleteCommentAndChildren(commentId);
        deleteDocument(comment);

        if (PublishType.KAFKA.getCode().equals(publishType)) { /// kafka 를 사용할 경우 (케이스 A)
            kafkaPublishCommentEvent(authorId, comment, MemberActivityType.COMMENT_DELETE);
        } else { /// feign 을 사용할 경우 (케이스 B)
            feignPublishCommentEvent(authorId, comment, MemberActivityType.COMMENT_DELETE, token, httpResponse);
        }

        return Boolean.TRUE;
    }

    // 본문 검색 용도의 컬렉션 'comments' 에 저장
    public void deleteDocument(Comment comment){
        CommentDocument document = commentMongoRepository
                .findByCommentPkAndIsDeleted(comment.getId(), Boolean.FALSE)
                .orElseThrow(CommentNotFoundException::new);
        commentMongoRepository.delete(document);
    }

    public void feignPublishCommentEvent(Long memberId, Comment comment, MemberActivityType activityType, String token, HttpServletResponse httpResponse) {
        MemberActivityDto.CommentActivityRequest feignRequest = commentMapper.commentActivityFeignRequest(memberId, comment.getPost().getId(), comment.getId(), activityType);
        try {
            memberActivityFeignHelper.createCommentsMemberActivity(feignRequest, token, httpResponse);
        } catch (DailyfeedWebTooManyRequestException e){
            throw new DailyfeedWebTooManyRequestException();
        } catch (Exception e) {
            try {
                feignDeadLetterService.createCommentActivityDeadLetter(feignRequest);
            } catch (Exception e1) {
                throw new FeignApiCommunicationFailException();
            }
        }
    }

    public void kafkaPublishCommentEvent(Long memberId, Comment comment, MemberActivityType activityType) {
        try {
            // 멤버 활동 기록 조회를 위한 활동 기록 이벤트 발행
            memberActivityKafkaPublisher.publishCommentCUDEvent(memberId, comment.getPost().getId(), comment.getId(), activityType);
        } catch (DailyfeedWebTooManyRequestException e){
            throw new DailyfeedWebTooManyRequestException();
        } catch (Exception e){
            MemberActivityDto.CommentActivityRequest activityRequest = MemberActivityDto.CommentActivityRequest.builder()
                    .memberId(memberId)
                    .postId(comment.getPost().getId())
                    .commentId(comment.getId())
                    .activityType(activityType)
                    .build();
            try {
                kafkaPublisherDeadLetterService.createCommentActivityDeadLetter(activityRequest);
            } catch (Exception e1){
                throw new KafkaNetworkErrorException();
            }
        }
    }

    // 좋아요 증가
    public Boolean incrementLikeCount(MemberDto.Member member, Long commentId, String token, HttpServletResponse httpResponse) {
        // 댓글 존재 확인
        Comment comment = commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(CommentNotFoundException::new);

        CommentLikeDocument existDocument = commentLikeMongoRepository.findByCommentPkAndMemberId(commentId, member.getId());
        if (existDocument != null) {
            throw new CommentLikeAlreadyExistsException();
        }

        CommentLikeDocument newDocument = CommentLikeDocument.newCommentLikeBuilder()
                .commentPk(comment.getId())
                .memberId(member.getId())
                .build();
        commentLikeMongoRepository.save(newDocument);

        if (PublishType.KAFKA.getCode().equals(publishType)) { /// kafka 를 사용할 경우 (케이스 A)
            kafkaPublishCommentLikeEvent(comment.getId(), comment, MemberActivityType.LIKE_COMMENT);
        } else { /// feign 을 사용할 경우 (케이스 B)
            feignPublishCommentLikeEvent(comment.getId(), comment, MemberActivityType.LIKE_COMMENT, token, httpResponse);
        }

        return Boolean.TRUE;
    }

    // 좋아요 감소
    public void decrementLikeCount(MemberDto.Member member, Long commentId, String token, HttpServletResponse httpResponse) {
        // 댓글 존재 확인
        Comment comment = commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(CommentNotFoundException::new);

        CommentLikeDocument existDocument = commentLikeMongoRepository.findByCommentPkAndMemberId(commentId, member.getId());
        if (existDocument == null) {
            throw new CommentLikeAlreadyExistsException();
        }
        commentLikeMongoRepository.delete(existDocument);

        if (PublishType.KAFKA.getCode().equals(publishType)) { /// kafka 를 사용할 경우 (케이스 A)
            kafkaPublishCommentLikeEvent(comment.getId(), comment, MemberActivityType.LIKE_COMMENT_CANCEL);
        } else { /// feign 을 사용할 경우 (케이스 B)
            feignPublishCommentLikeEvent(comment.getId(), comment, MemberActivityType.LIKE_COMMENT_CANCEL, token, httpResponse);
        }
    }

    public void feignPublishCommentLikeEvent(Long memberId, Comment comment, MemberActivityType activityType, String token, HttpServletResponse httpResponse) {
        MemberActivityDto.CommentLikeActivityRequest feignRequest = commentMapper.commentLikeActivityFeignRequest(memberId, comment.getPost().getId(), comment.getId(), activityType);
        try {
            memberActivityFeignHelper.createCommentLikeMemberActivity(feignRequest, token, httpResponse);
        } catch (DailyfeedWebTooManyRequestException e){
            throw new DailyfeedWebTooManyRequestException();
        } catch (Exception e) {
            try {
                feignDeadLetterService.createCommentLikeActivityDeadLetter(feignRequest);
            } catch (Exception e1) {
                throw new FeignApiCommunicationFailException();
            }
        }
    }

    public void kafkaPublishCommentLikeEvent(Long memberId, Comment comment, MemberActivityType activityType) {
        try {
            // 멤버 활동 기록 조회를 위한 활동 기록 이벤트 발행
            memberActivityKafkaPublisher.publishCommentLikeEvent(memberId, comment.getPost().getId(), comment.getId(), activityType);
        } catch (DailyfeedWebTooManyRequestException e){
            throw new DailyfeedWebTooManyRequestException();
        } catch (Exception e){
            MemberActivityDto.CommentLikeActivityRequest activityRequest = MemberActivityDto.CommentLikeActivityRequest.builder()
                    .memberId(memberId)
                    .postId(comment.getPost().getId())
                    .commentId(comment.getId())
                    .activityType(activityType)
                    .build();
            try {
                kafkaPublisherDeadLetterService.createCommentLikeActivityDeadLetter(activityRequest);
            } catch (Exception e1){
                throw new KafkaNetworkErrorException();
            }
        }
    }


    public CommentDto.Comment createReply(MemberProfileDto.Summary member, String authorizationHeader, CommentDto.@Valid CreateCommentRequest request, HttpServletResponse httpResponse) {
        Long authorId = member.getId();
        // 게시글 존재 확인
        Post post = getPostByIdOrThrow(request.getPostId());
        // 생성하려는 대댓글의 부모 댓글 확인
        Comment parentComment = commentRepository.findByIdAndNotDeleted(request.getParentId())
                .orElseThrow(ParentCommentNotFoundException::new);

        // 댓글 깊이 제한 확인
        if (parentComment.getDepth() >= MAX_COMMENT_DEPTH) {
            throw new CommentDepthLimitExceedsException();
        }

        // 부모 댓글과 같은 게시글인지 확인
        if (!parentComment.getPost().getId().equals(request.getPostId())) {
            throw new ParentCommentPostMismatchException();
        }

        Comment comment = Comment.replyCommentBuilder()
                .content(request.getContent())
                .authorId(authorId)
                .post(post)
                .build();

        parentComment.addChild(comment);
        Comment savedComment = commentRepository.save(comment);

        // 응답 생성 및 작성자 정보 추가
        CommentDto.Comment commentDto = commentMapper.fromCommentNonRecursive(savedComment, member);

        // mongodb 에 본문 저장
        insertNewDocument(post, savedComment);

        if (PublishType.KAFKA.getCode().equals(publishType)) { /// kafka 를 사용할 경우 (케이스 A)
            kafkaPublishCommentEvent(member.getId(), comment, MemberActivityType.COMMENT_CREATE);
        } else { /// feign 을 사용할 경우 (케이스 B)
            feignPublishCommentEvent(member.getId(), comment, MemberActivityType.COMMENT_CREATE, authorizationHeader, httpResponse);
        }

        return commentDto;
    }

    ///  helpers ///
    /// 글 조회
    public Post getPostByIdOrThrow(Long postId) {
        return postRepository.findByIdAndNotDeleted(postId).orElseThrow(PostNotFoundException::new);
    }
}
