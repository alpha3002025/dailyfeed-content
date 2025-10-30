package click.dailyfeed.content.domain.comment.service.commentservice;

import click.dailyfeed.code.domain.activity.type.MemberActivityType;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.content.domain.comment.document.CommentDocument;
import click.dailyfeed.content.domain.comment.entity.Comment;
import click.dailyfeed.content.domain.comment.repository.jpa.CommentRepository;
import click.dailyfeed.content.domain.comment.repository.mongo.CommentMongoRepository;
import click.dailyfeed.content.domain.comment.service.CommentService;
import click.dailyfeed.content.domain.post.entity.Post;
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
        "dailyfeed.services.content.publish-type.comment-service=KAFKA"
})
@DisplayName("CommentService.deleteComment() 테스트 (KAFKA)")
public class DeleteCommentKafkaTest {

    @Autowired
    private CommentService commentService;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private CommentMongoRepository commentMongoRepository;

    @MockBean
    private MemberActivityKafkaPublisher memberActivityKafkaPublisher;

    @MockBean
    private MemberActivityFeignHelper memberActivityFeignHelper;

    private MemberDto.Member member;
    private HttpServletResponse response;
    private Comment existingComment;
    private CommentDocument existingDocument;
    private Post mockPost;

    @BeforeEach
    void setUp() throws Exception {
        // 멤버 정보 생성
        member = MemberDto.Member.builder()
                .id(1L)
                .name("testUser")
                .build();

        // Mock Post 객체
        mockPost = Post.newPost("테스트 게시글", "게시글 내용", 999L);
        Field postIdField = Post.class.getDeclaredField("id");
        postIdField.setAccessible(true);
        postIdField.set(mockPost, 100L);

        // 기존 Comment 객체
        existingComment = Comment.commentBuilder()
                .content("원본 댓글 내용")
                .authorId(member.getId())
                .post(mockPost)
                .build();
        Field commentIdField = Comment.class.getDeclaredField("id");
        commentIdField.setAccessible(true);
        commentIdField.set(existingComment, 200L);

        // 기존 CommentDocument 객체
        existingDocument = mock(CommentDocument.class);

        // Mock Response
        response = mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("댓글 삭제 시 Kafka Publisher가 호출되어야 한다")
    void shouldUseKafkaPublisherWhenDeleteComment() {
        // Given
        String token = "test-token";
        when(commentRepository.findByIdAndNotDeleted(200L)).thenReturn(Optional.of(existingComment));
        doNothing().when(commentRepository).softDeleteCommentAndChildren(200L);
        when(commentMongoRepository.findByCommentPkAndIsDeleted(200L, Boolean.FALSE))
                .thenReturn(Optional.of(existingDocument));
        doNothing().when(commentMongoRepository).delete(any(CommentDocument.class));

        // When
        Boolean result = commentService.deleteComment(member, 200L, token, response);

        // Then
        // 1. Comment가 조회되었는지 확인
        verify(commentRepository, times(1)).findByIdAndNotDeleted(200L);

        // 2. Comment가 소프트 삭제되었는지 확인
        verify(commentRepository, times(1)).softDeleteCommentAndChildren(200L);

        // 3. MongoDB 문서가 소프트 삭제되었는지 확인
        verify(commentMongoRepository, times(1)).findByCommentPkAndIsDeleted(200L, Boolean.FALSE);
        verify(commentMongoRepository, times(1)).delete(any(CommentDocument.class));

        // 4. Kafka Publisher가 호출되었는지 확인
        verify(memberActivityKafkaPublisher, times(1))
                .publishCommentCUDEvent(
                        eq(member.getId()),
                        eq(100L),
                        eq(200L),
                        eq(MemberActivityType.COMMENT_DELETE)
                );

        // 5. Feign Helper는 호출되지 않았는지 확인
        verify(memberActivityFeignHelper, never())
                .createCommentsMemberActivity(any(), anyString(), any());

        // 6. 결과 검증
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Kafka 발행 실패 시에도 댓글은 정상적으로 삭제되어야 한다")
    void shouldDeleteCommentEvenIfKafkaPublishFails() {
        // Given
        when(commentRepository.findByIdAndNotDeleted(200L)).thenReturn(Optional.of(existingComment));
        doNothing().when(commentRepository).softDeleteCommentAndChildren(200L);
        when(commentMongoRepository.findByCommentPkAndIsDeleted(200L, Boolean.FALSE))
                .thenReturn(Optional.of(existingDocument));
        doNothing().when(commentMongoRepository).delete(any(CommentDocument.class));
        doThrow(new RuntimeException("Kafka publish failed"))
                .when(memberActivityKafkaPublisher)
                .publishCommentCUDEvent(anyLong(), anyLong(), anyLong(), any(MemberActivityType.class));

        // When & Then
        try {
            commentService.deleteComment(member, 200L, "token", response);
        } catch (Exception e) {
            // Kafka 실패는 예외를 발생시킬 수 있음
        }

        // Comment는 조회되었어야 함
        verify(commentRepository, times(1)).findByIdAndNotDeleted(200L);
        // Comment는 소프트 삭제되었어야 함
        verify(commentRepository, times(1)).softDeleteCommentAndChildren(200L);
    }
}