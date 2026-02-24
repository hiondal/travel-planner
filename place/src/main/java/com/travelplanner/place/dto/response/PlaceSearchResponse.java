package com.travelplanner.place.dto.response;

import com.travelplanner.place.domain.Place;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 장소 검색 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class PlaceSearchResponse {

    private final List<PlaceSummary> places;

    private PlaceSearchResponse(List<PlaceSummary> places) {
        this.places = places;
    }

    /**
     * Place 목록으로부터 응답 DTO를 생성한다.
     *
     * @param places 장소 목록
     * @return PlaceSearchResponse
     */
    public static PlaceSearchResponse of(List<Place> places) {
        List<PlaceSummary> summaries = places.stream()
                .map(PlaceSummary::from)
                .collect(Collectors.toList());
        return new PlaceSearchResponse(summaries);
    }

    public List<PlaceSummary> getPlaces() {
        return places;
    }
}
