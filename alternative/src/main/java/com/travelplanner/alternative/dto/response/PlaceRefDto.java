package com.travelplanner.alternative.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 장소 참조 DTO.
 */
@Getter
public class PlaceRefDto {

    @JsonProperty("place_id")
    private final String placeId;

    @JsonProperty("name")
    private final String name;

    public PlaceRefDto(String placeId, String name) {
        this.placeId = placeId;
        this.name = name;
    }
}
