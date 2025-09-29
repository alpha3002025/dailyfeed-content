package click.dailyfeed.content.domain.comment.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentLikeCountProjection {
    private Long commentPk;
    private Integer likeCount;
}