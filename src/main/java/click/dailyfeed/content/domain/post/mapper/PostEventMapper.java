package click.dailyfeed.content.domain.post.mapper;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.content.post.type.PostActivityType;
import click.dailyfeed.code.domain.content.post.type.PostLikeType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PostEventMapper {
    public LocalDateTime currentDateTime() {
        return LocalDateTime.now();
    }

    public PostDto.PostActivityEvent newPostActivityEvent(Long memberId, Long postId, PostActivityType activityType, LocalDateTime createdAt) {
        return PostDto.PostActivityEvent.builder()
                .memberId(memberId)
                .followingId(null) // 팔로우 관련 정보는 별도 처리 필요시 추가
                .postId(postId)
                .postActivityType(activityType)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    public PostDto.LikeActivityEvent newLikeActivityEvent(Long memberId, Long postId, PostLikeType postLikeType, LocalDateTime createdAt) {
        return PostDto.LikeActivityEvent.builder()
                .memberId(memberId)
                .postId(postId)
                .postLikeType(postLikeType)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
