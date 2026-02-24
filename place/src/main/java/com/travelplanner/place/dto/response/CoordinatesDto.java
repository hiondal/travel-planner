package com.travelplanner.place.dto.response;

import com.travelplanner.place.domain.Coordinates;

/**
 * 위경도 좌표 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class CoordinatesDto {

    private final double lat;
    private final double lng;

    private CoordinatesDto(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    /**
     * Coordinates 도메인 객체로부터 DTO를 생성한다.
     *
     * @param coordinates 좌표 객체
     * @return CoordinatesDto
     */
    public static CoordinatesDto from(Coordinates coordinates) {
        return new CoordinatesDto(coordinates.getLat(), coordinates.getLng());
    }

    public static CoordinatesDto of(double lat, double lng) {
        return new CoordinatesDto(lat, lng);
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}
