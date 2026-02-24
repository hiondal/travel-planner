package com.travelplanner.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 애플리케이션 공통 빈 설정.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Configuration
public class AppConfig {

    /**
     * Google OAuth API 호출용 RestTemplate 빈.
     *
     * @return RestTemplate 인스턴스
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
