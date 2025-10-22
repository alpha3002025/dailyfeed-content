package click.dailyfeed.content.domain.deadletter.repository.mongo;

import click.dailyfeed.content.domain.deadletter.document.FeignDeadLetterDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FeignDeadLetterRepository extends MongoRepository<FeignDeadLetterDocument, ObjectId> {
}
