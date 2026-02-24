package com.travelplanner.payment.config;

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
 * PAY 서비스 Redis 설정.
 *
 * <p>PAY 서비스는 Redis DB7을 사용한다.</p>
 * <ul>
 *   <li>DB7 — 구독 상태 캐시 (pay:subscription:{userId})</li>
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
     * PAY 서비스용 Redis 연결 팩토리 (DB7).
     *
     * @return Redis DB7 연결 팩토리
     */
    @Bean
    @Primary
    public RedisConnectionFactory payRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setDatabase(7);
        return new LettuceConnectionFactory(config);
    }

    /**
     * PAY 서비스용 RedisTemplate (DB7).
     *
     * @return DB7 RedisTemplate (String-String)
     */
    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate() {
        return buildStringRedisTemplate(payRedisConnectionFactory());
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
