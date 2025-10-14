package click.dailyfeed.content.domain.comment.entity;

import click.dailyfeed.content.domain.base.BaseTimeEntity;
import click.dailyfeed.content.domain.post.entity.Post;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Table(name = "comments")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "ofAll")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Comment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 부모 댓글 (null이면 최상위 댓글, 값이 있으면 대댓글)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 자식 댓글들 (대댓글들)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Comment> children = new ArrayList<>();

    @Column(name = "is_deleted", columnDefinition = "TINYINT(1)")
    private Boolean isDeleted = false;

    // 댓글 깊이 (0: 최상위 댓글, 1: 대댓글, 2: 대댓글의 댓글...)
    @Column(name = "depth")
    private Integer depth = 0;

    @Column(name = "like_count")
    private Long likeCount = 0L;

    @Builder(builderMethodName = "commentBuilder")
    public Comment(String content, Post post, Long authorId){
        this.content = content;
        this.post = post;
        this.authorId = authorId;
    }

    @Builder(builderMethodName = "replyCommentBuilder")
    public Comment(String content, Post post, Comment parent, Long authorId){
        this.content = content;
        this.post = post;
        this.authorId = authorId;
//        this.parent = parent;
//        this.depth = parent != null ? parent.getDepth() + 1 : 0;
    }

    // 비즈니스 메서드
    public boolean isTopLevel() {
        return parent == null;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public boolean isOwnedBy(Long userId) {
        return this.authorId.equals(userId);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public void addChild(Comment child) {
        children.add(child);
        child.updateParent(this);
        child.depth = this.depth + 1;  // 자식의 depth = 부모의 depth + 1
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public void updateParent(Comment parent) {
        this.parent = parent;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public int getChildrenCount() {
        return children.size();
    }

    // Private setter for parent (JPA 용)
    private void setParent(Comment parent) {
        this.parent = parent;
    }
}
