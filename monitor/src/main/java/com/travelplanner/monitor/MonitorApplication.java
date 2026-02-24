package com.travelplanner.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Monitor 서비스 애플리케이션.
 *
 * <p>실시간 장소 상태 모니터링(영업, 날씨, 혼잡도)을 담당한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class MonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }
}
