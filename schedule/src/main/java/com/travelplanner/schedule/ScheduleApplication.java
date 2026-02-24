package com.travelplanner.schedule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Schedule 서비스 애플리케이션.
 *
 * <p>여행 일정 CRUD 및 장소 추가/교체/삭제를 담당한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"com.travelplanner.schedule", "com.travelplanner.common"})
public class ScheduleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScheduleApplication.class, args);
    }
}
