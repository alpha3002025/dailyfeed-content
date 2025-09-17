package click.dailyfeed.content.domain.comment.mapper;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.content.comment.type.CommentActivityType;
import click.dailyfeed.code.domain.content.comment.type.CommentLikeType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CommentEventMapper {
    public LocalDateTime currentDateTime() {
        return LocalDateTime.now();
    }

    public CommentDto.CommentActivityEvent newCommentActivityEvent(Long memberId, Long commentId, CommentActivityType activityType, LocalDateTime createdAt) {
        return CommentDto.CommentActivityEvent.builder()
                .memberId(memberId)
                .commentId(commentId)
                .commentActivityType(activityType)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    public CommentDto.LikeActivityEvent newLikeActivityEvent(Long memberId, Long commentId, CommentLikeType commentLikeType, LocalDateTime createdAt) {
        return CommentDto.LikeActivityEvent.builder()
                .memberId(memberId)
                .commentId(commentId)
                .commentLikeType(commentLikeType)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

}
