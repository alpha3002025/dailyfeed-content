package click.dailyfeed.content.domain.redisdlq.repository.mongo;

import click.dailyfeed.content.domain.redisdlq.document.RedisDLQDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RedisDLQRepository extends MongoRepository<RedisDLQDocument, ObjectId> {
    List<RedisDLQDocument> findByMessageKey(String messageKey);
}
