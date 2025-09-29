package click.dailyfeed.content.domain.post.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "post_likes")
public class PostLikeDocument {

    @Id
    private ObjectId id;

    @Field("post_pk")
    private Long postPk;

    @Field("member_id")
    private Long memberId;

    @Builder(builderMethodName = "newPostLikeBuilder", builderClassName = "NewPostLike")
    public PostLikeDocument(Long postPk, Long memberId) {
        this.postPk = postPk;
        this.memberId = memberId;
    }

}
