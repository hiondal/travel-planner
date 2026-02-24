package com.travelplanner.alternative.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.alternative.dto.internal.SelectResult;
import lombok.Getter;

/**
 * 대안 선택 응답 DTO.
 */
@Getter
public class SelectAlternativeResponse {

    @JsonProperty("schedule_item_id")
    private final String scheduleItemId;

    @JsonProperty("original_place")
    private final PlaceRefDto originalPlace;

    @JsonProperty("new_place")
    private final PlaceRefDto newPlace;

    @JsonProperty("travel_time_diff_minutes")
    private final int travelTimeDiffMinutes;

    private SelectAlternativeResponse(SelectResult result) {
        this.scheduleItemId = result.getScheduleItemId();
        this.originalPlace = new PlaceRefDto(result.getOriginalPlaceId(), result.getOriginalPlaceName());
        this.newPlace = new PlaceRefDto(result.getNewPlaceId(), result.getNewPlaceName());
        this.travelTimeDiffMinutes = result.getTravelTimeDiffMinutes();
    }

    public static SelectAlternativeResponse of(SelectResult result) {
        return new SelectAlternativeResponse(result);
    }
}
