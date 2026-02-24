package com.travelplanner.place.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.place.domain.BusinessHour;

/**
 * 영업시간 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class BusinessHourDto {

    private final String day;
    private final String open;
    private final String close;

    private BusinessHourDto(String day, String open, String close) {
        this.day = day;
        this.open = open;
        this.close = close;
    }

    /**
     * BusinessHour 엔티티로부터 DTO를 생성한다.
     *
     * @param businessHour 영업시간 엔티티
     * @return BusinessHourDto
     */
    public static BusinessHourDto from(BusinessHour businessHour) {
        return new BusinessHourDto(
                businessHour.getDay(),
                businessHour.getOpen(),
                businessHour.getClose()
        );
    }

    public String getDay() {
        return day;
    }

    public String getOpen() {
        return open;
    }

    public String getClose() {
        return close;
    }
}
