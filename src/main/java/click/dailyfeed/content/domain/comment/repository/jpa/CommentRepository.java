package click.dailyfeed.content.domain.comment.repository.jpa;

import click.dailyfeed.content.domain.comment.entity.Comment;
import click.dailyfeed.content.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 특정 게시글의 모든 댓글 수 (삭제된 것 제외)
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post = :post AND c.isDeleted = false")
    int countByPostAndNotDeleted(@Param("post") Post post);

    // 특정 사용자의 댓글들
    @Query("SELECT c FROM Comment c WHERE c.authorId = :authorId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findByAuthorIdAndNotDeleted(@Param("authorId") Long authorId, Pageable pageable);

    // ID로 댓글 조회 (삭제되지 않은)
    @Query("SELECT c FROM Comment c INNER JOIN FETCH c.post WHERE c.id = :id AND c.isDeleted = false")
    Optional<Comment> findByIdAndNotDeleted(@Param("id") Long id);

    // 특정 댓글과 모든 자식 댓글들을 소프트 삭제
    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = true, c.createdAt = CURRENT_TIMESTAMP WHERE c.id = :commentId OR c.parent.id = :commentId")
    void softDeleteCommentAndChildren(@Param("commentId") Long commentId);

    interface PostCommentCountProjection {
        Long getPostId();
        Long getCommentCount();
    }

    // 글 하나에 대한 댓글 수 조회
    @Query("SELECT p.id as postId, COUNT(c.id) as commentCount " +
            "FROM Post p LEFT JOIN Comment c ON p.id = c.post.id AND c.isDeleted = false " +
            "WHERE p IN :posts AND p.isDeleted = false " +
            "GROUP BY p.id")
    List<PostCommentCountProjection> findCommentCountsByPosts(@Param("posts") List<Post> posts);
}
