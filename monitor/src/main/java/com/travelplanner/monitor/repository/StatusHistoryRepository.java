package com.travelplanner.monitor.repository;

import com.travelplanner.monitor.domain.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상태 이력 리포지토리.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, String> {

    @Query("SELECT sh FROM StatusHistory sh WHERE sh.placeId = :placeId " +
           "ORDER BY sh.judgmentAt DESC")
    List<StatusHistory> findLatestByPlaceId(
        @Param("placeId") String placeId,
        org.springframework.data.domain.Pageable pageable
    );

    List<StatusHistory> findByPlaceIdAndJudgmentAtBetween(
        String placeId, LocalDateTime from, LocalDateTime to);
}
