package com.travelplanner.place.repository;

import com.travelplanner.place.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Place JPA Repository.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public interface PlaceJpaRepository extends JpaRepository<Place, String> {

    /**
     * 도시와 키워드로 장소를 검색한다.
     *
     * @param city    도시명
     * @param keyword 검색 키워드
     * @return 검색 결과 장소 목록
     */
    @Query("SELECT p FROM Place p WHERE p.city = :city AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Place> findByCityAndKeyword(@Param("city") String city, @Param("keyword") String keyword);

    /**
     * 좌표 반경 내 특정 카테고리 장소를 검색한다 (PostGIS 없이 NUMERIC 범위 쿼리).
     *
     * @param lat      기준 위도
     * @param lng      기준 경도
     * @param radiusM  검색 반경 (미터)
     * @param category 카테고리
     * @return 검색 결과 장소 목록
     */
    @Query("SELECT p FROM Place p WHERE " +
           "ABS(p.lat - :lat) <= :latDelta AND " +
           "ABS(p.lng - :lng) <= :lngDelta AND " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :category, '%'))")
    List<Place> findNearby(@Param("lat") double lat,
                           @Param("lng") double lng,
                           @Param("latDelta") double latDelta,
                           @Param("lngDelta") double lngDelta,
                           @Param("category") String category);
}
