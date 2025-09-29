package click.dailyfeed.content.domain.comment.projection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostCommentCountProjection {
    private Long postPk;
    private Integer commentCount;
}
