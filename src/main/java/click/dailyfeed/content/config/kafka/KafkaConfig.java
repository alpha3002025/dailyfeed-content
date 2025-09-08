package click.dailyfeed.content.config.kafka;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${infrastructure.kafka.topic.post-activity.prefix}")
    private String postActivityPrefix;

    @Value("${infrastructure.kafka.topic.post-activity.prefix-date-format}")
    private String dateFormat;

    @Value("${infrastructure.kafka.topic.post-activity.retention-ms:604800000}")
    private String retentionMs;

    @Value("${KAFKA_USER:}")
    private String kafkaUser;

    @Value("${KAFKA_PASSWORD:}")
    private String kafkaPassword;

    @Value("${KAFKA_SASL_PROTOCOL:PLAINTEXT}")
    private String saslProtocol;

    @Value("${KAFKA_SASL_MECHANISM:PLAIN}")
    private String saslMechanism;

    // 공통 Consumer 설정 메서드
    private Map<String, Object> getCommonConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "click.dailyfeed.code.domain.content,click.dailyfeed.code.domain.member");

        // SASL 설정 (local 프로필에서)
        if (!kafkaUser.isEmpty() && !kafkaPassword.isEmpty()) {
            props.put("security.protocol", saslProtocol);
            props.put("sasl.mechanism", saslMechanism);
            props.put("sasl.jaas.config",
                    "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                            "username=\"" + kafkaUser + "\" " +
                            "password=\"" + kafkaPassword + "\";");
        }

        return props;
    }

    // Post Activity Consumer 설정
    @Bean(name = "postActivityConsumerFactory")
    public ConsumerFactory<String, PostDto.PostActivityEvent> postActivityConsumerFactory() {
        Map<String, Object> props = getCommonConsumerProps();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "post-activity-consumer-group");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PostDto.PostActivityEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PostDto.PostActivityEvent> postActivityKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PostDto.PostActivityEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(postActivityConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(3); // 동시 처리 스레드 수
        return factory;
    }

    // User Activity Consumer 설정 (그냥 예제 (컨슈머가 여러개 생길 경우를 대비에 템플릿을 하나 만들어둠))
    @Bean
    public ConsumerFactory<String, MemberDto.MemberActivity> userActivityConsumerFactory() {
        Map<String, Object> props = getCommonConsumerProps();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "user-activity-consumer-group");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MemberDto.MemberActivity.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    // Admin Configuration - local 프로필에서는 비활성화
    // @Bean
    // public KafkaAdmin kafkaAdmin() {
    //     Map<String, Object> configs = new HashMap<>();
    //     configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    //     return new KafkaAdmin(configs);
    // }

    // Topic 생성 (현재 날짜 기준) - local 프로필에서는 비활성화
    // @Bean
    // public NewTopic todayPostActivityTopic() {
    //     String today = LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat));
    //     String topicName = postActivityPrefix + today;

    //     // config
    //     Map<String, String> props = new HashMap<>();
    //     props.put(TopicConfig.RETENTION_MS_CONFIG, retentionMs);

    //     return new NewTopic(topicName, 3, (short) 1).configs(props);
    // }

    // Topic 생성 (어제 날짜 기준 - 테스트용) - local 프로필에서는 비활성화
    // @Bean
    // public NewTopic yesterdayPostActivityTopic() {
    //     String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern(dateFormat));
    //     String topicName = postActivityPrefix + yesterday;

    //     // config
    //     Map<String, String> props = new HashMap<>();
    //     props.put(TopicConfig.RETENTION_MS_CONFIG, retentionMs);

    //     return new NewTopic(topicName, 3, (short) 1).configs(props);
    // }
}
