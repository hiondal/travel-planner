package com.travelplanner.place.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 애플리케이션 공통 빈 설정.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Configuration
public class AppConfig {

    /**
     * Jackson ObjectMapper 빈 설정.
     *
     * <p>Java 8 날짜/시간 타입 직렬화를 지원한다.</p>
     *
     * @return ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * WebClient.Builder 빈 설정.
     *
     * @return WebClient.Builder
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
