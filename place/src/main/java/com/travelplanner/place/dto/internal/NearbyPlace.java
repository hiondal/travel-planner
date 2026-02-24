package com.travelplanner.place.dto.internal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.place.domain.Place;

/**
 * 주변 장소 내부 모델.
 *
 * <p>거리 및 영업 중 여부가 포함된 장소 정보를 담는다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class NearbyPlace {

    private final Place place;
    private final int distanceM;
    private final boolean isOpen;

    @JsonCreator
    public NearbyPlace(
            @JsonProperty("place") Place place,
            @JsonProperty("distanceM") int distanceM,
            @JsonProperty("open") boolean isOpen) {
        this.place = place;
        this.distanceM = distanceM;
        this.isOpen = isOpen;
    }

    public Place getPlace() {
        return place;
    }

    public int getDistanceM() {
        return distanceM;
    }

    public boolean isOpen() {
        return isOpen;
    }
}
