package com.travelplanner.alternative.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대안 카드 선택 요청 DTO.
 */
@Getter
@NoArgsConstructor
public class SelectAlternativeRequest {

    @NotBlank
    @JsonProperty("original_place_id")
    private String originalPlaceId;

    @NotBlank
    @JsonProperty("schedule_item_id")
    private String scheduleItemId;

    @NotBlank
    @JsonProperty("trip_id")
    private String tripId;

    @Min(1)
    @Max(3)
    @JsonProperty("selected_rank")
    private int selectedRank;

    @Min(0)
    @JsonProperty("elapsed_seconds")
    private int elapsedSeconds;
}
