package com.travelplanner.place;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PLACE 서비스 애플리케이션 진입점.
 *
 * <p>Google Places API 연동을 통한 장소 검색, 상세 조회, 주변 장소 검색 기능을 제공한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"com.travelplanner.place", "com.travelplanner.common"})
public class PlaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlaceApplication.class, args);
    }
}
