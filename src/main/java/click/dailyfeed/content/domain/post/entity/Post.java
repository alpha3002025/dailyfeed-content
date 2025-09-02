package click.dailyfeed.content.domain.post.entity;

import click.dailyfeed.content.domain.base.BaseTimeEntity;
import click.dailyfeed.content.domain.comment.entity.Comment;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Table(name = "posts")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "ofAll")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Post extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_id", nullable = false)
    private Long authorId; // DB 연결 x

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "like_count")
    private Long likeCount = 0L;

    @Column(name = "is_deleted", columnDefinition = "TINYINT(1)")
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Comment> comments = new ArrayList<>();

    @Builder
    public Post(String title, String content, Long authorId) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
    }

    public static Post newPost(String title, String content, Long authorId) {
        return Post.builder()
                .title(title)
                .content(content)
                .authorId(authorId)
                .build();
    }

    // 비즈니스 메서드
    public void updatePost(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void incrementViewCount(){
        this.viewCount++;
    }

    public void decrementViewCount(){
        if(this.viewCount > 0){
            this.viewCount--;
        }
    }

    public void incrementLikeCount(){
        this.likeCount++;
    }

    public void decrementLikeCount(){
        if(this.likeCount > 0){
            this.likeCount--;
        }
        else{
            this.likeCount = 0L;
        }
    }

    public void softDelete(){
        this.isDeleted = true;
    }

    public boolean isAuthor(Long userId){
        return this.authorId.equals(userId);
    }

    public int getCommentsCount(){
        return this.comments.size();
    }
}
