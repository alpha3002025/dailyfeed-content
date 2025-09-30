package click.dailyfeed.content.domain.post.repository.jpa;

import click.dailyfeed.content.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p LEFT JOIN FETCH Comment c WHERE p.id IN :postIds AND p.isDeleted = false ORDER BY p.updatedAt DESC")
    Page<Post> findPostsWithCommentsByPostIds(@Param("postIds") Set<Long> postIds, Pageable pageable);

    // ID로 삭제되지 않은 게시글 조회
    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.isDeleted = false")
    Optional<Post> findByIdAndNotDeleted(@Param("id") Long id);

    // 삭제되지 않은 게시글만 조회
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findAllNotDeletedOrderByCreatedDateDesc(Pageable pageable);

    // 제목으로 검색
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findByTitleContainingAndNotDeleted(@Param("keyword") String keyword, Pageable pageable);

    // 내용으로 검색
    @Query("SELECT p FROM Post p WHERE p.content LIKE %:keyword% AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findByContentContainingAndNotDeleted(@Param("keyword") String keyword, Pageable pageable);

    // 제목 또는 내용으로 검색 ==
    @Query("SELECT p FROM Post p WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findByTitleOrContentContainingAndNotDeleted(@Param("keyword") String keyword, Pageable pageable);

    // 최근 게시글 조회
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findRecentPosts(Pageable pageable);

    // 인기 게시글 (좋아요 수 기준)
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false ORDER BY (p.viewCount + p.likeCount * 2) DESC, p.createdAt DESC")
    Page<Post> findPopularPostsNotDeleted(Pageable pageable);

    // 조회수 많은 게시글 조회
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false ORDER BY p.viewCount DESC, p.createdAt DESC")
    Page<Post> findMostViewedPostsNotDeleted(Pageable pageable);

    // 특정 작성자들의 게시글 조회 (팔로우한 사용자들의 게시글)
    @Query("SELECT p FROM Post p WHERE p.authorId IN :authorIds AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findByAuthorIdsAndNotDeleted(@Param("authorIds") List<Long> authorIds, Pageable pageable);

    // 특정 작성자의 게시글 수 조회
    @Query("SELECT COUNT(p) FROM Post p WHERE p.authorId = :authorId AND p.isDeleted = false")
    long countByAuthorIdAndNotDeleted(@Param("authorId") Long authorId);

    // 전체 게시글 수 조회 (삭제되지 않은 것만)
    @Query("SELECT COUNT(p) FROM Post p WHERE p.isDeleted = false")
    long countAllNotDeleted();

    // 특정 기간 내 게시글 조회
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findByCreatedDateBetweenAndNotDeleted(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);

    // 댓글이 많은 게시글 조회 (댓글 수로 정렬)
    @Query("SELECT p " +
            "FROM Post p LEFT JOIN p.comments c " +
            "WHERE p.isDeleted = false AND (c.isDeleted = false OR c.id IS NULL) " +
            "GROUP BY p " +
            "ORDER BY COUNT(c) DESC, p.createdAt DESC")
    Page<Post> findMostCommentedPosts(Pageable pageable);

    // 최근 활동이 있는 게시글 (최근 댓글 기준)
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN p.comments c " +
            "WHERE p.isDeleted = false AND (c.isDeleted = false OR c.id IS NULL) " +
            "ORDER BY COALESCE(MAX(c.createdAt), p.createdAt) DESC")
    Page<Post> findPostsByRecentActivity(Pageable pageable);

    // 게시글 소프트 삭제
    @Modifying
    @Query("UPDATE Post p SET p.isDeleted = true WHERE p.id = :id")
    void softDeleteById(@Param("id") Long id);

    // 작성자별 게시글 소프트 삭제 (관리자용)
    @Modifying
    @Query("UPDATE Post p SET p.isDeleted = true WHERE p.authorId = :authorId")
    int softDeleteByAuthorId(@Param("authorId") Long authorId);

//    // click.dailyfeed.code.domain.content.post.dto
//    @Query("SELECT new click.dailyfeed.code.domain.content.post.dto.PostDto.PostStatistics(" +
//            "COUNT(p), " +
//            "SUM(p.viewCount), " +
//            "SUM(p.likeCount), " +
//            "SUM(SIZE(p.comments)), " +
//            "AVG(p.viewCount), " +
//            "AVG(p.likeCount), " +
//            "AVG(SIZE(p.comments))) " +
//            "FROM Post p WHERE p.isDeleted = false")
//    PostDto.PostStatistics getPostStatistics();
//
//    // 작성자별 통계 조회
//    @Query("SELECT new click.dailyfeed.code.domain.content.post.dto.PostDto.PostStatistics(" +
//            "COUNT(p), " +
//            "SUM(p.viewCount), " +
//            "SUM(p.likeCount), " +
//            "SUM(SIZE(p.comments)), " +
//            "AVG(p.viewCount), " +
//            "AVG(p.likeCount), " +
//            "AVG(SIZE(p.comments))) " +
//            "FROM Post p WHERE p.authorId = :authorId AND p.isDeleted = false")
//    PostDto.PostStatistics getPostStatisticsByAuthor(@Param("authorId") Long authorId);
//
//    // 월별 게시글 수 조회
//    @Query("SELECT FUNCTION('YEAR', p.createdAt) as year, " +
//            "FUNCTION('MONTH', p.createdAt) as month, " +
//            "COUNT(p) as postCount " +
//            "FROM Post p WHERE p.isDeleted = false " +
//            "GROUP BY FUNCTION('YEAR', p.createdAt), FUNCTION('MONTH', p.createdAt) " +
//            "ORDER BY FUNCTION('YEAR', p.createdAt) DESC, FUNCTION('MONTH', p.createdAt) DESC")
//    List<Object[]> getMonthlyPostCounts();
}
