package com.travelplanner.monitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Monitor 서비스 Redis 설정.
 *
 * <p>MONITOR 서비스는 Redis DB4를 사용한다.</p>
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
     * Monitor 서비스용 Redis 연결 팩토리 (DB4).
     *
     * @return Redis DB4 연결 팩토리
     */
    @Bean
    public RedisConnectionFactory mntrRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setDatabase(4);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Monitor 서비스용 RedisTemplate (DB4).
     *
     * @return DB4 RedisTemplate (String-String)
     */
    @Bean
    public RedisTemplate<String, String> mntrRedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(mntrRedisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
