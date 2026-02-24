package com.travelplanner.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 영업시간 외 경고 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class BusinessHoursWarningResponse {

    @JsonProperty("warning")
    private String warning;

    @JsonProperty("message")
    private String message;

    @JsonProperty("business_hours")
    private String businessHours;

    public static BusinessHoursWarningResponse of(String businessHours) {
        return BusinessHoursWarningResponse.builder()
            .warning("OUTSIDE_BUSINESS_HOURS")
            .message("영업시간 외입니다 (영업시간: " + businessHours + ")")
            .businessHours(businessHours)
            .build();
    }
}
