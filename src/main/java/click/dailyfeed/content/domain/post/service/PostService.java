package click.dailyfeed.content.domain.post.service;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.content.post.exception.PostDeleteForbiddenException;
import click.dailyfeed.code.domain.content.post.exception.PostNotFoundException;
import click.dailyfeed.code.domain.content.post.exception.PostUpdateForbiddenException;
import click.dailyfeed.code.domain.content.post.type.PostActivityType;
import click.dailyfeed.code.domain.content.post.type.PostLikeType;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.global.cache.RedisKeyConstant;
import click.dailyfeed.code.global.kafka.exception.KafkaNetworkErrorException;
import click.dailyfeed.code.global.kafka.type.DateBasedTopicType;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.redis.config.redis.generator.DatePeriodBasedPageKeyGenerator;
import click.dailyfeed.content.domain.kafka.KafkaHelper;
import click.dailyfeed.content.domain.post.document.PostDocument;
import click.dailyfeed.content.domain.post.entity.Post;
import click.dailyfeed.content.domain.post.mapper.PostEventMapper;
import click.dailyfeed.content.domain.post.mapper.PostMapper;
import click.dailyfeed.content.domain.post.repository.jpa.PostRepository;
import click.dailyfeed.content.domain.post.repository.mongo.PostMongoRepository;
import click.dailyfeed.feign.domain.member.MemberFeignHelper;
import click.dailyfeed.feign.domain.post.PostFeignHelper;
import click.dailyfeed.pagination.mapper.PageMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
public class PostService {
    private final PostRepository postRepository;
    private final PostMongoRepository postMongoRepository;
    private final PostMapper postMapper;
    private final PostEventMapper postEventMapper;
    private final PageMapper pageMapper;
    private final MemberFeignHelper memberFeignHelper;
    private final PostFeignHelper postFeignHelper;
    private final KafkaHelper kafkaHelper;
    private final DatePeriodBasedPageKeyGenerator datePeriodBasedPageKeyGenerator;

    // 특정 post id 리스트에 해당하는 post 리스트 조회
    @Cacheable(value = RedisKeyConstant.PostService.INTERNAL_LIST_GET_POST_LIST_BY_IDS_IN, key = "#request.ids", cacheManager = "redisCacheManager")
    public DailyfeedServerResponse<List<PostDto.Post>> getPostListByIdsIn(PostDto.PostsBulkRequest request, String token, HttpServletResponse httpResponse) {
        List<PostDto.Post> postList = postFeignHelper.getPostList(request, token, httpResponse);
        return DailyfeedServerResponse.<List<PostDto.Post>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(postList)
                .build();
    }

