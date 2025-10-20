package click.dailyfeed.content.domain.comment.service;

import click.dailyfeed.code.domain.activity.type.MemberActivityType;
import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.content.comment.exception.*;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.global.kafka.exception.KafkaDLQRedisNetworkErrorException;
import click.dailyfeed.content.domain.comment.document.CommentDocument;
import click.dailyfeed.content.domain.comment.document.CommentLikeDocument;
import click.dailyfeed.content.domain.comment.entity.Comment;
import click.dailyfeed.content.domain.comment.mapper.CommentMapper;
import click.dailyfeed.content.domain.comment.repository.jpa.CommentRepository;
import click.dailyfeed.content.domain.comment.repository.mongo.CommentLikeMongoRepository;
import click.dailyfeed.content.domain.comment.repository.mongo.CommentMongoRepository;
import click.dailyfeed.content.domain.post.entity.Post;
import click.dailyfeed.content.domain.post.repository.jpa.PostRepository;
import click.dailyfeed.content.domain.redisdlq.document.RedisDLQDocument;
import click.dailyfeed.content.domain.redisdlq.repository.mongo.RedisDLQRepository;
import click.dailyfeed.feign.domain.member.MemberFeignHelper;
import click.dailyfeed.kafka.domain.activity.publisher.MemberActivityKafkaPublisher;
<<<<<<< Updated upstream
=======
import click.dailyfeed.pvc.domain.kafka.service.KafkaPublisherFailureStorageService;
>>>>>>> Stashed changes
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentMongoRepository commentMongoRepository;
    private final CommentLikeMongoRepository commentLikeMongoRepository;
    private final RedisDLQRepository redisDLQRepository;

    private final CommentMapper commentMapper;
    private final MemberFeignHelper memberFeignHelper;
    private final MemberActivityKafkaPublisher memberActivityKafkaPublisher;

<<<<<<< Updated upstream
    private static final int MAX_COMMENT_DEPTH = 2; // 최대 댓글 깊이 제한
=======
    private final KafkaPublisherFailureStorageService kafkaPublisherFailureStorageService;

    private static final int MAX_COMMENT_DEPTH = CommentProperties.MAX_COMMENT_DEPTH; // 최대 댓글 깊이 제한
