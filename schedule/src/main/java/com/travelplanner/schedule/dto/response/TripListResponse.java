package com.travelplanner.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 여행 목록 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class TripListResponse {

    @JsonProperty("trips")
    private List<TripResponse> trips;

    @JsonProperty("total")
    private int total;

    public static TripListResponse of(List<TripResponse> trips) {
        return TripListResponse.builder()
            .trips(trips)
            .total(trips.size())
            .build();
    }
}
