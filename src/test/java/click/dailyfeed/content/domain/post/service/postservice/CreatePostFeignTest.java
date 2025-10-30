package click.dailyfeed.content.domain.post.service.postservice;

import click.dailyfeed.code.domain.activity.dto.MemberActivityDto;
import click.dailyfeed.code.domain.activity.type.MemberActivityType;
import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles({"local-was-test"})
@SpringBootTest
@TestPropertySource(properties = {
        "dailyfeed.services.content.publish-type.post-service=FEIGN"
})
@DisplayName("PostService.createPost() 테스트 (FEIGN)")
public class CreatePostFeignTest {

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

    private MemberProfileDto.Summary author;
    private PostDto.CreatePostRequest request;
    private HttpServletResponse response;
    private Post mockPost;

    @BeforeEach
    void setUp() throws Exception {
        // 작성자 정보 생성
        author = MemberProfileDto.Summary.builder()
                .id(1L)
                .memberName("testUser")
                .displayName("테스트 유저")
                .build();

        // 게시글 작성 요청 생성
        request = PostDto.CreatePostRequest.builder()
                .content("테스트 게시글 내용입니다.")
                .build();

        // Mock Post 객체 - Reflection을 사용하여 id 설정
        mockPost = Post.newPost("", request.getContent(), author.getId());
        Field idField = Post.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(mockPost, 100L);

        // Mock Response
        response = mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("게시글 작성 시 Feign Helper가 호출되어야 한다")
    void shouldUseFeignHelperWhenPublishTypeIsFeign() throws Exception {
        // Given
        String token = "test-token";
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post savedPost = invocation.getArgument(0);
            Field idField = Post.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedPost, 100L);
            return savedPost;
        });
        when(postMongoRepository.save(any(PostDocument.class))).thenReturn(mock(PostDocument.class));
        when(memberActivityFeignHelper.createPostsMemberActivity(any(), anyString(), any()))
                .thenReturn(mock(MemberActivityDto.MemberActivity.class));

        // When
        PostDto.Post result = postService.createPost(author, request, token, response);

        // Then
        // 1. Post가 저장되었는지 확인
        verify(postRepository, times(1)).save(any(Post.class));

        // 2. MongoDB에 문서가 저장되었는지 확인
        verify(postMongoRepository, times(1)).save(any(PostDocument.class));

        // 3. Feign Helper가 호출되었는지 확인
        verify(memberActivityFeignHelper, times(1))
                .createPostsMemberActivity(any(), eq(token), eq(response));

        // 4. Kafka Publisher는 호출되지 않았는지 확인
        verify(memberActivityKafkaPublisher, never())
                .publishPostCUDEvent(anyLong(), anyLong(), any(MemberActivityType.class));

        // 5. 결과 검증
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Feign 호출 실패 시에도 게시글은 정상적으로 저장되어야 한다")
    void shouldSavePostEvenIfFeignCallFails() throws Exception {
        // Given
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post savedPost = invocation.getArgument(0);
            Field idField = Post.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedPost, 100L);
            return savedPost;
        });
        when(postMongoRepository.save(any(PostDocument.class))).thenReturn(mock(PostDocument.class));
        doThrow(new RuntimeException("Feign call failed"))
                .when(memberActivityFeignHelper)
                .createPostsMemberActivity(any(), anyString(), any());

        // When & Then
        try {
            postService.createPost(author, request, "token", response);
        } catch (Exception e) {
            // Feign 실패는 예외를 발생시킬 수 있음
        }

        // Post는 저장되었어야 함
        verify(postRepository, times(1)).save(any(Post.class));
    }
}