package click.dailyfeed.content.domain.post.repository.mongo;

import click.dailyfeed.content.domain.post.document.PostDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PostMongoRepository extends MongoRepository<PostDocument, ObjectId> {
    Optional<PostDocument> findByPostPkAndIsDeleted(Long postPk, Boolean isDeleted);
}
