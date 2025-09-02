package click.dailyfeed.content.domain.post.repository;

import click.dailyfeed.content.domain.post.entity.PostActivityHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostActivityHistoryRepository extends JpaRepository<PostActivityHistory, Long> {
}
