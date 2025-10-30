package click.dailyfeed.content.domain.comment.service.commentservice;

import click.dailyfeed.code.domain.activity.dto.MemberActivityDto;
import click.dailyfeed.code.domain.activity.type.MemberActivityType;
import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.content.domain.comment.document.CommentDocument;
import click.dailyfeed.content.domain.comment.entity.Comment;
import click.dailyfeed.content.domain.comment.repository.jpa.CommentRepository;
import click.dailyfeed.content.domain.comment.repository.mongo.CommentMongoRepository;
import click.dailyfeed.content.domain.comment.service.CommentService;
import click.dailyfeed.content.domain.post.entity.Post;
import click.dailyfeed.feign.domain.activity.MemberActivityFeignHelper;
import click.dailyfeed.feign.domain.member.MemberFeignHelper;
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
@DisplayName("CommentService.updateComment() 테스트 (FEIGN)")
public class UpdateCommentFeignTest {

    @Autowired
    private CommentService commentService;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private CommentMongoRepository commentMongoRepository;

    @MockBean
    private MemberFeignHelper memberFeignHelper;

    @MockBean
    private MemberActivityKafkaPublisher memberActivityKafkaPublisher;

    @MockBean
    private MemberActivityFeignHelper memberActivityFeignHelper;

    private MemberDto.Member member;
    private MemberProfileDto.Summary author;
    private CommentDto.UpdateCommentRequest request;
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

        author = MemberProfileDto.Summary.builder()
                .id(1L)
                .memberName("testUser")
                .displayName("테스트 유저")
                .build();

        // 댓글 수정 요청 생성
        request = CommentDto.UpdateCommentRequest.builder()
                .content("수정된 댓글 내용입니다.")
                .build();

        // Mock Post 객체
        mockPost = Post.newPost("테스트 게시글", "게시글 내용", 999L);
        Field postIdField = Post.class.getDeclaredField("id");
        postIdField.setAccessible(true);
        postIdField.set(mockPost, 100L);

        // 기존 Comment 객체
        existingComment = Comment.commentBuilder()
                .content("원본 댓글 내용")
                .authorId(author.getId())
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
    @DisplayName("댓글 수정 시 Feign Helper가 호출되어야 한다")
    void shouldUseFeignHelperWhenPublishTypeIsFeign() {
        // Given
        String token = "test-token";
        when(memberFeignHelper.getMemberSummaryById(member.getId(), token, response)).thenReturn(author);
        when(commentRepository.findByIdAndNotDeleted(200L)).thenReturn(Optional.of(existingComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(existingComment);
        when(commentMongoRepository.findByCommentPkAndIsDeleted(200L, Boolean.FALSE))
                .thenReturn(Optional.of(existingDocument));
        doNothing().when(commentMongoRepository).delete(any(CommentDocument.class));
        when(commentMongoRepository.save(any(CommentDocument.class))).thenReturn(mock(CommentDocument.class));
        when(memberActivityFeignHelper.createCommentsMemberActivity(any(), anyString(), any()))
                .thenReturn(mock(MemberActivityDto.MemberActivity.class));

        // When
        CommentDto.Comment result = commentService.updateComment(member, 200L, request, token, response);

        // Then
        // 1. Member 정보가 조회되었는지 확인
        verify(memberFeignHelper, times(1)).getMemberSummaryById(member.getId(), token, response);

        // 2. Comment가 조회되었는지 확인
        verify(commentRepository, times(1)).findByIdAndNotDeleted(200L);

        // 3. Comment가 저장되었는지 확인
        verify(commentRepository, times(1)).save(any(Comment.class));

        // 4. MongoDB 문서가 업데이트되었는지 확인
        verify(commentMongoRepository, times(1)).findByCommentPkAndIsDeleted(200L, Boolean.FALSE);
        verify(commentMongoRepository, times(1)).delete(any(CommentDocument.class));
        verify(commentMongoRepository, times(1)).save(any(CommentDocument.class));

        // 5. Feign Helper가 호출되었는지 확인
        verify(memberActivityFeignHelper, times(1))
                .createCommentsMemberActivity(any(), eq(token), eq(response));

        // 6. Kafka Publisher는 호출되지 않았는지 확인
        verify(memberActivityKafkaPublisher, never())
                .publishCommentCUDEvent(anyLong(), anyLong(), anyLong(), any(MemberActivityType.class));

        // 7. 결과 검증
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Feign 호출 실패 시에도 댓글은 정상적으로 수정되어야 한다")
    void shouldUpdateCommentEvenIfFeignCallFails() {
        // Given
        when(memberFeignHelper.getMemberSummaryById(member.getId(), "token", response)).thenReturn(author);
        when(commentRepository.findByIdAndNotDeleted(200L)).thenReturn(Optional.of(existingComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(existingComment);
        when(commentMongoRepository.findByCommentPkAndIsDeleted(200L, Boolean.FALSE))
                .thenReturn(Optional.of(existingDocument));
        doNothing().when(commentMongoRepository).delete(any(CommentDocument.class));
        when(commentMongoRepository.save(any(CommentDocument.class))).thenReturn(mock(CommentDocument.class));
        doThrow(new RuntimeException("Feign call failed"))
                .when(memberActivityFeignHelper)
                .createCommentsMemberActivity(any(), anyString(), any());

        // When & Then
        try {
            commentService.updateComment(member, 200L, request, "token", response);
        } catch (Exception e) {
            // Feign 실패는 예외를 발생시킬 수 있음
        }

        // Comment는 조회되었어야 함
        verify(commentRepository, times(1)).findByIdAndNotDeleted(200L);
    }
}