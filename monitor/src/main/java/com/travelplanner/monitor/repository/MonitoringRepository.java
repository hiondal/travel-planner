package com.travelplanner.monitor.repository;

import com.travelplanner.monitor.domain.MonitoringTarget;
import com.travelplanner.monitor.domain.PlaceStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 모니터링 대상 리포지토리.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public interface MonitoringRepository extends JpaRepository<MonitoringTarget, String> {

    Optional<MonitoringTarget> findByPlaceId(String placeId);

    Optional<MonitoringTarget> findTopByPlaceIdOrderByVisitDatetimeDesc(String placeId);

    Optional<MonitoringTarget> findByScheduleItemId(String scheduleItemId);

    @Query("SELECT mt FROM MonitoringTarget mt WHERE mt.visitDatetime BETWEEN :from AND :to")
    List<MonitoringTarget> findUpcomingTargets(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    @Modifying
    @Query("UPDATE MonitoringTarget mt SET mt.currentStatus = :status, " +
           "mt.currentStatusUpdatedAt = :updatedAt WHERE mt.placeId = :placeId")
    void updateCurrentStatus(
        @Param("placeId") String placeId,
        @Param("status") PlaceStatusEnum status,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    List<MonitoringTarget> findByPlaceIdIn(List<String> placeIds);
}
