package com.travelplanner.place.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.place.domain.Place;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 장소 검색 결과 요약 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class PlaceSummary {

    @JsonProperty("place_id")
    private final String placeId;

    private final String name;
    private final String address;
    private final Float rating;

    @JsonProperty("business_hours")
    private final List<BusinessHourDto> businessHours;

    private final CoordinatesDto coordinates;

    private PlaceSummary(String placeId, String name, String address, Float rating,
                         List<BusinessHourDto> businessHours, CoordinatesDto coordinates) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.businessHours = businessHours;
        this.coordinates = coordinates;
    }

    /**
     * Place 엔티티로부터 PlaceSummary DTO를 생성한다.
     *
     * @param place 장소 엔티티
     * @return PlaceSummary
     */
    public static PlaceSummary from(Place place) {
        List<BusinessHourDto> hours = place.getBusinessHours().stream()
                .map(BusinessHourDto::from)
                .collect(Collectors.toList());
        return new PlaceSummary(
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getRating() != null ? place.getRating().floatValue() : null,
                hours,
                CoordinatesDto.from(place.getCoordinates())
        );
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

    public Float getRating() {
        return rating;
    }

    public List<BusinessHourDto> getBusinessHours() {
        return businessHours;
    }

    public CoordinatesDto getCoordinates() {
        return coordinates;
    }
}
