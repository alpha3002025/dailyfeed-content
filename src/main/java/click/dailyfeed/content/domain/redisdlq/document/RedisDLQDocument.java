package click.dailyfeed.content.domain.redisdlq.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "redis_dead_letters")
public class RedisDLQDocument {
    @Id
    private ObjectId id;
    @Field("redis_key")
    private String redisKey;
    private String payload; // jackson serialize

    @Builder(builderMethodName = "newRedisDLQBuilder", builderClassName = "NewRedisDLQ")
    private RedisDLQDocument(String redisKey, String payload) {
        this.redisKey = redisKey;
        this.payload = payload;
    }

    public static RedisDLQDocument newRedisDLQ(String redisKey, String payload) {
        return RedisDLQDocument.newRedisDLQBuilder()
                .redisKey(redisKey)
                .payload(payload)
                .build();
    }
}
