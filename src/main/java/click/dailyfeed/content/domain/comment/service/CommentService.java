package click.dailyfeed.content.domain.comment.service;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.content.comment.exception.*;
import click.dailyfeed.code.domain.content.comment.type.CommentActivityType;
import click.dailyfeed.code.domain.content.comment.type.CommentLikeType;
import click.dailyfeed.code.domain.member.member.code.MemberExceptionCode;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.member.member.exception.MemberException;
import click.dailyfeed.code.global.kafka.exception.KafkaNetworkErrorException;
import click.dailyfeed.code.global.kafka.type.DateBasedTopicType;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.content.domain.comment.entity.Comment;
import click.dailyfeed.content.domain.comment.mapper.CommentEventMapper;
import click.dailyfeed.content.domain.comment.mapper.CommentMapper;
import click.dailyfeed.content.domain.comment.repository.jpa.CommentRepository;
import click.dailyfeed.content.domain.kafka.KafkaHelper;
import click.dailyfeed.content.domain.post.entity.Post;
import click.dailyfeed.content.domain.post.repository.jpa.PostRepository;
import click.dailyfeed.feign.domain.member.MemberFeignHelper;
import click.dailyfeed.pagination.mapper.PageMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentMapper commentMapper;
    private final CommentEventMapper commentEventMapper;
    private final PageMapper pageMapper;
    private final MemberFeignHelper memberFeignHelper;
    private final KafkaHelper kafkaHelper;

    private static final int MAX_COMMENT_DEPTH = 2; // 최대 댓글 깊이 제한

    // 댓글 작성
    public DailyfeedServerResponse<CommentDto.Comment> createComment(MemberDto.Member member, String token, CommentDto.CreateCommentRequest request, HttpServletResponse httpResponse) {
        MemberProfileDto.Summary author = memberFeignHelper.getMemberSummaryById(member.getId(), token, httpResponse);
        Long authorId = author.getId();

        // 게시글 존재 확인
        Post post = getPostByIdOrThrow(request.getPostId());

        Comment comment;

        if (request.getParentId() != null) {
            // 대댓글인 경우
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

            comment = Comment.levelCommentBuilder()
                    .content(request.getContent())
                    .authorId(authorId)
                    .post(post)
                    .parent(parentComment)
                    .build();
        } else {
            // 최상위 댓글인 경우
            comment = Comment.commentBuilder()
                    .content(request.getContent())
                    .authorId(authorId)
                    .post(post)
                    .build();
        }

        Comment savedComment = commentRepository.save(comment);

        // 응답 생성 및 작성자 정보 추가
        CommentDto.Comment commentDto = commentMapper.toComment(savedComment, author);
        mergeAuthorData(List.of(commentDto), httpResponse);

        // timeline 을 위한 활동 기록
        publishCommentActivity(member.getId(), savedComment.getId(), CommentActivityType.CREATE);

        // mongodb 에 본문 저장 (Season2 개발 예정) (TODO)
//        insertNewDocument(savedComment);

        return DailyfeedServerResponse.<CommentDto.Comment>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(commentDto)
                .build();
    }

//     mongodb 에 본문 저장 (TODO)
//    public void insertNewDocument(Post post){
//        CommentDocument document = CommentDocument
//                .newPost(post.getId(), post.getTitle(), post.getContent(), post.getCreatedAt(), post.getUpdatedAt());
//        commentMongoRepository.save(document);
//    }

    // 댓글 수정
    public DailyfeedServerResponse<CommentDto.Comment> updateComment(MemberDto.Member member, Long commentId, CommentDto.UpdateCommentRequest request, String token, HttpServletResponse httpResponse) {
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

        // timeline 을 위한 활동 기록
        publishCommentActivity(member.getId(), updatedComment.getId(), CommentActivityType.UPDATE);

        // mongodb 에 본문 저장 (Season2 개발 예정) (TODO)
//        updateDocument(post);

        // 응답 생성 및 작성자 정보 추가
        CommentDto.Comment commentUpdated = commentMapper.toComment(updatedComment, author);
        mergeAuthorData(List.of(commentUpdated), httpResponse);

        return DailyfeedServerResponse.<CommentDto.Comment>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(commentUpdated)
                .build();
    }

    // mongodb 에 본문 저장 (TODO)
