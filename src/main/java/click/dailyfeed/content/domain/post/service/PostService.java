package click.dailyfeed.content.domain.post.service;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.content.post.exception.PostDeleteForbiddenException;
import click.dailyfeed.code.domain.content.post.exception.PostNotFoundException;
import click.dailyfeed.code.domain.content.post.exception.PostUpdateForbiddenException;
import click.dailyfeed.code.domain.content.post.type.PostActivityType;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.global.kafka.exception.KafkaException;
import click.dailyfeed.code.global.kafka.exception.KafkaNetworkErrorException;
import click.dailyfeed.code.global.web.response.DailyfeedPage;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.content.domain.comment.repository.jpa.CommentRepository;
import click.dailyfeed.content.domain.post.entity.Post;
import click.dailyfeed.content.domain.post.entity.PostActivityHistory;
import click.dailyfeed.content.domain.post.entity.PostLatestActivity;
import click.dailyfeed.content.domain.post.mapper.PostMapper;
import click.dailyfeed.content.domain.post.repository.jpa.PostActivityHistoryRepository;
import click.dailyfeed.content.domain.post.repository.jpa.PostLatestActivityRepository;
import click.dailyfeed.content.domain.post.repository.jpa.PostRepository;
import click.dailyfeed.feign.domain.member.MemberFeignHelper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class PostService {
    private final PostRepository postRepository;
    private final PostLatestActivityRepository postLatestActivityRepository;
    private final PostActivityHistoryRepository postActivityHistoryRepository;
    private final PostMapper postMapper;
    private final MemberFeignHelper memberFeignHelper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 게시글 작성
    public DailyfeedServerResponse<PostDto.Post> createPost(String token, PostDto.CreatePostRequest request, HttpServletResponse response) {
        // 작성자 정보 확인
        MemberDto.Member author = memberFeignHelper.getMember(token, response);
        if (author == null) {
            throw new MemberNotFoundException(() -> "삭제된 사용자의 접근입니다.");
        }

        Long authorId = author.getId();
        Post post = Post.newPost(request.getTitle(), request.getContent(), authorId);
        Post savedPost = postRepository.save(post);

        // todo timeline service
//        loggingPostActivity(authorId, savedPost.getId(), PostActivityType.CREATE);
        publishPostActivity(authorId, savedPost.getId(), PostActivityType.CREATE);

        return DailyfeedServerResponse.<PostDto.Post>builder()
                .data(postMapper.toPostDto(savedPost, author, 0))
                .ok("Y")
                .statusCode("201")
                .reason("SUCCESS")
                .build();
    }

    // 게시글 수정
    public DailyfeedServerResponse<PostDto.Post> updatePost(String token, Long postId, PostDto.UpdatePostRequest request, HttpServletResponse response) {
        MemberDto.Member author = memberFeignHelper.getMember(token, response);
        if (author == null) {
            throw new MemberNotFoundException(() -> "삭제된 사용자의 접근입니다.");
        }

        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        // 작성자 권한 확인
        if (!post.isAuthor(author.getId())) {
            throw new PostUpdateForbiddenException();
        }

        post.updatePost(request.getTitle(), request.getContent());
        Post updatedPost = postRepository.save(post);

        // todo timeline service
//        loggingPostActivity(author.getId(), updatedPost.getId(), PostActivityType.CREATE);
        publishPostActivity(author.getId(), updatedPost.getId(), PostActivityType.UPDATE);

        return DailyfeedServerResponse.<PostDto.Post>builder()
                .ok("Y")
                .statusCode("200")
                .reason("SUCCESS")
                .data(postMapper.toPostDto(updatedPost, author))
                .build();
    }

    public void publishPostActivity(Long memberId, Long postId, PostActivityType activityType) {
        try{
            LocalDateTime now = LocalDateTime.now();
            String currentDate = now.format(DATE_FORMATTER);
            String topicName = "post-activity-" + currentDate;

            PostDto.PostActivityEvent activityEvent = PostDto.PostActivityEvent.builder()
                    .memberId(memberId)
                    .followingId(null) // 팔로우 관련 정보는 별도 처리 필요시 추가
                    .postId(postId)
                    .postActivityType(activityType)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            kafkaTemplate.send(topicName, postId.toString(), activityEvent)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("Failed to send post activity event to topic: {}, postId: {}, activityType: {}",
                                    topicName, postId, activityType, throwable);
                        } else {
                            log.info("Successfully sent post activity event to topic: {}, postId: {}, activityType: {}",
                                    topicName, postId, activityType);
                        }
                    });
        }
        catch (Exception e){
            throw new KafkaNetworkErrorException();
        }
    }

