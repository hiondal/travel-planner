package com.travelplanner.briefing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BRIEFING 서비스 메인 애플리케이션.
 *
 * <p>출발 전 브리핑 생성, 조회, FCM Push 발송을 담당한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class BriefingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BriefingApplication.class, args);
    }
}
