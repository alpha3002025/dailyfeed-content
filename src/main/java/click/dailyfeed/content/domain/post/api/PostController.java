package click.dailyfeed.content.domain.post.api;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.content.domain.post.service.PostService;
import click.dailyfeed.feign.config.web.annotation.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
            @AuthenticatedMember MemberDto.Member member,
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
            @AuthenticatedMember MemberDto.Member member,
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

    @Operation(summary = "내가 쓴 개시글 목록 조회", description = "내가 쓴 개시글 목록을 조회합니다.")
    @GetMapping({"","/"})
    public DailyfeedPageResponse<PostDto.Post> getPosts(
            @AuthenticatedMember MemberDto.Member member,
            HttpServletResponse httpResponse,
            @RequestHeader("Authorization") String token,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        DailyfeedPage<PostDto.Post> result = postService.getMyPosts(member, pageable, token, httpResponse);
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }


    // 게시글 상세 조회
    @Operation(summary = "게시글 상세 조회", description = "특정 게시글의 상세 정보를 조회합니다.")
    @GetMapping("/{postId}")
    public DailyfeedServerResponse<PostDto.Post> getPost(
            @AuthenticatedMember MemberDto.Member member,
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse httpResponse,
            @PathVariable Long postId) {

        PostDto.Post result = postService.getPostById(member, postId, token, httpResponse);
        return DailyfeedServerResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 게시글 좋아요 증가
    @PostMapping("/{postId}/like")
    public DailyfeedServerResponse<Boolean> incrementLikeCount(
            @AuthenticatedMember MemberDto.Member member,
            @PathVariable Long postId
    ) {

        Boolean result = postService.incrementLikeCount(postId);
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

        Boolean result = postService.decrementLikeCount(postId);
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 작성자별 게시글 목록 조회
    @Operation(summary = "특정 사용자의 게시글 목록 조회", description = "특정 사용자가 작성한 게시글을 페이징하여 조회합니다.")
    @GetMapping("/authors/{authorId}")
    public DailyfeedPageResponse<PostDto.Post> getPostsByAuthor(
            @AuthenticatedMember MemberDto.Member member,
            HttpServletResponse httpResponse,
            @RequestHeader("Authorization") String token,
            @PathVariable Long authorId,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        DailyfeedPage<PostDto.Post> result = postService.getPostsByAuthor(authorId, pageable, token, httpResponse);
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    /// 특수목적 or internal
    /// 특정 post Id 리스트에 해당되는 글 목록
    @Operation(summary = "특정 post id 리스트에 해당하는 글 목록", description = "글 id 목록에 대한 글 데이터 목록을 조회합니다.")
    @PostMapping  ("/query/list")
    public DailyfeedServerResponse<List<PostDto.Post>> getPostListByIdsIn(
            @AuthenticatedMember MemberDto.Member member,
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse httpResponse,
            @RequestBody PostDto.PostsBulkRequest request
    ){

        List<PostDto.Post> result = postService.getPostListByIdsIn(request, token, httpResponse);
        return DailyfeedServerResponse.<List<PostDto.Post>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }


    // TODO : timeline-svc 으로 이관
    // 특정 기간 내 게시글 조회
    @GetMapping("/date-range")
    public DailyfeedPageResponse<PostDto.Post> getPostsByDateRange(
            @RequestHeader("Authorization") String token,
            HttpServletResponse httpResponse,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        DailyfeedPage<PostDto.Post> result = postService.getPostsByDateRange(startDate, endDate, page, size, token, httpResponse);
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 댓글이 많은 게시글 조회
    // (timeline 으로 이관 예정(/timeline/feed/popular/most-commented))
    @GetMapping("/popular-by-comments")
    public DailyfeedPageResponse<PostDto.Post> getPostsOrderByCommentCount(
            @RequestHeader("Authorization") String token,
            HttpServletResponse httpResponse,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        DailyfeedPage<PostDto.Post> result = postService.getPostsOrderByCommentCount(page, size, token, httpResponse);
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 인기 게시글 조회
    // (timeline 으로 이관 예정(/timeline/feed/popular))
    @GetMapping("/popular")
    public DailyfeedPageResponse<PostDto.Post> getPopularPosts(
            @RequestHeader("Authorization") String token,
            HttpServletResponse httpResponse,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        DailyfeedPage<PostDto.Post> result = postService.getPopularPosts(page, size, token, httpResponse);
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 최근 활동이 있는 게시글 조회
    // (timeline 으로 이관 예정 (/timeline/feed/latest))
    @GetMapping("/recent-activity")
    public DailyfeedPageResponse<PostDto.Post> getPostsByRecentActivity(
            @RequestHeader("Authorization") String token,
            HttpServletResponse httpResponse,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        DailyfeedPage<PostDto.Post> result = postService.getPostsByRecentActivity(page, size, token, httpResponse);
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 게시글 검색
    // (timeline 으로 이관 예정 (/timeline/search))
    @GetMapping("/search")
    public DailyfeedPageResponse<PostDto.Post> searchPosts(
            @RequestHeader("Authorization") String token,
            HttpServletResponse httpResponse,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        DailyfeedPage<PostDto.Post> result = postService.searchPosts(keyword, page, size, token, httpResponse);
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }
}
