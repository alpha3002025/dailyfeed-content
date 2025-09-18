package click.dailyfeed.content.config.redis;

import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.cache.CacheKeyConstant;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:26379}")
    private Integer redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisHost);
        configuration.setPort(redisPort);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    RedisTemplate<String, MemberDto.Member> memberDtoRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper
    ){
        RedisTemplate<String, MemberDto.Member> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Jackson2JsonRedisSerializer 설정
        Jackson2JsonRedisSerializer<MemberDto.Member> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(
                        redisObjectMapper,
                        MemberDto.Member.class
                );

        // Key는 String으로, Value는 JSON으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean // 1m
    public CacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("jsonRedisSerializer") GenericJackson2JsonRedisSerializer jsonRedisSerializer
    ) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1))
//                .prefixCacheNameWith("myapp:")  // 키 접두사
                .disableCachingNullValues()     // null 값 캐싱 비활성화
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonRedisSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        CacheKeyConstant.PostService.GET_ITEM_BY_ID_CACHE_LIST.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofMinutes(5))));
        CacheKeyConstant.PostService.GET_PAGE_CACHE_LIST.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofMinutes(10))));
        CacheKeyConstant.PostService.STATISTICS_CACHE_LIST.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofMinutes(15))));
        CacheKeyConstant.PostService.SEARCH_CHACHE_LIST.forEach(key -> cacheConfigurations.put(key, config.entryTtl(Duration.ofMinutes(20))));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .transactionAware()  // 트랜잭션 지원
                .build();
    }

    @Bean
    public GenericJackson2JsonRedisSerializer jsonRedisSerializer(
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper
    ) {
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }
    
}
