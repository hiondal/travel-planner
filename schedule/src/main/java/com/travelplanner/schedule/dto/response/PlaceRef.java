package com.travelplanner.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 장소 참조 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class PlaceRef {

    @JsonProperty("place_id")
    private String placeId;

    @JsonProperty("place_name")
    private String placeName;
}