//    public void updateDocument(Post post){
//        CommentDocument oldDocument = commentMongoRepository
//                .findByPostPkAndIsDeletedAndIsCurrent(post.getId(), Boolean.FALSE, Boolean.TRUE)
//                .orElseThrow(click.dailyfeed.code.domain.content.post.exception.PostNotFoundException::new);
//
////        oldDocument.markAsDeleted(Boolean.FALSE);
//        CommentDocument updatedPost = CommentDocument.newUpdatedPost(oldDocument, post.getUpdatedAt());
//
//        commentMongoRepository.save(updatedPost);
//    }

    // 댓글 삭제 (소프트 삭제)
    public DailyfeedServerResponse<Boolean> deleteComment(MemberDto.Member requestedMember, Long commentId, String token, HttpServletResponse httpResponse) {
        Long authorId = requestedMember.getId();

        Comment comment = commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(CommentNotFoundException::new);

        // 작성자 권한 확인
        if (!comment.isOwnedBy(authorId)) {
            throw new CommentDeletionPermissionDeniedException();
        }

        // 댓글과 모든 자식 댓글들을 소프트 삭제 (TODO : Season2)
        commentRepository.softDeleteCommentAndChildren(commentId);

        // timeline 을 위한 활동 기록
        publishCommentActivity(requestedMember.getId(), commentId, CommentActivityType.SOFT_DELETE);

        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(Boolean.TRUE)
                .build();
    }

    // 특정 게시글의 댓글 목록 조회 (계층구조)
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<CommentDto.Comment> getCommentsByPost(Long postId, Pageable pageable, HttpServletResponse httpResponse) {
        Post post = getPostByIdOrThrow(postId);

        Page<Comment> topLevelComments = commentRepository.findCommentsByPost(post, pageable);

        // 모든 댓글(자식 포함)의 작성자 정보 추가
        DailyfeedPage<CommentDto.Comment> updatedCommentPage = mergeAuthorDataRecursively(topLevelComments, httpResponse);

        return DailyfeedPageResponse.<CommentDto.Comment>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(updatedCommentPage)
                .build();
    }

    // 특정 게시글의 댓글 목록을 페이징으로 조회
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<CommentDto.Comment> getCommentsByPostWithPaging(Long postId, int page, int size, HttpServletResponse httpResponse) {
        Post post = getPostByIdOrThrow(postId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findTopLevelCommentsByPostWithPaging(post, pageable);

        DailyfeedPage<CommentDto.Comment> updatedCommentPage = mergeAuthorDataRecursively(comments, httpResponse);

        return DailyfeedPageResponse.<CommentDto.Comment>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(updatedCommentPage)
                .build();
    }

    // 대댓글 목록 조회
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<CommentDto.Comment> getRepliesByParent(Long parentId, int page, int size, HttpServletResponse httpResponse) {
        Comment parentComment = commentRepository.findByIdAndNotDeleted(parentId)
                .orElseThrow(ParentCommentNotFoundException::new);

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> replies = commentRepository.findChildrenByParentWithPaging(parentComment, pageable);

        List<CommentDto.Comment> commentList = replies.getContent().stream()
                .map(commentMapper::toComment)
                .collect(Collectors.toList());

        mergeAuthorData(commentList, httpResponse);

        return DailyfeedPageResponse.<CommentDto.Comment>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(pageMapper.fromJpaPageToDailyfeedPage(replies, commentList))
                .build();
    }

    // 댓글 상세 조회
    @Transactional(readOnly = true)
    public DailyfeedServerResponse<CommentDto.Comment> getComment(Long commentId, HttpServletResponse httpResponse) {
        Comment comment = commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(CommentNotFoundException::new);

        CommentDto.Comment commentDto = commentMapper.toComment(comment);
        mergeAuthorData(List.of(commentDto), httpResponse);

        return DailyfeedServerResponse.<CommentDto.Comment>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(commentDto).build();
    }

    // 나의 댓글
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<CommentDto.CommentSummary> getMyComments(String token, int page, int size, HttpServletResponse httpResponse) {
        MemberDto.Member member = memberFeignHelper.getMember(token, httpResponse);
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByAuthorIdAndNotDeleted(member.getId(), pageable);

        return DailyfeedPageResponse.<CommentDto.CommentSummary>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(mergeAuthorData(comments, httpResponse))
                .build();
    }

    // 특정 사용자의 댓글 목록
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<CommentDto.CommentSummary> getCommentsByUser(Long userId, int page, int size, HttpServletResponse httpResponse) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByAuthorIdAndNotDeleted(userId, pageable);

        return DailyfeedPageResponse.<CommentDto.CommentSummary>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(mergeAuthorData(comments, httpResponse))
                .build();
    }

    public DailyfeedPage<CommentDto.CommentSummary> mergeAuthorData(Page<Comment> commentsPage, HttpServletResponse httpResponse) {
        List<CommentDto.CommentSummary> summaries = commentsPage.getContent().stream().map(commentMapper::toCommentSummary).collect(Collectors.toList());
        if (summaries.isEmpty()) return pageMapper.emptyPage();

        Set<Long> authorIds = summaries.stream()
                .map(CommentDto.CommentSummary::getAuthorId)
                .collect(Collectors.toSet());

        Map<Long, MemberProfileDto.Summary> authorMap = memberFeignHelper.getMemberMap(authorIds, httpResponse);

        summaries.forEach(summary -> {
            MemberProfileDto.Summary author = authorMap.get(summary.getAuthorId());
            if (author != null) {
                summary.updateAuthor(author);
            }
        });


        return pageMapper.fromJpaPageToDailyfeedPage(commentsPage, summaries);
    }

    // 계층구조 댓글에 작성자 정보 추가 (재귀적)
    private DailyfeedPage<CommentDto.Comment> mergeAuthorDataRecursively(Page<Comment> commentsPage, HttpServletResponse httpResponse) {
        if(commentsPage.isEmpty()) return pageMapper.emptyPage();

        List<CommentDto.Comment> commentList = commentsPage.getContent().stream().map(commentMapper::toComment).collect(Collectors.toList());

        Set<Long> authorIds = commentList.stream()
                .map(CommentDto.Comment::getAuthorId)
                .collect(Collectors.toSet());

        Map<Long, MemberProfileDto.Summary> authorMap = memberFeignHelper.getMemberMap(authorIds, httpResponse);

        commentList.forEach(comment -> {
            MemberProfileDto.Summary author = authorMap.get(comment.getAuthorId());
            if (author != null) {
                comment.updateAuthorRecursively(authorMap);
            }
        });

        return pageMapper.fromJpaPageToDailyfeedPage(commentsPage, commentList);

    }

    private void mergeAuthorData(List<CommentDto.Comment> comments, HttpServletResponse httpResponse) {
        if (comments.isEmpty()) return;

        Set<Long> authorIds = comments.stream()
                .map(CommentDto.Comment::getAuthorId)
                .collect(Collectors.toSet());

        try {
            Map<Long, MemberProfileDto.Summary> authorMap = memberFeignHelper.getMemberMap(authorIds, httpResponse);

            comments.forEach(comment -> {
                MemberProfileDto.Summary author = authorMap.get(comment.getAuthorId());
                if (author != null) {
                    comment.updateAuthor(author);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to fetch author info: {}", e.getMessage());
            throw new MemberException(MemberExceptionCode.MEMBER_API_CONNECTION_ERROR);
        }
    }

    // 좋아요 증가
    public void incrementLikeCount(MemberDto.Member member, Long commentId) {
        log.info("Incrementing like count for comment: {}", commentId);

        // 댓글 존재 확인
        Comment comment = commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(CommentNotFoundException::new);

        commentRepository.incrementLikeCount(commentId);

        publishLikeActivity(member.getId(), commentId, CommentLikeType.LIKE);
    }

    // 좋아요 감소
    public void decrementLikeCount(MemberDto.Member member, Long commentId) {
        log.info("Decrementing like count for comment: {}", commentId);

        // 댓글 존재 확인
        commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(CommentNotFoundException::new);

        commentRepository.decrementLikeCount(commentId);
        publishLikeActivity(member.getId(), commentId, CommentLikeType.CANCEL);
    }


    ///  helpers ///
    /// 댓글 생성/삭제 이벤트
    public void publishCommentActivity(Long memberId, Long commentId, CommentActivityType activityType) {
        try{
            LocalDateTime now = commentEventMapper.currentDateTime();
            String topicName = DateBasedTopicType.POST_ACTIVITY.generateTopicName(now);
            CommentDto.CommentActivityEvent activityEvent = commentEventMapper.newCommentActivityEvent(commentId, memberId, activityType, now);
            kafkaHelper.send(topicName, commentId.toString(), activityEvent);
        }
        catch (Exception e){
            log.error("Error publishing post activity event: ", e);
            throw new KafkaNetworkErrorException();
        }
    }

    /// 댓글 좋아요/좋아요 취소 이벤트
    public void publishLikeActivity(Long memberId, Long commentId, CommentLikeType commentLikeType) {
        try{
            LocalDateTime now = commentEventMapper.currentDateTime();
            String topicName = DateBasedTopicType.COMMENT_LIKE_ACTIVITY.generateTopicName(now);
            CommentDto.LikeActivityEvent activityEvent = commentEventMapper.newLikeActivityEvent(commentId, memberId, commentLikeType, now);
            kafkaHelper.send(topicName, commentId.toString(), activityEvent);
        }
        catch (Exception e){
            log.error("Error publishing post activity event: ", e);
            throw new KafkaNetworkErrorException();
        }

    }

    /// 글 조회
    public Post getPostByIdOrThrow(Long postId) {
        return postRepository.findByIdAndNotDeleted(postId).orElseThrow(PostNotFoundException::new);
    }

}
