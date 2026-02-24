package com.travelplanner.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 일정 장소 교체 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class ReplaceScheduleItemResponse {

    @JsonProperty("schedule_item_id")
    private String scheduleItemId;

    @JsonProperty("original_place")
    private PlaceRef originalPlace;

    @JsonProperty("new_place")
    private PlaceRef newPlace;

    @JsonProperty("travel_time_diff_minutes")
    private int travelTimeDiffMinutes;

    @JsonProperty("updated_schedule_items")
    private List<ScheduleItemSummary> updatedScheduleItems;
}
