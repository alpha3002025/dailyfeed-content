package click.dailyfeed.content.domain.comment.repository.mongo;

import click.dailyfeed.content.domain.comment.document.CommentDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CommentMongoRepository extends MongoRepository<CommentDocument, ObjectId> {
    Optional<CommentDocument> findByCommentPkAndIsDeleted(Long commentPk, Boolean isDeleted);
}
