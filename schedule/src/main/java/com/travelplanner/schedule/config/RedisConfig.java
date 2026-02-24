package com.travelplanner.schedule.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Schedule 서비스 Redis 설정.
 *
 * <p>SCHEDULE 서비스는 Redis DB2를 사용한다.</p>
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
     * Schedule 서비스용 Redis 연결 팩토리 (DB2).
     *
     * @return Redis DB2 연결 팩토리
     */
    @Bean
    public RedisConnectionFactory scheduleRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setDatabase(2);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Schedule 서비스용 RedisTemplate (DB2).
     *
     * @return DB2 RedisTemplate (String-String)
     */
    @Bean
    public RedisTemplate<String, String> scheduleRedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(scheduleRedisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
