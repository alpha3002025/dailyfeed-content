package click.dailyfeed.content.domain.post.api;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.content.domain.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@RestController
public class PostController {
    private final PostService postService;

    /// 특정 post Id 리스트에 해당되는 글 목록
    @Operation(summary = "특정 post id 리스트에 해당하는 글 목록", description = "글 id 목록에 대한 글 데이터 목록을 조회합니다.")
    @PostMapping  ("/list")
    public DailyfeedServerResponse<List<PostDto.Post>> getPostListByIdsIn(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse httpResponse,
            PostDto.PostsBulkRequest request
    ){
        return postService.getPostListByIdsIn(request, token, httpResponse);
    }

    // 게시글 작성
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다.")
    @PostMapping
    public DailyfeedServerResponse<PostDto.Post> createPost(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody PostDto.CreatePostRequest request,
            HttpServletResponse response ) {

        return postService.createPost(authorizationHeader, request, response);
    }

    // 게시글 수정
    @Operation(summary = "게시글 수정", description = "기존 게시글을 수정합니다.")
    @PutMapping("/{postId}")
    public DailyfeedServerResponse<PostDto.Post> updatePost(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse,
            @PathVariable Long postId,
            @Valid @RequestBody PostDto.UpdatePostRequest updateRequest ) {
        return postService.updatePost(authorizationHeader, postId, updateRequest, httpResponse);
    }

    // 게시글 삭제
    @Operation(summary = "게시글 삭제", description = "기존 게시글을 삭제합니다.")
    @DeleteMapping("/{postId}")
    public DailyfeedServerResponse<Boolean> deletePost(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse,
            @PathVariable Long postId ) {
        return postService.deletePost(authorizationHeader, postId, httpResponse);
    }

    // 게시글 상세 조회
    @Operation(summary = "게시글 상세 조회", description = "특정 게시글의 상세 정보를 조회합니다.")
    @GetMapping("/{postId}")
    public DailyfeedServerResponse<PostDto.Post> getPost(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse httpResponse,
            @PathVariable Long postId) {
        return postService.getPost(postId, token, httpResponse);
    }

    // 게시글 좋아요 증가
    @PostMapping("/{postId}/like")
    public DailyfeedServerResponse<Boolean> incrementLikeCount(@PathVariable Long postId) {
        return postService.incrementLikeCount(postId);
    }

    // 게시글 좋아요 감소
    @DeleteMapping("/{postId}/like")
    public DailyfeedServerResponse<Boolean> decrementLikeCount(@PathVariable Long postId) {
        return postService.decrementLikeCount(postId);
    }

    // 작성자별 게시글 목록 조회
    @Operation(summary = "특정 사용자의 게시글 목록 조회", description = "특정 사용자가 작성한 게시글을 페이징하여 조회합니다.")
    @GetMapping("/authors/{authorId}")
    public DailyfeedPageResponse<PostDto.Post> getPostsByAuthor(
            HttpServletResponse httpResponse,
            @RequestHeader("Authorization") String authorizationHeader,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {
        return postService.getPostsByAuthor(authorizationHeader, pageable, httpResponse);
    }

    // TODO (삭제) timeline+contents 서비스로 이관
//    // 팔로잉 게시글 목록 조회
//    // (timeline 으로 이관 예정(/timeline/feed))
//    @Operation(summary = "팔로잉 사용자들의 게시물 조회 (피드)", description = "팔로잉한 사용자들의 게시물을 시간순으로 조회합니다.")
//    @GetMapping("/feed")
//    public DailyfeedPageResponse<PostDto.Post> getFeed(
//            HttpServletResponse httpResponse,
//            @RequestHeader("Authorization") String authorizationHeader,
//            @PageableDefault(
//                    page = 0,
//                    size = 15,
//                    sort = "createdAt",
//                    direction = Sort.Direction.DESC
//            ) Pageable pageable) {
//        return postService.getRecentlyActiveFollowingMembersPosts(authorizationHeader, pageable, httpResponse);
//    }

    // 특정 기간 내 게시글 조회
    // (timeline 으로 이관 예정(/timeline/feed/search/period))
    @GetMapping("/date-range")
    public DailyfeedPageResponse<PostDto.Post> getPostsByDateRange(
            HttpServletResponse httpResponse,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postService.getPostsByDateRange(startDate, endDate, page, size, httpResponse);
    }

    // 댓글이 많은 게시글 조회
    // (timeline 으로 이관 예정(/timeline/feed/popular/most-commented))
    @GetMapping("/popular-by-comments")
    public DailyfeedPageResponse<PostDto.Post> getPostsOrderByCommentCount(
            HttpServletResponse httpResponse,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postService.getPostsOrderByCommentCount(page, size, httpResponse);
    }

    // 인기 게시글 조회
    // (timeline 으로 이관 예정(/timeline/feed/popular))
    @GetMapping("/popular")
    public DailyfeedPageResponse<PostDto.Post> getPopularPosts(
            HttpServletResponse httpResponse,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postService.getPopularPosts(page, size, httpResponse);
    }

    // 최근 활동이 있는 게시글 조회
    // (timeline 으로 이관 예정 (/timeline/feed/latest))
    @GetMapping("/recent-activity")
    public DailyfeedPageResponse<PostDto.Post> getPostsByRecentActivity(
            HttpServletResponse httpResponse,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postService.getPostsByRecentActivity(page, size, httpResponse);
    }

    // 게시글 검색
    // (timeline 으로 이관 예정 (/timeline/search))
    @GetMapping("/search")
    public DailyfeedPageResponse<PostDto.Post> searchPosts(
            HttpServletResponse httpResponse,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Searching posts with keyword: {}", keyword);

        return postService.searchPosts(keyword, page, size, httpResponse);
    }

//    // 관리자용: 작성자별 게시글 일괄 삭제
//    @DeleteMapping("/admin/author/{authorId}")
//    public ServerResponse<Integer> deletePostsByAuthor(
//            @PathVariable Long authorId,
//            @RequestHeader("X-Admin-Auth") String adminToken) {
//
//        log.info("Admin deleting all posts by author: {}", authorId);
//
//        // 관리자 권한 검증 로직 (todo :: 어드민 기능도 염두에 두었으나, 현재 버전에서는 보류)
//        // validateAdminToken(adminToken);
//
//        int deletedCount = postService.deletePostsByAuthor(authorId);
//
//        return ServerResponse.<Integer>builder()
//                .data(deletedCount)
//                .ok("Y")
//                .statusCode("200")
//                .reason("SUCCESS")
//                .build();
//    }
}