    // 게시글 작성
    public DailyfeedServerResponse<PostDto.Post> createPost(MemberDto.Member author, PostDto.CreatePostRequest request, String token, HttpServletResponse response) {
        // 작성자 정보 확인
        Long authorId = author.getId();
        MemberProfileDto.Summary memberSummary = memberFeignHelper.getMemberSummaryById(authorId, token, response);

        // 본문 저장
        Post post = Post.newPost(request.getTitle(), request.getContent(), authorId);
        Post savedPost = postRepository.save(post);

        // mongodb 에 본문 내용 저장
        insertNewDocument(savedPost);

        // timeline 조회를 위한 활동 기록 이벤트 발행
        publishPostActivity(authorId, savedPost.getId(), PostActivityType.CREATE);

        // return
        return DailyfeedServerResponse.<PostDto.Post>builder()
                .content(postMapper.toPostDto(post, memberSummary))
                .status(HttpStatus.CREATED.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    public void insertNewDocument(Post post){
        PostDocument document = PostDocument
                .newPost(post.getId(), post.getTitle(), post.getContent(), post.getCreatedAt(), post.getUpdatedAt());
        postMongoRepository.save(document);
    }

    // 게시글 수정
    public DailyfeedServerResponse<PostDto.Post> updatePost(MemberDto.Member author, Long postId, PostDto.UpdatePostRequest request, String token, HttpServletResponse response) {
        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        // 작성자 권한 확인
        if (!post.isAuthor(author.getId())) {
            throw new PostUpdateForbiddenException();
        }

        MemberProfileDto.Summary memberSummary = memberFeignHelper.getMemberSummaryById(author.getId(), token, response);

        // 수정 요청 반영
        post.updatePost(request.getTitle(), request.getContent());

        // mongodb에 본문 내용 저장
        updateDocument(post);

        // timeline 조회를 위한 활동 기록 이벤트 발행
        publishPostActivity(author.getId(), post.getId(), PostActivityType.UPDATE);

        // return
        return DailyfeedServerResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(postMapper.toPostDto(post, memberSummary))
                .build();
    }

    public void updateDocument(Post post){
        PostDocument oldDocument = postMongoRepository
                .findByPostPkAndIsDeletedAndIsCurrent(post.getId(), Boolean.FALSE, Boolean.TRUE)
                .orElseThrow(PostNotFoundException::new);

//        oldDocument.markAsDeleted(Boolean.FALSE);
        PostDocument updatedPost = PostDocument.newUpdatedPost(oldDocument, post.getUpdatedAt());

        postMongoRepository.save(updatedPost);
    }

    public void publishLikeActivity(Long memberId, Long postId, PostLikeType postLikeType) {
        try{
            LocalDateTime now = postEventMapper.currentDateTime();
            String topicName = DateBasedTopicType.POST_LIKE_ACTIVITY.generateTopicName(now);
            PostDto.LikeActivityEvent activityEvent = postEventMapper.newLikeActivityEvent(postId, memberId, postLikeType, now);
            kafkaHelper.send(topicName, postId.toString(), activityEvent);
        }
        catch (Exception e){
            log.error("Error publishing post activity event: ", e);
            throw new KafkaNetworkErrorException();
        }
    }

    public void publishPostActivity(Long memberId, Long postId, PostActivityType activityType) {
        try{
            LocalDateTime now = postEventMapper.currentDateTime();
            String topicName = DateBasedTopicType.POST_ACTIVITY.generateTopicName(now);
            PostDto.PostActivityEvent activityEvent = postEventMapper.newPostActivityEvent(postId, memberId, activityType, now);
            kafkaHelper.send(topicName, postId.toString(), activityEvent);
        }
        catch (Exception e){
            log.error("Error publishing post activity event: ", e);
            throw new KafkaNetworkErrorException();
        }
    }

    // 게시글 삭제 (소프트 삭제)
    public DailyfeedServerResponse<Boolean> deletePost(MemberDto.Member author, Long postId, HttpServletResponse response) {
        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        // 작성자 권한 확인
        if (!post.isAuthor(author.getId())) {
            throw new PostDeleteForbiddenException();
        }

        // 관계형 데이터베이스에 데이터
        postRepository.softDeleteById(postId);

        // timeline 을 위한 활동 기록
        publishPostActivity(author.getId(), postId, PostActivityType.SOFT_DELETE);

        // mongodb
        deletePostDocument(post);

        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(Boolean.TRUE)
                .build();
    }

    public void deletePostDocument(Post post){
        PostDocument oldDocument = postMongoRepository
                .findByPostPkAndIsDeletedAndIsCurrent(post.getId(), Boolean.FALSE, Boolean.TRUE)
                .orElseThrow(PostNotFoundException::new);

        oldDocument.softDelete();
    }

    // 게시글 상세 조회 (조회수 증가)
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.PostService.WEB_GET_POST_BY_ID, key = "#postId", cacheManager = "redisCacheManager")
    public DailyfeedServerResponse<PostDto.Post> getPostById(MemberDto.Member member, Long postId, String token, HttpServletResponse response) {
        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        // 조회수 증가 (별도 트랜잭션으로 처리)
        post.incrementLikeCount();

        MemberProfileDto.Summary authorSummary = memberFeignHelper.getMemberSummaryById(post.getAuthorId(), token, response);

        // 작성자 정보 조회
        MemberDto.Member author = memberFeignHelper.getMemberById(post.getAuthorId(), token, response);

        return DailyfeedServerResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(postMapper.toPostDto(post, authorSummary))
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
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(Boolean.TRUE)
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
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(Boolean.TRUE)
                .build();
    }

    // 작성자별 게시글 목록 조회
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.PostService.WEB_GET_POSTS_BY_AUTHOR, key = "#authorId+'__page:'+#pageable.getPageNumber()+'_size:'+#pageable.getPageSize()", cacheManager = "redisCacheManager")
    public DailyfeedPageResponse<PostDto.Post> getPostsByAuthor(Long authorId, String token, Pageable pageable, HttpServletResponse httpResponse) {
        MemberDto.Member author = memberFeignHelper.getMemberById(authorId, token, httpResponse);
        if (author == null) {
            throw new MemberNotFoundException(() -> "삭제된 사용자입니다");
        }

        Page<Post> posts = postRepository.findByAuthorIdAndNotDeleted(author.getId(), pageable);
        DailyfeedPage<PostDto.Post> postDailyfeedPage = pageMapper.fromJpaPageToDailyfeedPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));

        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(postDailyfeedPage)
                .build();
    }

    // 댓글이 많은 게시글 조회 (댓글 수로 정렬)
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.PostService.WEB_GET_POSTS_ORDER_BY_COMMENT_COUNT, key = "'__page:'+#page+'_size:'+#size", cacheManager = "redisCacheManager")
    public DailyfeedPageResponse<PostDto.Post> getPostsOrderByCommentCount(int page, int size, HttpServletResponse httpResponse) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findMostCommentedPosts(pageable);

        DailyfeedPage<PostDto.Post> postDailyfeedPage = pageMapper.fromJpaPageToDailyfeedPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(postDailyfeedPage)
                .build();
    }

    // 인기 게시글 조회
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.PostService.WEB_STATISTICS_GET_POPULAR_POSTS, key = "'__page:'+#page+'_size:'+#size", cacheManager = "redisCacheManager")
    public DailyfeedPageResponse<PostDto.Post> getPopularPosts(int page, int size, HttpServletResponse httpResponse) {
        log.info("Getting popular posts");

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findPopularPostsNotDeleted(pageable);

        DailyfeedPage<PostDto.Post> postDailyfeedPage = pageMapper.fromJpaPageToDailyfeedPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(postDailyfeedPage)
                .build();
    }

    // 최근 댓글이 있는 게시글 조회
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.PostService.WEB_STATISTICS_GET_POSTS_BY_RECENT_ACTIVITY, key = "'__page:'+#page+'_size:'+#size", cacheManager = "redisCacheManager")
    public DailyfeedPageResponse<PostDto.Post> getPostsByRecentActivity(int page, int size, HttpServletResponse httpResponse) {
        log.info("Getting posts by recent activity");

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findPostsByRecentActivity(pageable);

        DailyfeedPage<PostDto.Post> postDailyfeedPage = pageMapper.fromJpaPageToDailyfeedPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(postDailyfeedPage)
                .build();
    }

    // 게시글 검색
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.PostService.WEB_SEARCH_SEARCH_POSTS, key = "#keyword+'__page:'+#page+'_size:'+#size", cacheManager = "redisCacheManager")
    public DailyfeedPageResponse<PostDto.Post> searchPosts(String keyword, int page, int size, HttpServletResponse httpResponse) {
        log.info("Searching posts with keyword: {}", keyword);

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByTitleOrContentContainingAndNotDeleted(keyword, pageable);

        DailyfeedPage<PostDto.Post> postDailyfeedPage = pageMapper.fromJpaPageToDailyfeedPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(postDailyfeedPage)
                .build();
    }

    public List<PostDto.Post> mergeAuthorAndCommentCount(List<Post> posts, HttpServletResponse httpResponse){
        // (1) 작성자 id 추출
        Set<Long> authorIds = posts.stream()
                .map(Post::getAuthorId)
                .collect(Collectors.toSet());

        // (2) 작성자 상세 정보
        Map<Long, MemberProfileDto.Summary> authorsMap = memberFeignHelper.getMemberMap(authorIds, httpResponse);

        return posts.stream()
                .map(post -> {
                    return postMapper.toPostDto(post, authorsMap.get(post.getAuthorId()), post.getCommentsCount());
                })
                .collect(Collectors.toList());
    }

    // 특정 기간 내 게시글 조회 (필요할지는 모르겠지만...)
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.PostService.WEB_SEARCH_GET_POSTS_BY_DATE_RANGE, keyGenerator = "datePeriodBasedPageKeyGenerator", cacheManager = "redisCacheManager")
    public DailyfeedPageResponse<PostDto.Post> getPostsByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size, HttpServletResponse httpResponse) {
        log.info("Getting posts between {} and {}", startDate, endDate);

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByCreatedDateBetweenAndNotDeleted(startDate, endDate, pageable);

        DailyfeedPage<PostDto.Post> postDailyfeedPage = pageMapper.fromJpaPageToDailyfeedPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(postDailyfeedPage)
                .build();
    }

    // 관리자용: 작성자별 게시글 일괄 삭제
    public int deletePostsByAuthor(Long authorId) {
        log.info("Admin deleting all posts by author: {}", authorId);
        return postRepository.softDeleteByAuthorId(authorId);
    }


    // 기본 기능 (REST API 기준으로만 짤때 만들었던 기능)
    @Transactional(readOnly = true)
    public DailyfeedPageResponse<PostDto.Post> getPosts(int page, int size, HttpServletResponse httpResponse) {
        log.info("Getting posts with paging - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findAllNotDeletedOrderByCreatedDateDesc(pageable);

        DailyfeedPage<PostDto.Post> postDailyfeedPage = pageMapper.fromJpaPageToDailyfeedPage(posts, mergeAuthorAndCommentCount(posts.getContent(), httpResponse));
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(postDailyfeedPage)
                .build();
    }
}
