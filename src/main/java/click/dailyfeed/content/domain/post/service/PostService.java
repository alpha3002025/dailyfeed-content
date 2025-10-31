package click.dailyfeed.content.domain.post.service;

import click.dailyfeed.code.domain.activity.dto.MemberActivityDto;
import click.dailyfeed.code.domain.activity.type.MemberActivityType;
import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.content.post.exception.*;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.timeline.statistics.TimelineStatisticsDto;
import click.dailyfeed.code.global.feign.exception.FeignApiCommunicationFailException;
import click.dailyfeed.code.global.kafka.exception.KafkaMessageKeyCreationException;
import click.dailyfeed.code.global.kafka.exception.KafkaNetworkErrorException;
import click.dailyfeed.code.global.system.type.PublishType;
import click.dailyfeed.code.global.web.excecption.DailyfeedWebTooManyRequestException;
import click.dailyfeed.content.domain.post.document.PostDocument;
import click.dailyfeed.content.domain.post.document.PostLikeDocument;
import click.dailyfeed.content.domain.post.entity.Post;
import click.dailyfeed.content.domain.post.mapper.PostMapper;
import click.dailyfeed.content.domain.post.repository.jpa.PostRepository;
import click.dailyfeed.content.domain.post.repository.mongo.PostLikeMongoRepository;
import click.dailyfeed.content.domain.post.repository.mongo.PostMongoRepository;
import click.dailyfeed.deadletter.domain.deadletter.service.FeignDeadLetterService;
import click.dailyfeed.deadletter.domain.deadletter.service.KafkaPublisherDeadLetterService;
import click.dailyfeed.feign.domain.activity.MemberActivityFeignHelper;
import click.dailyfeed.feign.domain.timeline.TimelineFeignHelper;
import click.dailyfeed.kafka.domain.activity.publisher.MemberActivityKafkaPublisher;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
@Service
public class PostService {
    private final PostRepository postRepository;
    private final PostMongoRepository postMongoRepository;
    private final PostLikeMongoRepository postLikeMongoRepository;

    private final FeignDeadLetterService feignDeadLetterService;
    private final KafkaPublisherDeadLetterService kafkaPublisherDeadLetterService;

    private final PostMapper postMapper;

    private final TimelineFeignHelper timelineFeignHelper;
    private final MemberActivityFeignHelper memberActivityFeignHelper;
    private final MemberActivityKafkaPublisher memberActivityKafkaPublisher;

    @Value("${dailyfeed.services.content.publish-type.post-service}")
    private String publishType;

