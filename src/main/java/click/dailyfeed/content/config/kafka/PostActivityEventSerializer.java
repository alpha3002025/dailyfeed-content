package click.dailyfeed.content.config.kafka;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class PostActivityEventSerializer implements Serializer<PostDto.PostActivityEvent> {
    private final ObjectMapper objectMapper;

    public PostActivityEventSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // 필요시 설정
    }

    @Override
    public byte[] serialize(String topic, PostDto.PostActivityEvent data) {
        if (data == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing PostActivityEvent", e);
        }
    }

    @Override
    public void close() {
        // 리소스 정리 필요시
    }
}
