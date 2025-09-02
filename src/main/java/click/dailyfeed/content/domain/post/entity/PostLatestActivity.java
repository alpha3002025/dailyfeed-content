package click.dailyfeed.content.domain.post.entity;

import click.dailyfeed.code.domain.content.post.type.PostActivityType;
import click.dailyfeed.content.domain.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO (삭제) timeline+contents 서비스로 이관
@Getter
@Entity
@Table(
        name = "post_latest_activity",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "post_latest_activity_uk",
                        columnNames = {"post_id", "member_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLatestActivity extends BaseTimeEntity {

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

    @Builder(builderMethodName = "insertBuilder")
    public PostLatestActivity(Long memberId, Long postId, PostActivityType activityType) {
        this.memberId = memberId;
        this.postId = postId;
        this.activityType = activityType;
    }

    public static PostLatestActivity newPostLatestActivity(Long memberId, Long postId, PostActivityType activityType) {
        return PostLatestActivity.insertBuilder()
                .memberId(memberId)
                .postId(postId)
                .activityType(activityType)
                .build();
    }
}