//    @Transactional(propagation = Propagation.REQUIRES_NEW) // 추후 고려
    public void loggingPostActivity(Long memberId, Long postId, PostActivityType activityType) {
        postActivityHistoryRepository.save(PostActivityHistory.newPostActivityHistory(memberId, postId, activityType));
        postLatestActivityRepository.save(PostLatestActivity.newPostLatestActivity(memberId, postId, activityType));
    }

    // 게시글 삭제 (소프트 삭제)
    public DailyfeedServerResponse<Boolean> deletePost(String token, Long postId, HttpServletResponse response) {
        MemberDto.Member author = memberFeignHelper.getMember(token, response);
        if (author == null) {
            throw new MemberNotFoundException(() -> "삭제된 사용자의 접근입니다.");
        }

        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        // 작성자 권한 확인
        if (!post.isAuthor(author.getId())) {
            throw new PostDeleteForbiddenException();
        }

        postRepository.softDeleteById(postId);

        // todo timeline service
        publishPostActivity(author.getId(), postId, PostActivityType.DELETE);

        return DailyfeedServerResponse.<Boolean>builder()
                .ok("Y")
                .statusCode("204")
                .reason("SUCCESS")
                .data(Boolean.TRUE)
                .build();
    }

    // 게시글 상세 조회 (조회수 증가)
    @Transactional(readOnly = true)
    public DailyfeedServerResponse<PostDto.Post> getPost(Long postId, HttpServletResponse response) {
        log.info("Getting post: {}", postId);

        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        // 조회수 증가 (별도 트랜잭션으로 처리)
        post.incrementLikeCount();

        // 작성자 정보 조회
        MemberDto.Member author = memberFeignHelper.getMemberById(post.getAuthorId(), response);

        return DailyfeedServerResponse.<PostDto.Post>builder()
                .ok("Y")
                .statusCode("200")
                .reason("SUCCESS")
                .data(postMapper.toPostDto(post, author))
                .build();
    }

    // 삭제할지 결정을 못함 TODO
    // 기본 기능 (REST API 기준으로만 짤때 만들었던 기능)
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<PostDto.Post> getPosts(int page, int size, HttpServletResponse httpResponse) {
        log.info("Getting posts with paging - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findAllNotDeletedOrderByCreatedDateDesc(pageable);

        DailyfeedPage<PostDto.Post> postDailyfeedPage = postMapper.fromJpaPostPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .ok("Y").statusCode("200").reason("SUCCESS")
                .content(postDailyfeedPage)
                .build();
    }

    // 게시글 좋아요 증가
    public DailyfeedServerResponse<Boolean> incrementLikeCount(Long postId) {
        log.info("Incrementing like count for post: {}", postId);

        // 게시글 존재 확인
        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        post.incrementLikeCount();

        return DailyfeedServerResponse.<Boolean>builder()
                .ok("Y")
                .statusCode("200")
                .reason("SUCCESS")
                .data(Boolean.TRUE)
                .build();
    }

    // 게시글 좋아요 감소
    public DailyfeedServerResponse<Boolean> decrementLikeCount(Long postId) {
        log.info("Decrementing like count for post: {}", postId);

        // 게시글 존재 확인
        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        post.decrementLikeCount();

        return DailyfeedServerResponse.<Boolean>builder()
                .ok("Y")
                .statusCode("200")
                .reason("SUCCESS")
                .data(Boolean.TRUE)
                .build();
    }

    // 작성자별 게시글 목록 조회
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<PostDto.Post> getPostsByAuthor(String token, Pageable pageable, HttpServletResponse httpResponse) {
        MemberDto.Member author = memberFeignHelper.getMember(token, httpResponse);
        if (author == null) {
            throw new MemberNotFoundException(() -> "삭제된 사용자입니다");
        }

        Page<Post> posts = postRepository.findByAuthorIdAndNotDeleted(author.getId(), pageable);
        DailyfeedPage<PostDto.Post> postDailyfeedPage = postMapper.fromJpaPostPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));

        return DailyfeedPageResponse.<PostDto.Post>builder()
                .ok("Y").statusCode("200").reason("SUCCESS")
                .content(postDailyfeedPage)
                .build();
    }

    // TODO (삭제) timeline+contents 서비스로 이관
