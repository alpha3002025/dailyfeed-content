package click.dailyfeed.content.domain.post.repository;

import click.dailyfeed.content.domain.post.entity.PostLatestActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostLatestActivityRepository extends JpaRepository<PostLatestActivity, Long> {
    /**
     * 특정 회원과 게시글에 대한 최신 활동 조회
     */
    Optional<PostLatestActivity> findByMemberIdAndPostId(Long memberId, Long postId);

    /**
     * 특정 회원과 게시글에 대한 활동 타입과 수정일자 업데이트
     */
    @Modifying
    @Query("UPDATE PostLatestActivity p SET p.activityType = :activityType, p.updatedAt = CURRENT_TIMESTAMP WHERE p.memberId = :memberId AND p.postId = :postId")
    int updateActivityTypeAndLastModifiedDate(@Param("memberId") Long memberId,
                                              @Param("postId") Long postId,
                                              @Param("activityType") String activityType);

    /**
     * DELETE가 아닌 최신 활동들을 최신 날짜 순으로 조회 (페이징)
     * 만들긴 했는데 굳이 쓸모는 헛 ㅋㅋㅋ
     */
    @Query("SELECT p FROM PostLatestActivity p WHERE p.activityType != 'DELETE' ORDER BY p.updatedAt DESC")
    Page<PostLatestActivity> findActivePostsOrderByLastModifiedDateDesc(Pageable pageable);
}
