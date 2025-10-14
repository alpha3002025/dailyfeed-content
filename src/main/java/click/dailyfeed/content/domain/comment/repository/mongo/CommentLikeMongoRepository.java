package click.dailyfeed.content.domain.comment.repository.mongo;

import click.dailyfeed.content.domain.comment.document.CommentLikeDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentLikeMongoRepository extends MongoRepository<CommentLikeDocument, ObjectId> {
    CommentLikeDocument findByCommentPkAndMemberId(Long commentPk, Long memberId);
}
