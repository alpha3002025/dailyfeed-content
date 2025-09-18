package click.dailyfeed.content.domain.comment.api;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.content.domain.comment.service.CommentService;
import click.dailyfeed.feign.config.web.AuthenticatedMember;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/comments")
@RestController
public class CommentController {
    private final CommentService commentService;

    ///  /comments  ///
    // 댓글 작성
    @PostMapping("/")
    public DailyfeedServerResponse<CommentDto.Comment> createComment(
            @AuthenticatedMember MemberDto.Member member,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse,
            @Valid @RequestBody CommentDto.CreateCommentRequest request) {
        return commentService.createComment(member, authorizationHeader, request, httpResponse);
    }

    // 내 댓글 목록
    @GetMapping("/")
    public DailyfeedPageResponse<CommentDto.CommentSummary> getMyComments(
            HttpServletResponse httpResponse,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return commentService.getMyComments(authorizationHeader, page, size, httpResponse);
    }

    ///  /comments/post/{postId}    ///
    /// 참고)
    ///   Post Controller 내에서 구성하는게 이론적으로는 적절하지만,
    ///   게시글 서비스와 댓글 서비스간의 경계를 구분하기로 결정했기에 댓글 관리의 주체를 CommentController 로 지정
    ///   특정 게시글의 댓글 목록 조회 (계층구조)
    @GetMapping("/post/{postId}/list")
    public DailyfeedPageResponse<CommentDto.Comment> getCommentsByPostWithoutPaging(
            String token,
            HttpServletResponse httpResponse,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @PathVariable Long postId
    ) {
        return commentService.getCommentsByPost(postId, pageable, httpResponse);
    }

    // 특정 게시글의 댓글 목록을 페이징으로 조회
    @GetMapping("/post/{postId}")
    public DailyfeedPageResponse<CommentDto.Comment> getCommentsByPost(
            HttpServletResponse httpResponse,
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return commentService.getCommentsByPostWithPaging(postId, page, size, httpResponse);
    }

    /// /comments/member/{memberId}     ///
    // 특정 사용자의 댓글 목록
    @GetMapping("/member/{memberId}")
    public DailyfeedPageResponse<CommentDto.CommentSummary> getCommentsByUser(
            HttpServletResponse httpResponse,
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return commentService.getCommentsByUser(memberId, page, size, httpResponse);
    }

    /// /comments/{commentId}   ///
    // 댓글 수정
    @PutMapping("/{commentId}")
    public DailyfeedServerResponse<CommentDto.Comment> updateComment(
            @AuthenticatedMember MemberDto.Member member,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentDto.UpdateCommentRequest request) {
        return commentService.updateComment(member, commentId, request, authorizationHeader, httpResponse);
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public DailyfeedServerResponse<Boolean> deleteComment(
            @PathVariable Long commentId,
            @AuthenticatedMember MemberDto.Member member,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse
    ) {
        return commentService.deleteComment(member, commentId, authorizationHeader, httpResponse);
    }

    // 댓글 상세 조회
    @GetMapping("/{commentId}")
    public DailyfeedServerResponse<CommentDto.Comment> getComment(
            HttpServletResponse httpResponse,
            @PathVariable Long commentId) {
        return commentService.getComment(commentId, httpResponse);
    }

    // 대댓글 목록 조회
    @GetMapping("/{commentId}/replies")
    public DailyfeedPageResponse<CommentDto.Comment> getRepliesByParent(
            HttpServletResponse httpResponse,
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return commentService.getRepliesByParent(commentId, page, size, httpResponse);
    }

    // 댓글 좋아요
    @PostMapping("/{commentId}/like")
    public DailyfeedServerResponse<Boolean> likeComment(
            @PathVariable Long commentId,
            @AuthenticatedMember MemberDto.Member member
    ) {
        commentService.incrementLikeCount(member, commentId);
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.CREATED.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(Boolean.TRUE)
                .build();
    }

    // 댓글 좋아요 취소
    @DeleteMapping("/{commentId}/like")
    public DailyfeedServerResponse<Boolean> cancelLikeComment(
            @PathVariable Long commentId,
            @AuthenticatedMember MemberDto.Member member
    ) {
        commentService.decrementLikeCount(member, commentId);
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.NO_CONTENT.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(Boolean.TRUE)
                .build();
    }

}