//    public DailyfeedPageResponse<PostDto.Post> getRecentlyActiveFollowingMembersPosts(String token, Pageable pageable, HttpServletResponse httpResponse) {
//        // 작성자 정보 확인
//        MemberDto.Member member = memberFeignHelper.getMember(token, httpResponse);
//        if (member == null) {
//            throw new MemberNotFoundException(() -> "삭제된 사용자의 접근입니다.");
//        }
//
//        // 팔로잉하고 있는 멤버들의 최근 활동(최신순 정렬)
//        DailyfeedPage<FollowDto.FollowingActivity> recentFollowersActivities = memberFeignHelper.getRecentlyActiveFollowingMembers(token, httpResponse);
//        // Post ID 추출
//        List<FollowDto.FollowingActivity> content = recentFollowersActivities.getContent();
//        Set<Long> postIds = content.stream().map(FollowDto.FollowingActivity::getPostId).collect(Collectors.toSet());
//
//        // 팔로잉 멤버가 없거나 팔로잉 멤버들의 활동이 전혀 없을 경우 인기게시물 노출
//        if(postIds == null || postIds.isEmpty()) {
//            return getPopularPosts(pageable.getPageSize(), pageable.getPageNumber(), httpResponse);
//        }
//
//        // Post ID에 대한 게시글 조회
//        Page<Post> postPage = postRepository.findPostsWithCommentsByPostIds(postIds, pageable);
//
//        // 댓글 수, 작가 정보 조합
//        List<PostDto.Post> posts = mergeAuthorAndCommentCount(postPage.getContent(), httpResponse);
//        DailyfeedPage<PostDto.Post> postDailyfeedPage = postMapper.fromJpaPostPage(postPage, posts);
//
//        return DailyfeedPageResponse.<PostDto.Post>builder()
//                .ok("Y").statusCode("200").reason("SUCCESS")
//                .content(postDailyfeedPage)
//                .build();
//    }

    // 댓글이 많은 게시글 조회 (댓글 수로 정렬)
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<PostDto.Post> getPostsOrderByCommentCount(int page, int size, HttpServletResponse httpResponse) {
        log.info("Getting posts ordered by comment count");

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findMostCommentedPosts(pageable);

        DailyfeedPage<PostDto.Post> postDailyfeedPage = postMapper.fromJpaPostPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .ok("Y").statusCode("200").reason("SUCCESS")
                .content(postDailyfeedPage)
                .build();
    }

    // 인기 게시글 조회
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<PostDto.Post> getPopularPosts(int page, int size, HttpServletResponse httpResponse) {
        log.info("Getting popular posts");

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findPopularPostsNotDeleted(pageable);

        DailyfeedPage<PostDto.Post> postDailyfeedPage = postMapper.fromJpaPostPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .ok("Y").statusCode("200").reason("SUCCESS")
                .content(postDailyfeedPage)
                .build();
    }

    // 최근 댓글이 있는 게시글 조회
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<PostDto.Post> getPostsByRecentActivity(int page, int size, HttpServletResponse httpResponse) {
        log.info("Getting posts by recent activity");

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findPostsByRecentActivity(pageable);

        DailyfeedPage<PostDto.Post> postDailyfeedPage = postMapper.fromJpaPostPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .ok("Y").statusCode("200").reason("SUCCESS")
                .content(postDailyfeedPage)
                .build();
    }

    // 게시글 검색
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<PostDto.Post> searchPosts(String keyword, int page, int size, HttpServletResponse httpResponse) {
        log.info("Searching posts with keyword: {}", keyword);

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByTitleOrContentContainingAndNotDeleted(keyword, pageable);

        DailyfeedPage<PostDto.Post> postDailyfeedPage = postMapper.fromJpaPostPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .ok("Y").statusCode("200").reason("SUCCESS")
                .content(postDailyfeedPage)
                .build();
    }

    public List<PostDto.Post> mergeAuthorAndCommentCount(List<Post> posts, HttpServletResponse httpResponse){
        // (1) 작성자 id 추출
        Set<Long> authorIds = posts.stream()
                .map(Post::getAuthorId)
                .collect(Collectors.toSet());

        // (2) 작성자 상세 정보
        Map<Long, MemberDto.Member> authorsMap = memberFeignHelper.getMemberMap(authorIds, httpResponse);

        return posts.stream()
                .map(post -> {
                    return postMapper.toPostDto(post, authorsMap.get(post.getAuthorId()), post.getCommentsCount());
                })
                .collect(Collectors.toList());
    }

    // 특정 기간 내 게시글 조회 (필요할지는 모르겠지만...)
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<PostDto.Post> getPostsByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size, HttpServletResponse httpResponse) {
        log.info("Getting posts between {} and {}", startDate, endDate);

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByCreatedDateBetweenAndNotDeleted(startDate, endDate, pageable);

        DailyfeedPage<PostDto.Post> postDailyfeedPage = postMapper.fromJpaPostPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .ok("Y").statusCode("200").reason("SUCCESS")
                .content(postDailyfeedPage)
                .build();
    }

    // 관리자용: 작성자별 게시글 일괄 삭제
    public int deletePostsByAuthor(Long authorId) {
        log.info("Admin deleting all posts by author: {}", authorId);
        return postRepository.softDeleteByAuthorId(authorId);
    }
}
