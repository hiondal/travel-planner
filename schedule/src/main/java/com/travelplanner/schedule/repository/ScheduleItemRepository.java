package com.travelplanner.schedule.repository;

import com.travelplanner.schedule.domain.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 일정 아이템 리포지토리.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, String> {

    List<ScheduleItem> findByTripIdOrderByVisitDatetimeAsc(String tripId);

    Optional<ScheduleItem> findByIdAndTripId(String id, String tripId);

    @Query("SELECT COUNT(si) FROM ScheduleItem si WHERE si.tripId = :tripId")
    int countByTripId(@Param("tripId") String tripId);

    @Query("SELECT si FROM ScheduleItem si WHERE si.tripId = :tripId ORDER BY si.visitDatetime ASC")
    List<ScheduleItem> findSurroundingItems(@Param("tripId") String tripId);
}
