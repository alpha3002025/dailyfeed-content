package click.dailyfeed.content.domain.post.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostLikeCountProjection {
    private Long postPk;
    private Integer likeCount;
}