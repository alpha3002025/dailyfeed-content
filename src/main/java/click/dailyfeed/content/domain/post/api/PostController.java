package click.dailyfeed.content.domain.post.api;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.content.domain.post.service.PostService;
import click.dailyfeed.feign.config.web.annotation.AuthenticatedMember;
import click.dailyfeed.feign.config.web.annotation.AuthenticatedMemberProfileSummary;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@RestController
public class PostController {
    private final PostService postService;

    /// entity
    // 게시글 작성
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다.")
    @PostMapping
    public DailyfeedServerResponse<PostDto.Post> createPost(
            @AuthenticatedMemberProfileSummary MemberProfileDto.Summary member,
            @Valid @RequestBody PostDto.CreatePostRequest request,
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response ) {

        PostDto.Post result = postService.createPost(member, request, token, response);
        return DailyfeedServerResponse.<PostDto.Post>builder()
                .data(result)
                .status(HttpStatus.CREATED.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    // 게시글 수정
    @Operation(summary = "게시글 수정", description = "기존 게시글을 수정합니다.")
    @PutMapping("/{postId}")
    public DailyfeedServerResponse<PostDto.Post> updatePost(
            @AuthenticatedMemberProfileSummary MemberProfileDto.Summary member,
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse httpResponse,
            @PathVariable Long postId,
            @Valid @RequestBody PostDto.UpdatePostRequest updateRequest ) {

        PostDto.Post result = postService.updatePost(member, postId, updateRequest, token, httpResponse);
        return DailyfeedServerResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 게시글 삭제
    @Operation(summary = "게시글 삭제", description = "기존 게시글을 삭제합니다.")
    @DeleteMapping("/{postId}")
    public DailyfeedServerResponse<Boolean> deletePost(
            @AuthenticatedMember MemberDto.Member member,
            HttpServletResponse httpResponse,
            @PathVariable Long postId ) {
        Boolean result = postService.deletePost(member, postId, httpResponse);
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

//    @Operation(summary = "내가 쓴 개시글 목록 조회", description = "내가 쓴 개시글 목록을 조회합니다.")
//    @GetMapping({"","/"})
//    public DailyfeedPageResponse<PostDto.Post> getPosts(
//            @AuthenticatedMember MemberDto.Member member,
//            HttpServletResponse httpResponse,
//            @RequestHeader("Authorization") String token,
//            @PageableDefault(
//                    page = 0,
//                    size = 10,
//                    sort = "createdAt",
//                    direction = Sort.Direction.DESC
//            ) Pageable pageable
//    ) {
//        DailyfeedPage<PostDto.Post> result = postService.getMyPosts(member, pageable, token, httpResponse);
//        return DailyfeedPageResponse.<PostDto.Post>builder()
//                .status(HttpStatus.OK.value())
//                .result(ResponseSuccessCode.SUCCESS)
//                .data(result)
//                .build();
//    }


    // 게시글 상세 조회
//    @Operation(summary = "게시글 상세 조회", description = "특정 게시글의 상세 정보를 조회합니다.")
//    @GetMapping("/{postId}")
//    public DailyfeedServerResponse<PostDto.Post> getPost(
//            @AuthenticatedMember MemberDto.Member member,
//            @RequestHeader(value = "Authorization", required = false) String token,
//            HttpServletResponse httpResponse,
//            @PathVariable Long postId) {
//
//        PostDto.Post result = postService.getPostById(member, postId, token, httpResponse);
//        return DailyfeedServerResponse.<PostDto.Post>builder()
//                .status(HttpStatus.OK.value())
//                .result(ResponseSuccessCode.SUCCESS)
//                .data(result)
//                .build();
//    }

    // 게시글 좋아요 증가
    @PostMapping("/{postId}/like")
    public DailyfeedServerResponse<Boolean> incrementLikeCount(
            @AuthenticatedMember MemberDto.Member member,
            @PathVariable Long postId
    ) {

        Boolean result = postService.incrementLikeCount(postId, member);
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 게시글 좋아요 감소
    @DeleteMapping("/{postId}/like")
    public DailyfeedServerResponse<Boolean> decrementLikeCount(
            @AuthenticatedMember MemberDto.Member member,
            @PathVariable Long postId) {

        Boolean result = postService.decrementLikeCount(postId, member);
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 작성자별 게시글 목록 조회
//    @Operation(summary = "특정 사용자의 게시글 목록 조회", description = "특정 사용자가 작성한 게시글을 페이징하여 조회합니다.")
//    @GetMapping("/authors/{authorId}")
//    public DailyfeedPageResponse<PostDto.Post> getPostsByAuthor(
//            @AuthenticatedMember MemberDto.Member member,
//            HttpServletResponse httpResponse,
//            @RequestHeader("Authorization") String token,
//            @PathVariable Long authorId,
//            @PageableDefault(
//                    page = 0,
//                    size = 10,
//                    sort = "createdAt",
//                    direction = Sort.Direction.DESC
//            ) Pageable pageable) {
//
//        DailyfeedPage<PostDto.Post> result = postService.getPostsByAuthor(authorId, pageable, token, httpResponse);
//        return DailyfeedPageResponse.<PostDto.Post>builder()
//                .status(HttpStatus.OK.value())
//                .result(ResponseSuccessCode.SUCCESS)
//                .data(result)
//                .build();
//    }

    // 게시글 검색
//    @GetMapping("/search")
//    public DailyfeedPageResponse<PostDto.Post> searchPosts(
//            @RequestHeader("Authorization") String token,
//            HttpServletResponse httpResponse,
//            @RequestParam String keyword,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//
//        DailyfeedPage<PostDto.Post> result = postService.searchPosts(keyword, page, size, token, httpResponse);
//        return DailyfeedPageResponse.<PostDto.Post>builder()
//                .status(HttpStatus.OK.value())
//                .result(ResponseSuccessCode.SUCCESS)
//                .data(result)
//                .build();
//    }
}
