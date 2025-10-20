package click.dailyfeed.content.domain.deadletter.mongo;

import click.dailyfeed.content.domain.deadletter.document.FeignDeadLetterDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FeignDeadLetterRepository extends MongoRepository<FeignDeadLetterDocument, ObjectId> {
    List<FeignDeadLetterDocument> findByMessageKey(String messageKey);
}
