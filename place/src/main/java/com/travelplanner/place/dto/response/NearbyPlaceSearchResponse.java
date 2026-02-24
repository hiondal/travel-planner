package com.travelplanner.place.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.place.dto.internal.NearbySearchResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 주변 장소 검색 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class NearbyPlaceSearchResponse {

    private final List<NearbyPlaceDto> places;

    @JsonProperty("radius_used")
    private final int radiusUsed;

    private NearbyPlaceSearchResponse(List<NearbyPlaceDto> places, int radiusUsed) {
        this.places = places;
        this.radiusUsed = radiusUsed;
    }

    /**
     * NearbySearchResult로부터 응답 DTO를 생성한다.
     *
     * @param result 주변 검색 결과
     * @return NearbyPlaceSearchResponse
     */
    public static NearbyPlaceSearchResponse of(NearbySearchResult result) {
        List<NearbyPlaceDto> dtos = result.getPlaces().stream()
                .map(np -> new NearbyPlaceDto(
                        np.getPlace().getId(),
                        np.getPlace().getName(),
                        np.getPlace().getAddress(),
                        np.getDistanceM(),
                        np.getPlace().getRating() != null ? np.getPlace().getRating().floatValue() : null,
                        np.getPlace().getCategory(),
                        CoordinatesDto.from(np.getPlace().getCoordinates()),
                        np.isOpen()
                ))
                .collect(Collectors.toList());
        return new NearbyPlaceSearchResponse(dtos, result.getRadiusUsed());
    }

    public List<NearbyPlaceDto> getPlaces() {
        return places;
    }

    public int getRadiusUsed() {
        return radiusUsed;
    }
}
