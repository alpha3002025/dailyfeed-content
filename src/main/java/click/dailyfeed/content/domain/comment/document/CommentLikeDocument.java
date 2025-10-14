package click.dailyfeed.content.domain.comment.document;

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
@Document(collection = "comment_likes")
public class CommentLikeDocument {

    @Id
    private ObjectId id;

    @Field("comment_pk")
    private Long commentPk;

    @Field("member_id")
    private Long memberId;

    @Builder(builderMethodName = "newCommentLikeBuilder", builderClassName = "NewCommentLike")
    public CommentLikeDocument(Long commentPk, Long memberId) {
        this.commentPk = commentPk;
        this.memberId = memberId;
    }
}
