package com.travelplanner.place.dto.internal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 주변 장소 검색 결과 내부 모델.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class NearbySearchResult {

    private final List<NearbyPlace> places;
    private final int radiusUsed;

    @JsonCreator
    public NearbySearchResult(
            @JsonProperty("places") List<NearbyPlace> places,
            @JsonProperty("radiusUsed") int radiusUsed) {
        this.places = places != null ? places : new ArrayList<>();
        this.radiusUsed = radiusUsed;
    }

    public List<NearbyPlace> getPlaces() {
        return places;
    }

    public int getRadiusUsed() {
        return radiusUsed;
    }
}
