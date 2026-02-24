package com.travelplanner.alternative.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.alternative.domain.Alternative;
import com.travelplanner.alternative.dto.request.CoordinatesDto;
import lombok.Getter;

/**
 * 대안 카드 응답 DTO.
 */
@Getter
public class AlternativeCardDto {

    @JsonProperty("alt_id")
    private final String altId;

    @JsonProperty("place_id")
    private final String placeId;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("distance_m")
    private final int distanceM;

    @JsonProperty("rating")
    private final Float rating;

    @JsonProperty("congestion")
    private final String congestion;

    @JsonProperty("reason")
    private final String reason;

    @JsonProperty("status_label")
    private final String statusLabel;

    @JsonProperty("coordinates")
    private final CoordinatesResponse coordinates;

    @JsonProperty("travel_time")
    private final TravelTimeDto travelTime;

    private AlternativeCardDto(Alternative alternative) {
        this.altId = alternative.getId();
        this.placeId = alternative.getPlaceId();
        this.name = alternative.getName();
        this.distanceM = alternative.getDistanceM();
        this.rating = alternative.getRating();
        this.congestion = alternative.getCongestion();
        this.reason = alternative.getReason();
        this.statusLabel = alternative.getStatusLabel();
        this.coordinates = new CoordinatesResponse(alternative.getLat(), alternative.getLng());
        this.travelTime = new TravelTimeDto(alternative.getWalkingMinutes(), alternative.getTransitMinutes());
    }

    public static AlternativeCardDto from(Alternative alternative) {
        return new AlternativeCardDto(alternative);
    }

    @Getter
    public static class CoordinatesResponse {
        @JsonProperty("lat")
        private final double lat;
        @JsonProperty("lng")
        private final double lng;

        public CoordinatesResponse(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }
}
