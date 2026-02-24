package com.travelplanner.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AUTH 서비스 Spring Boot 애플리케이션 진입점.
 *
 * <p>Google OAuth2 소셜 로그인 및 JWT 토큰 관리 서비스를 제공한다.</p>
 *
 * <p>scanBasePackages에 {@code com.travelplanner.common}을 포함하여
 * common 모듈의 JwtProvider, 예외 핸들러 등을 자동 빈으로 등록한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"com.travelplanner.auth", "com.travelplanner.common"})
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
