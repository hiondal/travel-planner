package com.travelplanner.place.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.place.domain.Place;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 장소 상세 조회 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class PlaceDetailResponse {

    @JsonProperty("place_id")
    private final String placeId;

    private final String name;
    private final String address;
    private final String category;
    private final Float rating;

    @JsonProperty("business_hours")
    private final List<BusinessHourDto> businessHours;

    private final CoordinatesDto coordinates;
    private final String timezone;

    @JsonProperty("photo_url")
    private final String photoUrl;

    private PlaceDetailResponse(String placeId, String name, String address, String category,
                                Float rating, List<BusinessHourDto> businessHours,
                                CoordinatesDto coordinates, String timezone, String photoUrl) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
        this.category = category;
        this.rating = rating;
        this.businessHours = businessHours;
        this.coordinates = coordinates;
        this.timezone = timezone;
        this.photoUrl = photoUrl;
    }

    /**
     * Place 엔티티로부터 상세 응답 DTO를 생성한다.
     *
     * @param place 장소 엔티티
     * @return PlaceDetailResponse
     */
    public static PlaceDetailResponse from(Place place) {
        List<BusinessHourDto> hours = place.getBusinessHours().stream()
                .map(BusinessHourDto::from)
                .collect(Collectors.toList());
        return new PlaceDetailResponse(
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getCategory(),
                place.getRating() != null ? place.getRating().floatValue() : null,
                hours,
                CoordinatesDto.from(place.getCoordinates()),
                place.getTimezone(),
                place.getPhotoUrl()
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

    public String getCategory() {
        return category;
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

    public String getTimezone() {
        return timezone;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}
