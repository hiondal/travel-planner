package com.travelplanner.briefing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 브리핑 생성 요청 DTO.
 */
@Getter
@NoArgsConstructor
public class GenerateBriefingRequest {

    @NotBlank
    @JsonProperty("schedule_item_id")
    private String scheduleItemId;

    @NotBlank
    @JsonProperty("place_id")
    private String placeId;

    @NotBlank
    @JsonProperty("user_id")
    private String userId;

    @NotNull
    @JsonProperty("departure_time")
    private LocalDateTime departureTime;

    @NotNull
    @JsonProperty("triggered_at")
    private LocalDateTime triggeredAt;
}
