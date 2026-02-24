package com.travelplanner.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.schedule.domain.ScheduleItem;
import com.travelplanner.schedule.domain.Trip;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 일정표 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class ScheduleResponse {

    @JsonProperty("trip_id")
    private String tripId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("city")
    private String city;

    @JsonProperty("schedule_items")
    private List<ScheduleItemSummary> scheduleItems;

    public static ScheduleResponse from(Trip trip, List<ScheduleItem> items) {
        return ScheduleResponse.builder()
            .tripId(trip.getId())
            .name(trip.getName())
            .city(trip.getCity())
            .scheduleItems(
                items.stream()
                    .map(ScheduleItemSummary::from)
                    .collect(Collectors.toList())
            )
            .build();
    }
}
