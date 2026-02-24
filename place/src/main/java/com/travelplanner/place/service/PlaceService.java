package com.travelplanner.place.service;

import com.travelplanner.place.domain.Place;
import com.travelplanner.place.dto.internal.NearbySearchResult;

import java.util.List;

/**
 * 장소 서비스 인터페이스.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public interface PlaceService {

    /**
     * 키워드와 도시 기반으로 장소를 검색한다.
     *
     * @param keyword 검색 키워드
     * @param city    검색 도시
     * @return 검색 결과 장소 목록
     */
    List<Place> searchPlaces(String keyword, String city);

    /**
     * 장소 상세 정보를 조회한다.
     *
     * @param placeId Google Place ID
     * @return 장소 상세 정보
     */
    Place getPlaceDetail(String placeId);

    /**
     * 좌표 기반 주변 장소를 검색한다.
     *
     * @param lat      기준 위도
     * @param lng      기준 경도
     * @param category 카테고리
     * @param radius   검색 반경 (미터)
     * @return 주변 장소 검색 결과
     */
    NearbySearchResult searchNearbyPlaces(double lat, double lng, String category, int radius);
}