>>>>>>> Stashed changes

    // 댓글 작성
    public CommentDto.Comment createComment(MemberProfileDto.Summary member, String token, CommentDto.CreateCommentRequest request, HttpServletResponse httpResponse) {
        Long authorId = member.getId();

        // 게시글 존재 확인
        Post post = getPostByIdOrThrow(request.getPostId());

        Comment comment = click.dailyfeed.content.domain.comment.entity.Comment.commentBuilder()
                    .content(request.getContent())
                    .authorId(authorId)
                    .post(post)
                    .build();

        Comment savedComment = commentRepository.save(comment);

        // 응답 생성 및 작성자 정보 추가
        CommentDto.Comment commentDto = commentMapper.fromCommentNonRecursive(savedComment, member);

        // mongodb 에 본문 저장
        insertNewDocument(post, savedComment);

        try {
            // 멤버 활동 기록 조회를 위한 활동 기록 이벤트 발행
            memberActivityKafkaPublisher.publishCommentCUDEvent(member.getId(), comment.getPost().getId(), comment.getId(), MemberActivityType.COMMENT_CREATE);
        }
        catch (KafkaDLQRedisNetworkErrorException redisDlqException){
            RedisDLQDocument redisDLQDocument = RedisDLQDocument.newRedisDLQ(redisDlqException.getRedisKey(), redisDlqException.getPayload());
            redisDLQRepository.save(redisDLQDocument);
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

        try {
            // 멤버 활동 기록 조회를 위한 활동 기록 이벤트 발행
            memberActivityKafkaPublisher.publishCommentCUDEvent(member.getId(), comment.getPost().getId(), commentId, MemberActivityType.COMMENT_UPDATE);
        }
        catch (KafkaDLQRedisNetworkErrorException redisDlqException){
            RedisDLQDocument redisDLQDocument = RedisDLQDocument.newRedisDLQ(redisDlqException.getRedisKey(), redisDlqException.getPayload());
            redisDLQRepository.save(redisDLQDocument);
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

        // 댓글과 모든 자식 댓글들을 소프트 삭제 (TODO : Season2)
        commentRepository.softDeleteCommentAndChildren(commentId);
        deleteDocument(comment);

        try {
            // 멤버 활동 기록 조회를 위한 활동 기록 이벤트 발행
            memberActivityKafkaPublisher.publishCommentCUDEvent(requestedMember.getId(), comment.getPost().getId(), commentId, MemberActivityType.COMMENT_DELETE);
        }
        catch (KafkaDLQRedisNetworkErrorException redisDlqException){
            RedisDLQDocument redisDLQDocument = RedisDLQDocument.newRedisDLQ(redisDlqException.getRedisKey(), redisDlqException.getPayload());
            redisDLQRepository.save(redisDLQDocument);
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

    /// helpers

    // 좋아요 증가
    public Boolean incrementLikeCount(MemberDto.Member member, Long commentId) {
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
        try {
            // 멤버 활동 기록 조회를 위한 활동 기록 이벤트 발행
            memberActivityKafkaPublisher.publishCommentLikeEvent(member.getId(), comment.getPost().getId(), commentId, MemberActivityType.LIKE_COMMENT);
        }
        catch (KafkaDLQRedisNetworkErrorException redisDlqException){
            RedisDLQDocument redisDLQDocument = RedisDLQDocument.newRedisDLQ(redisDlqException.getRedisKey(), redisDlqException.getPayload());
            redisDLQRepository.save(redisDLQDocument);
        }
        catch (Exception e) {
            RedisDLQDocument redisDLQDocument = RedisDLQDocument.newRedisDLQ("1", "asdfa");
            redisDLQRepository.save(redisDLQDocument);
        }

        return Boolean.TRUE;
    }

    // 좋아요 감소
    public void decrementLikeCount(MemberDto.Member member, Long commentId) {
        // 댓글 존재 확인
        Comment comment = commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(CommentNotFoundException::new);

        CommentLikeDocument existDocument = commentLikeMongoRepository.findByCommentPkAndMemberId(commentId, member.getId());
        if (existDocument == null) {
            throw new CommentLikeAlreadyExistsException();
        }
        commentLikeMongoRepository.delete(existDocument);

        try {
            // 멤버 활동 기록 조회를 위한 활동 기록 이벤트 발행
            memberActivityKafkaPublisher.publishCommentLikeEvent(member.getId(), comment.getPost().getId(), commentId, MemberActivityType.LIKE_COMMENT_CANCEL);
        }
        catch (KafkaDLQRedisNetworkErrorException redisDlqException){
            RedisDLQDocument redisDLQDocument = RedisDLQDocument.newRedisDLQ(redisDlqException.getRedisKey(), redisDlqException.getPayload());
            redisDLQRepository.save(redisDLQDocument);
        }
    }


    ///  helpers ///
    /// 글 조회
    public Post getPostByIdOrThrow(Long postId) {
        return postRepository.findByIdAndNotDeleted(postId).orElseThrow(PostNotFoundException::new);
    }

    public CommentDto.Comment createReply(MemberProfileDto.Summary member, String authorizationHeader, CommentDto.@Valid CreateCommentRequest request, HttpServletResponse httpResponse) {
        Long authorId = member.getId();
        // 게시글 존재 확인
        Post post = getPostByIdOrThrow(request.getPostId());
        // 생성하려는 대댓글의 부모 댓글 확인
        Comment parentComment = commentRepository.findByIdAndNotDeleted(request.getParentId())
                .orElseThrow(ParentCommentNotFoundException::new);

        // 댓글 깊이 제한 확인
//        if (parentComment.getDepth() >= MAX_COMMENT_DEPTH) {
//            throw new CommentDepthLimitExceedsException();
//        }

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

        try {
            // 멤버 활동 기록 조회를 위한 활동 기록 이벤트 발행
            memberActivityKafkaPublisher.publishCommentCUDEvent(member.getId(), comment.getPost().getId(), comment.getId(), MemberActivityType.COMMENT_CREATE);
        }
        catch (KafkaDLQRedisNetworkErrorException redisDlqException){
            RedisDLQDocument redisDLQDocument = RedisDLQDocument.newRedisDLQ(redisDlqException.getRedisKey(), redisDlqException.getPayload());
            redisDLQRepository.save(redisDLQDocument);
        }

        return commentDto;

    }

    public void insertNewReplyDocument(Post post, Comment parentComment, Comment comment) {
        CommentDocument document = CommentDocument.newReplyDocument(
                post.getId(), parentComment.getId(), comment.getId(), comment.getContent(), comment.getCreatedAt(), comment.getUpdatedAt());
        commentMongoRepository.save(document);
    }
<<<<<<< Updated upstream
=======

    public void handleRedisDLQException(KafkaDLQRedisNetworkErrorException redisDlqException, MemberActivityType memberActivityType){
        try {
            RedisDLQDocument redisDLQDocument = RedisDLQDocument.newRedisDLQ(redisDlqException.getRedisKey(), redisDlqException.getPayload());
            redisDLQRepository.save(redisDLQDocument);
        }
        catch (Exception e) {
            try{
                kafkaPublisherFailureStorageService.store(ServiceType.MEMBER_ACTIVITY.name(), memberActivityType.getCode(), redisDlqException.getRedisKey(), redisDlqException.getPayload());
            }
            catch (Exception finalException){
                // PVC 저장까지 실패할 경우 트랜잭션을 실패시키는 것으로 처리
                throw new KafkaNetworkErrorException();
            }
        }
    }
>>>>>>> Stashed changes
}
