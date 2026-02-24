package com.travelplanner.place.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 주변 장소 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class NearbyPlaceDto {

    @JsonProperty("place_id")
    private final String placeId;

    private final String name;
    private final String address;

    @JsonProperty("distance_m")
    private final int distanceM;

    private final Float rating;
    private final String category;
    private final CoordinatesDto coordinates;

    @JsonProperty("is_open")
    private final boolean isOpen;

    public NearbyPlaceDto(String placeId, String name, String address, int distanceM,
                          Float rating, String category, CoordinatesDto coordinates, boolean isOpen) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
        this.distanceM = distanceM;
        this.rating = rating;
        this.category = category;
        this.coordinates = coordinates;
        this.isOpen = isOpen;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getDistanceM() {
        return distanceM;
    }

    public Float getRating() {
        return rating;
    }

    public String getCategory() {
        return category;
    }

    public CoordinatesDto getCoordinates() {
        return coordinates;
    }

    public boolean isOpen() {
        return isOpen;
    }
}
