package click.dailyfeed.content.domain.comment.document;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Document(collection = "comments")
public class CommentDocument {
    @Id
    private ObjectId id;

    @Field("post_pk")
    private Long postPk;

    @Field("comment_pk")
    private Long commentPk;

    @Field("parent_pk")
    private Long parentPk;

    private String content;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("is_deleted")
    private Boolean isDeleted;

    @PersistenceCreator
    public CommentDocument(
        ObjectId id,
        Long postPk,
        Long commentPk,
        Long parentPk,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isDeleted
    ){
        this.id = id;
        this.postPk = postPk;
        this.commentPk = commentPk;
        this.parentPk = parentPk;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    @Builder(builderMethodName = "newCommentBuilder", builderClassName = "NewPost")
    private CommentDocument(Long postPk, Long commentPk, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.postPk = postPk;
        this.commentPk = commentPk;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = Boolean.FALSE;
    }

    @Builder(builderMethodName = "updatedCommentBuilder", builderClassName = "UpdatedPost")
    private CommentDocument(CommentDocument oldDocument, LocalDateTime updatedAt) {
        this.postPk = oldDocument.getPostPk();
        this.commentPk = oldDocument.getCommentPk();
        this.content = oldDocument.getContent();
        this.createdAt = oldDocument.getCreatedAt();
        this.updatedAt =  updatedAt;
        this.isDeleted = Boolean.FALSE;
    }

    public static CommentDocument newComment(Long postPk, Long commentPk, String content, LocalDateTime createdAt, LocalDateTime updatedAt){
        return CommentDocument.newCommentBuilder()
                .postPk(postPk)
                .commentPk(commentPk)
                .content(content)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static CommentDocument newUpdatedComment(CommentDocument commentDocument, LocalDateTime updatedAt){
        return CommentDocument.updatedCommentBuilder()
                .oldDocument(commentDocument)
                .updatedAt(updatedAt)
                .build();
    }

    public static CommentDocument newReplyDocument(Long postPk, Long parentPk, Long commentPk, String content, LocalDateTime createdAt, LocalDateTime updatedAt){
        return CommentDocument.newCommentBuilder()
                .postPk(postPk)
                .commentPk(commentPk)
                .content(content)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public void softDelete(){
        this.isDeleted = true;
    }
}
