package com.travelplanner.alternative.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 이동 시간 DTO.
 */
@Getter
public class TravelTimeDto {

    @JsonProperty("walking_minutes")
    private final Integer walkingMinutes;

    @JsonProperty("transit_minutes")
    private final Integer transitMinutes;

    public TravelTimeDto(Integer walkingMinutes, Integer transitMinutes) {
        this.walkingMinutes = walkingMinutes;
        this.transitMinutes = transitMinutes;
    }
}
