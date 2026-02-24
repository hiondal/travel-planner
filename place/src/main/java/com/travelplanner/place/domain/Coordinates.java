package com.travelplanner.place.domain;

/**
 * 위경도 좌표 값 객체.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class Coordinates {

    private final double lat;
    private final double lng;

    public Coordinates(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    /**
     * 두 좌표 사이의 거리를 미터 단위로 계산한다 (Haversine 공식).
     *
     * @param other 비교할 좌표
     * @return 거리 (미터)
     */
    public double distanceTo(Coordinates other) {
        final double earthRadiusM = 6371000.0;
        double lat1Rad = Math.toRadians(this.lat);
        double lat2Rad = Math.toRadians(other.lat);
        double deltaLat = Math.toRadians(other.lat - this.lat);
        double deltaLng = Math.toRadians(other.lng - this.lng);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusM * c;
    }
}
