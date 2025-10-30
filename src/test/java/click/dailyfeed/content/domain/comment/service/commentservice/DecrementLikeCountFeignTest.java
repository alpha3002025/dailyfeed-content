package click.dailyfeed.content.domain.comment.service.commentservice;

import click.dailyfeed.code.domain.activity.dto.MemberActivityDto;
import click.dailyfeed.code.domain.activity.type.MemberActivityType;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.content.domain.comment.document.CommentLikeDocument;
import click.dailyfeed.content.domain.comment.entity.Comment;
import click.dailyfeed.content.domain.comment.repository.jpa.CommentRepository;
import click.dailyfeed.content.domain.comment.repository.mongo.CommentLikeMongoRepository;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles({"local-was-test"})
@SpringBootTest
@TestPropertySource(properties = {
        "dailyfeed.services.content.publish-type.comment-service=FEIGN"
})
@DisplayName("CommentService.decrementLikeCount() 테스트 (FEIGN)")
public class DecrementLikeCountFeignTest {

    @Autowired
    private CommentService commentService;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private CommentLikeMongoRepository commentLikeMongoRepository;

    @MockBean
    private MemberActivityKafkaPublisher memberActivityKafkaPublisher;

    @MockBean
    private MemberActivityFeignHelper memberActivityFeignHelper;

    private MemberDto.Member member;
    private HttpServletResponse response;
    private Comment existingComment;
    private CommentLikeDocument existingLikeDocument;
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
                .content("테스트 댓글 내용")
                .authorId(999L)
                .post(mockPost)
                .build();
        Field commentIdField = Comment.class.getDeclaredField("id");
        commentIdField.setAccessible(true);
        commentIdField.set(existingComment, 200L);

        // 기존 CommentLikeDocument 객체
        existingLikeDocument = mock(CommentLikeDocument.class);

        // Mock Response
        response = mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("좋아요 취소 시 Feign Helper가 호출되어야 한다")
    void shouldUseFeignHelperWhenDecrementLikeCount() {
        // Given
        String token = "test-token";
        when(commentRepository.findByIdAndNotDeleted(200L)).thenReturn(Optional.of(existingComment));
        when(commentLikeMongoRepository.findByCommentPkAndMemberId(200L, member.getId())).thenReturn(existingLikeDocument);
        doNothing().when(commentLikeMongoRepository).delete(any(CommentLikeDocument.class));
        when(memberActivityFeignHelper.createCommentLikeMemberActivity(any(), anyString(), any()))
                .thenReturn(mock(MemberActivityDto.MemberActivity.class));

        // When
        commentService.decrementLikeCount(member, 200L, token, response);

        // Then
        // 1. Comment가 조회되었는지 확인
        verify(commentRepository, times(1)).findByIdAndNotDeleted(200L);

        // 2. 기존 좋아요가 조회되었는지 확인
        verify(commentLikeMongoRepository, times(1)).findByCommentPkAndMemberId(200L, member.getId());

        // 3. CommentLikeDocument가 삭제되었는지 확인
        verify(commentLikeMongoRepository, times(1)).delete(any(CommentLikeDocument.class));

        // 4. Feign Helper가 호출되었는지 확인
        verify(memberActivityFeignHelper, times(1))
                .createCommentLikeMemberActivity(any(), eq(token), eq(response));

        // 5. Kafka Publisher는 호출되지 않았는지 확인
        verify(memberActivityKafkaPublisher, never())
                .publishCommentLikeEvent(anyLong(), anyLong(), anyLong(), any(MemberActivityType.class));
    }

    @Test
    @DisplayName("Feign 호출 실패 시에도 좋아요 취소는 정상적으로 처리되어야 한다")
    void shouldDecrementLikeCountEvenIfFeignCallFails() {
        // Given
        when(commentRepository.findByIdAndNotDeleted(200L)).thenReturn(Optional.of(existingComment));
        when(commentLikeMongoRepository.findByCommentPkAndMemberId(200L, member.getId())).thenReturn(existingLikeDocument);
        doNothing().when(commentLikeMongoRepository).delete(any(CommentLikeDocument.class));
        doThrow(new RuntimeException("Feign call failed"))
                .when(memberActivityFeignHelper)
                .createCommentLikeMemberActivity(any(), anyString(), any());

        // When & Then
        try {
            commentService.decrementLikeCount(member, 200L, "token", response);
        } catch (Exception e) {
            // Feign 실패는 예외를 발생시킬 수 있음
        }

        // Comment는 조회되었어야 함
        verify(commentRepository, times(1)).findByIdAndNotDeleted(200L);
        // CommentLikeDocument는 삭제되었어야 함
        verify(commentLikeMongoRepository, times(1)).delete(any(CommentLikeDocument.class));
    }
}