package com.travelplanner.monitor.repository;

import com.travelplanner.monitor.domain.CollectedData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 수집 데이터 리포지토리.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public interface CollectedDataRepository extends JpaRepository<CollectedData, String> {

    @Query("SELECT cd FROM CollectedData cd WHERE cd.placeId = :placeId " +
           "ORDER BY cd.collectedAt DESC")
    Optional<CollectedData> findLatestByPlaceId(@Param("placeId") String placeId,
        org.springframework.data.domain.Pageable pageable);
}