    // 게시글 작성
    public PostDto.Post createPost(MemberProfileDto.Summary author, PostDto.CreatePostRequest request, String token, HttpServletResponse response) {

        // 작성자 정보 확인
        Long authorId = author.getId();

        // 본문 저장 (제목 기능을 그대로 둘지 아직 결정을 못해서 일단은 첫 문장만 떼어두기로 (요약 등..))
        Post post = Post.newPost("", request.getContent(), authorId);
        Post savedPost = postRepository.save(post);

        // mongodb 에 본문 내용 저장
        insertNewDocument(savedPost);

        if (PublishType.KAFKA.getCode().equals(publishType)) { /// kafka 를 사용할 경우 (케이스 A)
            kafkaPublishPostEvent(savedPost, MemberActivityType.POST_CREATE);
        }
        else{ /// feign 을 사용할 경우 (케이스 B)
            feignPublishPostEvent(savedPost, MemberActivityType.POST_CREATE, token, response);
        }
        // return
        return postMapper.fromCreatedPost(savedPost, author);
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

        TimelineStatisticsDto.PostItemCounts postItemCounts = timelineFeignHelper.getPostItemCounts(post.getId(), token, response);

        if (PublishType.KAFKA.getCode().equals(publishType)) { /// kafka 를 사용할 경우 (케이스 A)
            kafkaPublishPostEvent(post, MemberActivityType.POST_UPDATE);
        } else { /// feign 을 사용할 경우 (케이스 B)
            feignPublishPostEvent(post, MemberActivityType.POST_UPDATE, token, response);
        }

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

    // 게시글 삭제 (소프트 삭제)
    public Boolean deletePost(MemberDto.Member author, Long postId, String token, HttpServletResponse response) {
        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        // 작성자 권한 확인
        if (!post.isAuthor(author.getId())) {
            throw new PostDeleteForbiddenException();
        }

        // 관계형 데이터베이스에 데이터
        postRepository.softDeleteById(postId);

        // mongodb
        deletePostDocument(post);

        if (PublishType.KAFKA.getCode().equals(publishType)) { /// kafka 를 사용할 경우 (케이스 A)
            kafkaPublishPostEvent(post, MemberActivityType.POST_DELETE);
        } else { /// feign 을 사용할 경우 (케이스 B)
            feignPublishPostEvent(post, MemberActivityType.POST_DELETE, token, response);
        }

        return Boolean.TRUE;
    }

    public void deletePostDocument(Post post){
        PostDocument oldDocument = postMongoRepository
                .findByPostPkAndIsDeleted(post.getId(), Boolean.FALSE)
                .orElseThrow(PostNotFoundException::new);

        oldDocument.softDelete();
    }

    /**
     * 게시글 작성/수정/삭제 기록 이벤트 kafka 요청
     */
    public void kafkaPublishPostEvent(Post post, MemberActivityType activityType){
        try {
            // 멤버 활동 기록 조회를 위한 활동 기록 이벤트 발행
            memberActivityKafkaPublisher.publishPostCUDEvent(post.getAuthorId(), post.getId(), activityType);
        } catch (KafkaMessageKeyCreationException e){
            throw new KafkaMessageKeyCreationException();
        } catch (DailyfeedWebTooManyRequestException e){
            throw new DailyfeedWebTooManyRequestException();
        } catch (Exception e){
            MemberActivityDto.PostActivityRequest postActivityRequest = MemberActivityDto.PostActivityRequest
                    .builder()
                    .memberId(post.getAuthorId()).postId(post.getId()).activityType(activityType)
                    .build();

            try {
                kafkaPublisherDeadLetterService.createPostActivityDeadLetter(postActivityRequest);
            } catch (Exception e1){
                throw new KafkaNetworkErrorException();
            }
        }
    }

    /**
     * 게시글 작성/수정/삭제 기록 이벤트 Feign 요청
     */
    public void feignPublishPostEvent(Post post, MemberActivityType activityType, String token, HttpServletResponse response){
        MemberActivityDto.PostActivityRequest feignRequest = postMapper.postActivityFeignRequest(post.getAuthorId(), post.getId(), activityType);
        try{
            memberActivityFeignHelper.createPostsMemberActivity(feignRequest, token, response);
        } catch (DailyfeedWebTooManyRequestException e){
            throw new DailyfeedWebTooManyRequestException();
        } catch (Exception e){
            try{
                feignDeadLetterService.createPostActivityDeadLetter(feignRequest);
            } catch (Exception e1) {
                throw new FeignApiCommunicationFailException();
            }
        }
    }

    // 게시글 좋아요 증가
    public Boolean incrementLikeCount(Long postId, MemberDto.Member member, String token, HttpServletResponse response) {
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

        // 멤버 활동 기록 조회를 위한 활동 기록 이벤트 발행
        if (PublishType.KAFKA.getCode().equals(publishType)) { /// kafka 를 사용할 경우 (케이스 A)
            kafkaPublishPostLikeEvent(member, post, MemberActivityType.LIKE_POST);
        } else { /// feign 을 사용할 경우 (케이스 B)
            feignPublishPostLikeEvent(post, MemberActivityType.LIKE_POST, token, response);
        }

        return Boolean.TRUE;
    }

    // 게시글 좋아요 감소
    public Boolean decrementLikeCount(Long postId, MemberDto.Member member, String token, HttpServletResponse response) {
        // 게시글 존재 확인
        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        PostLikeDocument existDocument = postLikeMongoRepository.findByPostPkAndMemberId(post.getId(), member.getId());
        if (existDocument == null) {
            throw new PostLikeCancelAlreadyCommittedException();
        }
        postLikeMongoRepository.deleteById(existDocument.getId());

        if (PublishType.KAFKA.getCode().equals(publishType)) { /// kafka 를 사용할 경우 (케이스 A)
            kafkaPublishPostLikeEvent(member, post, MemberActivityType.LIKE_POST_CANCEL);
        } else { /// feign 을 사용할 경우 (케이스 B)
            feignPublishPostLikeEvent(post, MemberActivityType.LIKE_POST_CANCEL, token, response);
        }

        return Boolean.TRUE;
    }

    /**
     * 게시글 좋아요 기록 이벤트 kafka 요청
     */
    public void kafkaPublishPostLikeEvent(MemberDto.Member member, Post post, MemberActivityType activityType){
        try {
            // 멤버 활동 기록 조회를 위한 활동 기록 이벤트 발행
            memberActivityKafkaPublisher.publishPostLikeEvent(member.getId(), post.getId(), activityType);
        }
        catch (KafkaMessageKeyCreationException e){
            throw new KafkaMessageKeyCreationException();
        } catch (DailyfeedWebTooManyRequestException e){
            throw new DailyfeedWebTooManyRequestException();
        } catch (Exception e){
            MemberActivityDto.PostLikeActivityRequest activityRequest = MemberActivityDto.PostLikeActivityRequest
                    .builder()
                    .memberId(post.getAuthorId()).postId(post.getId()).activityType(activityType)
                    .build();
            try {
                kafkaPublisherDeadLetterService.createPostLikeActivityDeadLetter(activityRequest);
            } catch (Exception e1) {
                throw new KafkaNetworkErrorException();
            }
        }
    }

    /**
     * 게시글 좋아요 기록 이벤트 Feign 요청
     */
    public void feignPublishPostLikeEvent(Post post, MemberActivityType activityType, String token, HttpServletResponse response){
        MemberActivityDto.PostLikeActivityRequest feignRequest = postMapper.postLikeActivityFeignRequest(post.getAuthorId(), post.getId(), activityType);
        try {
            memberActivityFeignHelper.createPostLikeMemberActivity(feignRequest, token, response);
        } catch (DailyfeedWebTooManyRequestException e){
            throw new DailyfeedWebTooManyRequestException();
        } catch (Exception e){
            try {
                feignDeadLetterService.createPostLikeActivityDeadLetter(feignRequest);
            } catch (Exception e1) {
                throw new FeignApiCommunicationFailException();
            }
        }
    }
}
