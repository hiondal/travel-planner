package com.travelplanner.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 멀티 DB 설정.
 *
 * <p>AUTH 서비스는 두 개의 Redis DB를 사용한다.</p>
 * <ul>
 *   <li>DB0 — JWT 블랙리스트 (공통 영역)</li>
 *   <li>DB1 — Refresh Token 세션 (AUTH 전용)</li>
 * </ul>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * JWT 블랙리스트용 Redis 연결 팩토리 (DB0).
     *
     * @return Redis DB0 연결 팩토리
     */
    @Bean
    @Primary
    public RedisConnectionFactory blacklistRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setDatabase(0);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Refresh Token 세션용 Redis 연결 팩토리 (DB1).
     *
     * @return Redis DB1 연결 팩토리
     */
    @Bean
    public RedisConnectionFactory authRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setDatabase(1);
        return new LettuceConnectionFactory(config);
    }

    /**
     * JWT 블랙리스트용 RedisTemplate (DB0).
     *
     * @return DB0 RedisTemplate (String-String)
     */
    @Bean
    @Primary
    public RedisTemplate<String, String> blacklistRedisTemplate() {
        return buildStringRedisTemplate(blacklistRedisConnectionFactory());
    }

    /**
     * Refresh Token 세션용 RedisTemplate (DB1).
     *
     * @return DB1 RedisTemplate (String-String)
     */
    @Bean
    public RedisTemplate<String, String> authRedisTemplate() {
        return buildStringRedisTemplate(authRedisConnectionFactory());
    }

    /**
     * String 직렬화를 사용하는 RedisTemplate을 생성한다.
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return 설정된 RedisTemplate
     */
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
