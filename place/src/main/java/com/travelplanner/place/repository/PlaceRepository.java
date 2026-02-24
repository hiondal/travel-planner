package com.travelplanner.place.repository;

import com.travelplanner.place.domain.Place;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Place 리포지토리 구현체.
 *
 * <p>PlaceJpaRepository를 래핑하여 도메인 레이어에서 사용하는 인터페이스를 제공한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class PlaceRepository {

    /** 위도 1도 = 약 111km, 1km ≈ 0.009도 */
    private static final double METERS_PER_DEGREE_LAT = 111000.0;

    private final PlaceJpaRepository jpaRepository;

    /**
     * ID로 장소를 조회한다.
     *
     * @param placeId Google Place ID
     * @return 장소 (없으면 empty)
     */
    public Optional<Place> findById(String placeId) {
        return jpaRepository.findById(placeId);
    }

    /**
     * 도시와 키워드로 장소를 검색한다.
     *
     * @param city    도시명
     * @param keyword 검색 키워드
     * @return 검색 결과 목록
     */
    public List<Place> findByCityAndKeyword(String city, String keyword) {
        return jpaRepository.findByCityAndKeyword(city, keyword);
    }

    /**
     * 좌표 반경 내 특정 카테고리 장소를 검색한다.
     *
     * @param lat      기준 위도
     * @param lng      기준 경도
     * @param radiusM  검색 반경 (미터)
     * @param category 카테고리
     * @return 검색 결과 목록
     */
    public List<Place> findNearby(double lat, double lng, int radiusM, String category) {
        double latDelta = radiusM / METERS_PER_DEGREE_LAT;
        double lngDelta = radiusM / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));
        return jpaRepository.findNearby(lat, lng, latDelta, lngDelta, category);
    }

    /**
     * 장소를 저장(upsert)한다.
     *
     * @param place 저장할 장소
     * @return 저장된 장소
     */
    public Place save(Place place) {
        return jpaRepository.save(place);
    }

    /**
     * 장소 목록을 일괄 저장(upsert)한다.
     *
     * @param places 저장할 장소 목록
     * @return 저장된 장소 목록
     */
    public List<Place> saveAll(List<Place> places) {
        return jpaRepository.saveAll(places);
    }
}
