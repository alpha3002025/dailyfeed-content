package click.dailyfeed.content.domain.comment.repository.mongo;

import click.dailyfeed.content.domain.comment.document.CommentDocument;
import click.dailyfeed.content.domain.comment.projection.PostCommentCountProjection;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CommentMongoRepository extends MongoRepository<CommentDocument, ObjectId> {
    Optional<CommentDocument> findByCommentPkAndIsDeleted(Long commentPk, Boolean isDeleted);

    // 여러 포스트에 대한 댓글 수를 한 번에 조회
    @Aggregation(pipeline = {
            "{ '$match': { 'post_pk': { '$in': ?0 } } }",
            "{ '$group': { '_id': '$post_pk', 'count': { '$sum': 1 } } }",
            "{ '$project': { 'postPk': '$_id', 'commentCount': '$count', '_id': 0 } }"
    })
    List<PostCommentCountProjection> countCommentsByPostPks(Set<Long> postPks);
}
