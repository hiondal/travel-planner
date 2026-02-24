package com.travelplanner.monitor.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 상태 상세 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class StatusDetailsDto {

    @JsonProperty("business_status")
    private BusinessStatusItemDto businessStatus;

    @JsonProperty("congestion")
    private CongestionItemDto congestion;

    @JsonProperty("weather")
    private WeatherItemDto weather;

    @JsonProperty("travel_time")
    private TravelTimeItemDto travelTime;

    @Getter
    @Builder
    public static class BusinessStatusItemDto {
        @JsonProperty("status")
        private String status;

        @JsonProperty("value")
        private String value;
    }

    @Getter
    @Builder
    public static class CongestionItemDto {
        @JsonProperty("status")
        private String status;

        @JsonProperty("value")
        private String value;

        @JsonProperty("is_unknown")
        private boolean isUnknown;
    }

    @Getter
    @Builder
    public static class WeatherItemDto {
        @JsonProperty("status")
        private String status;

        @JsonProperty("value")
        private String value;

        @JsonProperty("precipitation_prob")
        private int precipitationProb;
    }

    @Getter
    @Builder
    public static class TravelTimeItemDto {
        @JsonProperty("status")
        private String status;

        @JsonProperty("walking_minutes")
        private int walkingMinutes;

        @JsonProperty("transit_minutes")
        private Integer transitMinutes;

        @JsonProperty("distance_m")
        private int distanceM;
    }
}
