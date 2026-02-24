package com.travelplanner.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.schedule.domain.Trip;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 여행 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class TripResponse {

    @JsonProperty("trip_id")
    private String tripId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    @JsonProperty("city")
    private String city;

    @JsonProperty("status")
    private String status;

    @JsonProperty("schedule_items")
    private List<ScheduleItemSummary> scheduleItems;

    public static TripResponse from(Trip trip) {
        return TripResponse.builder()
            .tripId(trip.getId())
            .name(trip.getName())
            .startDate(trip.getStartDate())
            .endDate(trip.getEndDate())
            .city(trip.getCity())
            .status(trip.getStatus().name())
            .scheduleItems(
                trip.getScheduleItems().stream()
                    .map(ScheduleItemSummary::from)
                    .collect(Collectors.toList())
            )
            .build();
    }
}
