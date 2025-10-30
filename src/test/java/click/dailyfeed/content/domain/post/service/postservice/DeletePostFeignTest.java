package click.dailyfeed.content.domain.post.service.postservice;

import click.dailyfeed.code.domain.activity.dto.MemberActivityDto;
import click.dailyfeed.code.domain.activity.type.MemberActivityType;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.content.domain.post.document.PostDocument;
import click.dailyfeed.content.domain.post.entity.Post;
import click.dailyfeed.content.domain.post.repository.jpa.PostRepository;
import click.dailyfeed.content.domain.post.repository.mongo.PostMongoRepository;
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
@DisplayName("PostService.deletePost() 테스트 (FEIGN)")
public class DeletePostFeignTest {

    @Autowired
    private PostService postService;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private PostMongoRepository postMongoRepository;

    @MockBean
    private MemberActivityKafkaPublisher memberActivityKafkaPublisher;

    @MockBean
    private MemberActivityFeignHelper memberActivityFeignHelper;

    private MemberDto.Member author;
    private HttpServletResponse response;
    private Post existingPost;
    private PostDocument existingDocument;

    @BeforeEach
    void setUp() throws Exception {
        // 작성자 정보 생성
        author = MemberDto.Member.builder()
                .id(1L)
                .name("testUser")
                .build();

        // 기존 Post 객체
        existingPost = Post.newPost("원본 제목", "원본 내용", author.getId());
        Field idField = Post.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(existingPost, 100L);

        // 기존 PostDocument 객체
        existingDocument = mock(PostDocument.class);

        // Mock Response
        response = mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("게시글 삭제 시 Feign Helper가 호출되어야 한다")
    void shouldUseFeignHelperWhenPublishTypeIsFeign() {
        // Given
        String token = "test-token";
        when(postRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(existingPost));
        doNothing().when(postRepository).softDeleteById(100L);
        when(postMongoRepository.findByPostPkAndIsDeleted(100L, Boolean.FALSE))
                .thenReturn(Optional.of(existingDocument));
        when(memberActivityFeignHelper.createPostsMemberActivity(any(), anyString(), any()))
                .thenReturn(mock(MemberActivityDto.MemberActivity.class));

        // When
        Boolean result = postService.deletePost(author, 100L, token, response);

        // Then
        // 1. Post가 조회되었는지 확인
        verify(postRepository, times(1)).findByIdAndNotDeleted(100L);

        // 2. Post가 소프트 삭제되었는지 확인
        verify(postRepository, times(1)).softDeleteById(100L);

        // 3. MongoDB 문서가 소프트 삭제되었는지 확인
        verify(postMongoRepository, times(1)).findByPostPkAndIsDeleted(100L, Boolean.FALSE);
        verify(existingDocument, times(1)).softDelete();

        // 4. Feign Helper가 호출되었는지 확인
        verify(memberActivityFeignHelper, times(1))
                .createPostsMemberActivity(any(), eq(token), eq(response));

        // 5. Kafka Publisher는 호출되지 않았는지 확인
        verify(memberActivityKafkaPublisher, never())
                .publishPostCUDEvent(anyLong(), anyLong(), any(MemberActivityType.class));

        // 6. 결과 검증
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Feign 호출 실패 시에도 게시글은 정상적으로 삭제되어야 한다")
    void shouldDeletePostEvenIfFeignCallFails() {
        // Given
        when(postRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(existingPost));
        doNothing().when(postRepository).softDeleteById(100L);
        when(postMongoRepository.findByPostPkAndIsDeleted(100L, Boolean.FALSE))
                .thenReturn(Optional.of(existingDocument));
        doThrow(new RuntimeException("Feign call failed"))
                .when(memberActivityFeignHelper)
                .createPostsMemberActivity(any(), anyString(), any());

        // When & Then
        try {
            postService.deletePost(author, 100L, "token", response);
        } catch (Exception e) {
            // Feign 실패는 예외를 발생시킬 수 있음
        }

        // Post는 조회되었어야 함
        verify(postRepository, times(1)).findByIdAndNotDeleted(100L);
        // Post는 소프트 삭제되었어야 함
        verify(postRepository, times(1)).softDeleteById(100L);
    }
}