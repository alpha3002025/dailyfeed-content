package click.dailyfeed.content.domain.comment.api;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.content.domain.comment.service.CommentService;
import click.dailyfeed.feign.config.web.annotation.AuthenticatedMember;
import click.dailyfeed.feign.config.web.annotation.AuthenticatedMemberProfileSummary;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @PostMapping({"","/"})
    public DailyfeedServerResponse<CommentDto.Comment> createComment(
            @AuthenticatedMemberProfileSummary MemberProfileDto.Summary member,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse,
            @Valid @RequestBody CommentDto.CreateCommentRequest request) {
        CommentDto.Comment result = commentService.createComment(member, authorizationHeader, request, httpResponse);
        return DailyfeedServerResponse.<CommentDto.Comment>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

//    // 내 댓글 목록
//    @GetMapping("/")
//    public DailyfeedPageResponse<CommentDto.CommentSummary> getMyComments(
//            @RequestHeader("Authorization") String authorizationHeader,
//            HttpServletResponse httpResponse,
//            @AuthenticatedMember MemberDto.Member requestedMember,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//        DailyfeedPage<CommentDto.CommentSummary> result = commentService.getMyComments(requestedMember.getId(), page, size, authorizationHeader, httpResponse);
//        return DailyfeedPageResponse.<CommentDto.CommentSummary>builder()
//                .status(HttpStatus.OK.value())
//                .result(ResponseSuccessCode.SUCCESS)
//                .data(result)
//                .build();
//    }

    ///  /comments/post/{postId}    ///
    /// 참고)
    ///   Post Controller 내에서 구성하는게 이론적으로는 적절하지만,
    ///   게시글 서비스와 댓글 서비스간의 경계를 구분하기로 결정했기에 댓글 관리의 주체를 CommentController 로 지정
    ///   특정 게시글의 댓글 목록 조회 (계층구조)
    // 특정 게시글의 댓글 목록을 페이징으로 조회
//    @GetMapping("/post/{postId}")
//    public DailyfeedPageResponse<CommentDto.Comment> getCommentsByPost(
//            @AuthenticatedMember MemberDto.Member requestedMember,
//            @RequestHeader("Authorization") String authorizationHeader,
//            HttpServletResponse httpResponse,
//            @PathVariable Long postId,
//            @PageableDefault(
//                    page = 0,
//                    size = 10,
//                    sort = "createdAt",
//                    direction = Sort.Direction.DESC
//            ) Pageable pageable) {
//
//        DailyfeedPage<CommentDto.Comment> result = commentService.getCommentsByPostWithPaging(postId, pageable, authorizationHeader, httpResponse);
//        return DailyfeedPageResponse.<CommentDto.Comment>builder()
//                .status(HttpStatus.OK.value())
//                .result(ResponseSuccessCode.SUCCESS)
//                .data(result)
//                .build();
//    }

//    /// /comments/member/{memberId}     ///
//    // 특정 사용자의 댓글 목록
//    @GetMapping("/member/{memberId}")
//    public DailyfeedPageResponse<CommentDto.CommentSummary> getCommentsByUser(
//            @RequestHeader("Authorization") String authorizationHeader,
//            HttpServletResponse httpResponse,
//            @PathVariable Long memberId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//
//        DailyfeedPage<CommentDto.CommentSummary> result = commentService.getCommentsByUser(memberId, page, size, authorizationHeader, httpResponse);
//        return DailyfeedPageResponse.<CommentDto.CommentSummary>builder()
//                .status(HttpStatus.OK.value())
//                .result(ResponseSuccessCode.SUCCESS)
//                .data(result)
//                .build();
//    }

    /// /comments/{commentId}   ///
    // 댓글 수정
    @PutMapping("/{commentId}")
    public DailyfeedServerResponse<CommentDto.Comment> updateComment(
            @AuthenticatedMember MemberDto.Member member,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentDto.UpdateCommentRequest request) {

        CommentDto.Comment result = commentService.updateComment(member, commentId, request, authorizationHeader, httpResponse);
        return DailyfeedServerResponse.<CommentDto.Comment>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public DailyfeedServerResponse<Boolean> deleteComment(
            @PathVariable Long commentId,
            @AuthenticatedMember MemberDto.Member member,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse
    ) {

        Boolean result = commentService.deleteComment(member, commentId, authorizationHeader, httpResponse);
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

//    // 댓글 상세 조회
//    @GetMapping("/{commentId}")
//    public DailyfeedServerResponse<CommentDto.Comment> getComment(
//            @RequestHeader("Authorization") String authorizationHeader,
//            HttpServletResponse httpResponse,
//            @PathVariable Long commentId) {
//
//        CommentDto.Comment result = commentService.getCommentById(commentId, authorizationHeader, httpResponse);
//        return DailyfeedServerResponse.<CommentDto.Comment>builder()
//                .status(HttpStatus.OK.value())
//                .result(ResponseSuccessCode.SUCCESS)
//                .data(result)
//                .build();
//    }
//
//    // 대댓글 목록 조회
//    @GetMapping("/{commentId}/replies")
//    public DailyfeedPageResponse<CommentDto.Comment> getRepliesByParent(
//            @RequestHeader("Authorization") String authorizationHeader,
//            HttpServletResponse httpResponse,
//            @PathVariable Long commentId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//
//        DailyfeedPage<CommentDto.Comment> result = commentService.getRepliesByParent(commentId, page, size, authorizationHeader, httpResponse);
//        return DailyfeedPageResponse.<CommentDto.Comment>builder()
//                .status(HttpStatus.OK.value())
//                .result(ResponseSuccessCode.SUCCESS)
//                .data(result)
//                .build();
//    }

    // 댓글 좋아요
    @PostMapping("/{commentId}/like")
    public DailyfeedServerResponse<Boolean> likeComment(
            @PathVariable Long commentId,
            @AuthenticatedMember MemberDto.Member member,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse
    ) {
        commentService.incrementLikeCount(member, commentId, authorizationHeader, httpResponse);
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.CREATED.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(Boolean.TRUE)
                .build();
    }

    // 댓글 좋아요 취소
    @DeleteMapping("/{commentId}/like")
    public DailyfeedServerResponse<Boolean> cancelLikeComment(
            @PathVariable Long commentId,
            @AuthenticatedMember MemberDto.Member member,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse
    ) {
        commentService.decrementLikeCount(member, commentId, authorizationHeader, httpResponse);
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.NO_CONTENT.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(Boolean.TRUE)
                .build();
    }

}
