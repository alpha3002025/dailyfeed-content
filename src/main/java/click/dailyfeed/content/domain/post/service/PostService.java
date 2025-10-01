package click.dailyfeed.content.domain.post.service;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.content.post.exception.*;
import click.dailyfeed.code.domain.content.post.type.PostActivityType;
import click.dailyfeed.code.domain.content.post.type.PostLikeType;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.timeline.statistics.TimelineStatisticsDto;
import click.dailyfeed.code.global.kafka.exception.KafkaNetworkErrorException;
import click.dailyfeed.code.global.kafka.type.DateBasedTopicType;
import click.dailyfeed.content.domain.post.document.PostDocument;
import click.dailyfeed.content.domain.post.document.PostLikeDocument;
import click.dailyfeed.content.domain.post.entity.Post;
import click.dailyfeed.content.domain.post.mapper.PostEventMapper;
import click.dailyfeed.content.domain.post.mapper.PostMapper;
import click.dailyfeed.content.domain.post.repository.jpa.PostRepository;
import click.dailyfeed.content.domain.post.repository.mongo.PostLikeMongoRepository;
import click.dailyfeed.content.domain.post.repository.mongo.PostMongoRepository;
import click.dailyfeed.feign.domain.timeline.TimelineFeignHelper;
import click.dailyfeed.kafka.domain.kafka.service.KafkaHelper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class PostService {
    private final PostRepository postRepository;
    private final PostMongoRepository postMongoRepository;
    private final PostLikeMongoRepository postLikeMongoRepository;
    private final PostMapper postMapper;
    private final PostEventMapper postEventMapper;
    private final TimelineFeignHelper timelineFeignHelper;
    private final KafkaHelper kafkaHelper;

    // 게시글 작성
    public PostDto.Post createPost(MemberProfileDto.Summary author, PostDto.CreatePostRequest request, String token, HttpServletResponse response) {
        // 작성자 정보 확인
        Long authorId = author.getId();

        // 본문 저장 (제목 기능을 그대로 둘지 아직 결정을 못해서 일단은 첫 문장만 떼어두기로 (요약 등..))
        Post post = Post.newPost("", request.getContent(), authorId);
        Post savedPost = postRepository.save(post);

        // mongodb 에 본문 내용 저장
        insertNewDocument(savedPost);

        // timeline 조회를 위한 활동 기록 이벤트 발행  TODO :: feat/member/member-activity-logger-v1-0001
        publishPostActivity(authorId, savedPost.getId(), PostActivityType.CREATE);

        // return
        return postMapper.fromCreatedPost(post, author);
    }

    public void insertNewDocument(Post post){
        PostDocument document = PostDocument
                .newPost(post.getId(), post.getTitle(), post.getContent(), post.getCreatedAt(), post.getUpdatedAt());
        postMongoRepository.save(document);
    }

    // 게시글 수정
    public PostDto.Post updatePost(MemberProfileDto.Summary author, Long postId, PostDto.UpdatePostRequest request, String token, HttpServletResponse response) {
        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        // 작성자 권한 확인
        if (!post.isAuthor(author.getId())) {
            throw new PostUpdateForbiddenException();
        }

        // 수정 요청 반영
        post.updatePost(request.getTitle(), request.getContent());

        // mongodb에 본문 내용 저장
        updateDocument(post);

        // timeline 조회를 위한 활동 기록 이벤트 발행 TODO :: feat/member/member-activity-logger-v1-0001
        publishPostActivity(author.getId(), post.getId(), PostActivityType.UPDATE);

        TimelineStatisticsDto.PostItemCounts postItemCounts = timelineFeignHelper.getPostItemCounts(post.getId(), token, response);

        return postMapper.fromUpdatedPost(post, author, postItemCounts);
    }

    public void updateDocument(Post post){
        PostDocument oldDocument = postMongoRepository
                .findByPostPkAndIsDeleted(post.getId(), Boolean.FALSE)
                .orElseThrow(PostNotFoundException::new);

        oldDocument.softDelete();
        postMongoRepository.save(oldDocument);
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
            PostDto.PostActivityEvent activityEvent = postEventMapper.newPostActivityEvent(memberId, postId, activityType, now);
            kafkaHelper.send(topicName, postId.toString(), activityEvent);
        }
        catch (Exception e){
            log.error("Error publishing post activity event: ", e);
            throw new KafkaNetworkErrorException();
        }
    }

    // 게시글 삭제 (소프트 삭제)
    public Boolean deletePost(MemberDto.Member author, Long postId, HttpServletResponse response) {
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

        return Boolean.TRUE;
    }

    public void deletePostDocument(Post post){
        PostDocument oldDocument = postMongoRepository
                .findByPostPkAndIsDeleted(post.getId(), Boolean.FALSE)
                .orElseThrow(PostNotFoundException::new);

        oldDocument.softDelete();
    }

    // 게시글 좋아요 증가
    public Boolean incrementLikeCount(Long postId, MemberDto.Member member) {
        // 게시글 존재 확인
        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        PostLikeDocument existDocument = postLikeMongoRepository.findByPostPkAndMemberId(post.getId(), member.getId());
        if (existDocument != null) {
            throw new PostLikeAlreadyExistsException();
        }

        PostLikeDocument postLikeDocument = PostLikeDocument.newPostLikeBuilder()
                .postPk(post.getId())
                .memberId(member.getId())
                .build();

        postLikeMongoRepository.save(postLikeDocument);
        return Boolean.TRUE;
    }

    // 게시글 좋아요 감소
    public Boolean decrementLikeCount(Long postId, MemberDto.Member member) {
        // 게시글 존재 확인
        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        PostLikeDocument existDocument = postLikeMongoRepository.findByPostPkAndMemberId(post.getId(), member.getId());
        if (existDocument == null) {
            throw new PostLikeCancelAlreadyCommittedException();
        }

        PostLikeDocument postLikeDocument = postLikeMongoRepository.findByPostPkAndMemberId(post.getId(), member.getId());
        postLikeMongoRepository.deleteById(postLikeDocument.getId());

        return Boolean.TRUE;
    }
}
