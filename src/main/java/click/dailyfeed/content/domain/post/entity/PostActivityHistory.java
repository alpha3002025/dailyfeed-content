package click.dailyfeed.content.domain.post.entity;

import click.dailyfeed.code.domain.content.post.type.PostActivityType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// TODO (삭제) timeline+contents 서비스로 이관
@Entity
@Table(name = "post_activity_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PostActivityHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 40)
    private PostActivityType activityType;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder(builderMethodName = "insertBuilder")
    public PostActivityHistory(Long memberId, Long postId, PostActivityType activityType) {
        this.memberId = memberId;
        this.postId = postId;
        this.activityType = activityType;
    }

    public static PostActivityHistory newPostActivityHistory(Long memberId, Long postId, PostActivityType activityType) {
        return PostActivityHistory.insertBuilder()
                .memberId(memberId)
                .postId(postId)
                .activityType(activityType)
                .build();
    }
}
