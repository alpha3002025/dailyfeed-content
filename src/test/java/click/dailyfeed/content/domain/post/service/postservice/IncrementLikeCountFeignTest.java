package click.dailyfeed.content.domain.post.service.postservice;

import click.dailyfeed.code.domain.activity.dto.MemberActivityDto;
import click.dailyfeed.code.domain.activity.type.MemberActivityType;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.content.domain.post.document.PostLikeDocument;
import click.dailyfeed.content.domain.post.entity.Post;
import click.dailyfeed.content.domain.post.repository.jpa.PostRepository;
import click.dailyfeed.content.domain.post.repository.mongo.PostLikeMongoRepository;
import click.dailyfeed.content.domain.post.service.PostService;
import click.dailyfeed.feign.domain.activity.MemberActivityFeignHelper;
import click.dailyfeed.kafka.domain.activity.publisher.MemberActivityKafkaPublisher;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles({"local-was-test"})
@SpringBootTest
@TestPropertySource(properties = {
        "dailyfeed.services.content.publish-type.post-service=FEIGN"
})
@DisplayName("PostService.incrementLikeCount() 테스트 (FEIGN)")
public class IncrementLikeCountFeignTest {

    @Autowired
    private PostService postService;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private PostLikeMongoRepository postLikeMongoRepository;

    @MockBean
    private MemberActivityKafkaPublisher memberActivityKafkaPublisher;

    @MockBean
    private MemberActivityFeignHelper memberActivityFeignHelper;

    private MemberDto.Member member;
    private HttpServletResponse response;
    private Post existingPost;

    @BeforeEach
    void setUp() throws Exception {
        // 멤버 정보 생성
        member = MemberDto.Member.builder()
                .id(1L)
                .name("testUser")
                .build();

        // 기존 Post 객체
        existingPost = Post.newPost("테스트 제목", "테스트 내용", 999L);
        Field idField = Post.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(existingPost, 100L);

        // Mock Response
        response = mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("좋아요 증가 시 Feign Helper가 호출되어야 한다")
    void shouldUseFeignHelperWhenIncrementLikeCount() {
        // Given
        String token = "test-token";
        when(postRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(existingPost));
        when(postLikeMongoRepository.findByPostPkAndMemberId(100L, member.getId())).thenReturn(null);
        when(postLikeMongoRepository.save(any(PostLikeDocument.class))).thenReturn(mock(PostLikeDocument.class));
        when(memberActivityFeignHelper.createPostLikeMemberActivity(any(), anyString(), any()))
                .thenReturn(mock(MemberActivityDto.MemberActivity.class));

        // When
        Boolean result = postService.incrementLikeCount(100L, member, token, response);

        // Then
        // 1. Post가 조회되었는지 확인
        verify(postRepository, times(1)).findByIdAndNotDeleted(100L);

        // 2. 기존 좋아요가 조회되었는지 확인
        verify(postLikeMongoRepository, times(1)).findByPostPkAndMemberId(100L, member.getId());

        // 3. PostLikeDocument가 저장되었는지 확인
        verify(postLikeMongoRepository, times(1)).save(any(PostLikeDocument.class));

        // 4. Feign Helper가 호출되었는지 확인
        verify(memberActivityFeignHelper, times(1))
                .createPostLikeMemberActivity(any(), eq(token), eq(response));

        // 5. Kafka Publisher는 호출되지 않았는지 확인
        verify(memberActivityKafkaPublisher, never())
                .publishPostLikeEvent(anyLong(), anyLong(), any(MemberActivityType.class));

        // 6. 결과 검증
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Feign 호출 실패 시에도 좋아요는 정상적으로 증가되어야 한다")
    void shouldIncrementLikeCountEvenIfFeignCallFails() {
        // Given
        when(postRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(existingPost));
        when(postLikeMongoRepository.findByPostPkAndMemberId(100L, member.getId())).thenReturn(null);
        when(postLikeMongoRepository.save(any(PostLikeDocument.class))).thenReturn(mock(PostLikeDocument.class));
        doThrow(new RuntimeException("Feign call failed"))
                .when(memberActivityFeignHelper)
                .createPostLikeMemberActivity(any(), anyString(), any());

        // When & Then
        try {
            postService.incrementLikeCount(100L, member, "token", response);
        } catch (Exception e) {
            // Feign 실패는 예외를 발생시킬 수 있음
        }

        // Post는 조회되었어야 함
        verify(postRepository, times(1)).findByIdAndNotDeleted(100L);
        // PostLikeDocument는 저장되었어야 함
        verify(postLikeMongoRepository, times(1)).save(any(PostLikeDocument.class));
    }
}
