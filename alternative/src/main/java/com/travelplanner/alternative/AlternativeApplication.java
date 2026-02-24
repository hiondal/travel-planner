package com.travelplanner.alternative;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ALTERNATIVE 서비스 메인 애플리케이션.
 *
 * <p>대안 장소 검색 및 카드 선택 서비스.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"com.travelplanner.alternative", "com.travelplanner.common"})
public class AlternativeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlternativeApplication.class, args);
    }
}
