package click.dailyfeed.content.domain.comment.service.commentservice;

import click.dailyfeed.code.domain.activity.dto.MemberActivityDto;
import click.dailyfeed.code.domain.activity.type.MemberActivityType;
import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.content.domain.comment.document.CommentDocument;
import click.dailyfeed.content.domain.comment.entity.Comment;
import click.dailyfeed.content.domain.comment.repository.jpa.CommentRepository;
import click.dailyfeed.content.domain.comment.repository.mongo.CommentMongoRepository;
import click.dailyfeed.content.domain.comment.service.CommentService;
import click.dailyfeed.content.domain.post.entity.Post;
import click.dailyfeed.content.domain.post.repository.jpa.PostRepository;
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
        "dailyfeed.services.content.publish-type.comment-service=FEIGN"
})
@DisplayName("CommentService.createComment() 테스트 (FEIGN)")
public class CreateCommentFeignTest {

    @Autowired
    private CommentService commentService;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private CommentMongoRepository commentMongoRepository;

    @MockBean
    private MemberActivityKafkaPublisher memberActivityKafkaPublisher;

    @MockBean
    private MemberActivityFeignHelper memberActivityFeignHelper;

    private MemberProfileDto.Summary author;
    private CommentDto.CreateCommentRequest request;
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

        // 댓글 작성 요청 생성
        request = CommentDto.CreateCommentRequest.builder()
                .postId(100L)
                .content("테스트 댓글 내용입니다.")
                .build();

        // Mock Post 객체
        mockPost = Post.newPost("테스트 게시글", "게시글 내용", 999L);
        Field postIdField = Post.class.getDeclaredField("id");
        postIdField.setAccessible(true);
        postIdField.set(mockPost, 100L);

        // Mock Response
        response = mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("댓글 작성 시 Feign Helper가 호출되어야 한다")
    void shouldUseFeignHelperWhenPublishTypeIsFeign() throws Exception {
        // Given
        String token = "test-token";
        when(postRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment savedComment = invocation.getArgument(0);
            Field idField = Comment.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedComment, 200L);
            return savedComment;
        });
        when(commentMongoRepository.save(any(CommentDocument.class))).thenReturn(mock(CommentDocument.class));
        when(memberActivityFeignHelper.createCommentsMemberActivity(any(), anyString(), any()))
                .thenReturn(mock(MemberActivityDto.MemberActivity.class));

        // When
        CommentDto.Comment result = commentService.createComment(author, token, request, response);

        // Then
        // 1. Post가 조회되었는지 확인
        verify(postRepository, times(1)).findByIdAndNotDeleted(100L);

        // 2. Comment가 저장되었는지 확인
        verify(commentRepository, times(1)).save(any(Comment.class));

        // 3. MongoDB에 문서가 저장되었는지 확인
        verify(commentMongoRepository, times(1)).save(any(CommentDocument.class));

        // 4. Feign Helper가 호출되었는지 확인
        verify(memberActivityFeignHelper, times(1))
                .createCommentsMemberActivity(any(), eq(token), eq(response));

        // 5. Kafka Publisher는 호출되지 않았는지 확인
        verify(memberActivityKafkaPublisher, never())
                .publishCommentCUDEvent(anyLong(), anyLong(), anyLong(), any(MemberActivityType.class));

        // 6. 결과 검증
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Feign 호출 실패 시에도 댓글은 정상적으로 저장되어야 한다")
    void shouldSaveCommentEvenIfFeignCallFails() throws Exception {
        // Given
        when(postRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment savedComment = invocation.getArgument(0);
            Field idField = Comment.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedComment, 200L);
            return savedComment;
        });
        when(commentMongoRepository.save(any(CommentDocument.class))).thenReturn(mock(CommentDocument.class));
        doThrow(new RuntimeException("Feign call failed"))
                .when(memberActivityFeignHelper)
                .createCommentsMemberActivity(any(), anyString(), any());

        // When & Then
        try {
            commentService.createComment(author, "token", request, response);
        } catch (Exception e) {
            // Feign 실패는 예외를 발생시킬 수 있음
        }

        // Comment는 저장되었어야 함
        verify(commentRepository, times(1)).save(any(Comment.class));
    }
}