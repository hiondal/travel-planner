package com.travelplanner.briefing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.briefing.domain.BriefingContent;
import lombok.Getter;

/**
 * 브리핑 본문 DTO.
 */
@Getter
public class BriefingContentDto {

    @JsonProperty("business_status")
    private final String businessStatus;

    @JsonProperty("congestion")
    private final String congestion;

    @JsonProperty("weather")
    private final String weather;

    @JsonProperty("travel_time")
    private final TravelTimeDto travelTime;

    @JsonProperty("summary")
    private final String summary;

    public BriefingContentDto(BriefingContent content, String summary) {
        this.businessStatus = content.getBusinessStatus();
        this.congestion = content.getCongestion();
        this.weather = content.getWeather();
        this.travelTime = new TravelTimeDto(content.getWalkingMinutes(),
                content.getTransitMinutes(), content.getDistanceM());
        this.summary = summary;
    }

    @Getter
    public static class TravelTimeDto {

        @JsonProperty("walking_minutes")
        private final Integer walkingMinutes;

        @JsonProperty("transit_minutes")
        private final Integer transitMinutes;

        @JsonProperty("distance_m")
        private final Integer distanceM;

        public TravelTimeDto(Integer walkingMinutes, Integer transitMinutes, Integer distanceM) {
            this.walkingMinutes = walkingMinutes;
            this.transitMinutes = transitMinutes;
            this.distanceM = distanceM;
        }
    }
}
