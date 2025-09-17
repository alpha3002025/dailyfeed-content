package click.dailyfeed.content.domain.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaHelper {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public <T> void send(String topicName, String key, T payload) {
        kafkaTemplate.send(topicName, key, payload)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to send post activity event to topic: {}, key: {}",
                                topicName, key, throwable);
                    } else {
                        log.info("Successfully sent post activity event to topic: {}, postId: {}",
                                topicName, key);
                    }
                });
    }

}
