package click.dailyfeed.content.domain.post.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "posts")
public class PostDocument {
    @Id
    private ObjectId id;

    @Field("post_pk")
    private Long postPk;

    private String title;

    private String content;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("is_deleted")
    private Boolean isDeleted;

    @Field("comment_count")
    private Integer commentCount;

    @Field("is_current")
    private Boolean isCurrent;

    @Field("version")
    private Integer version;

    @Builder(builderMethodName = "newPostBuilder", builderClassName = "NewPost")
    private PostDocument(Long postPk, String title, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.postPk = postPk;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = false;
        this.commentCount = 0;
        this.isCurrent = true;
        this.version = 1;
    }

    @Builder(builderMethodName = "updatedPostBuilder", builderClassName = "UpdatedPost")
    private PostDocument(PostDocument oldDocument, LocalDateTime updatedAt) {
        this.postPk = oldDocument.getPostPk();
        this.title = oldDocument.getTitle();
        this.content = oldDocument.getContent();
        this.createdAt = oldDocument.getCreatedAt();
        this.updatedAt =  updatedAt;
        this.isDeleted = Boolean.FALSE;
        this.isCurrent = Boolean.TRUE;
        this.commentCount = oldDocument.getCommentCount();
        this.version = oldDocument.getVersion() + 1;
    }

    public static PostDocument newPost(Long postPk, String title, String content, LocalDateTime createdAt, LocalDateTime updatedAt){
        return PostDocument.newPostBuilder()
                .postPk(postPk)
                .title(title)
                .content(content)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    // 글 수정시 기존 post 도큐먼트는 isCurrent = false 처리, 기존 도큐먼트를 복사한 새로운 도큐먼트를 추가 후 isCurrent = true 로 지정
    public static PostDocument newUpdatedPost(PostDocument postDocument, LocalDateTime updatedAt){
        return PostDocument.updatedPostBuilder()
                .oldDocument(postDocument)
                .updatedAt(updatedAt)
                .build();
    }

    public void softDelete(){
        this.isDeleted = true;
    }
}
