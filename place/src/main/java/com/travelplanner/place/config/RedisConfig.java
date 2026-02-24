package com.travelplanner.place.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정.
 *
 * <p>PLACE 서비스는 Redis DB3를 사용한다.</p>
 * <ul>
 *   <li>DB3 — 장소 상세 캐시, 검색 결과 캐시, 주변 장소 캐시</li>
 * </ul>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Configuration
public class RedisConfig {

    private static final int PLACE_REDIS_DB = 3;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * PLACE 서비스 전용 Redis 연결 팩토리 (DB3).
     *
     * @return Redis DB3 연결 팩토리
     */
    @Bean
    public RedisConnectionFactory placeRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setDatabase(PLACE_REDIS_DB);
        return new LettuceConnectionFactory(config);
    }

    /**
     * PLACE 서비스 전용 RedisTemplate (DB3).
     *
     * @return DB3 RedisTemplate (String-String)
     */
    @Bean
    public RedisTemplate<String, String> placeRedisTemplate() {
        return buildStringRedisTemplate(placeRedisConnectionFactory());
    }

    private RedisTemplate<String, String> buildStringRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
