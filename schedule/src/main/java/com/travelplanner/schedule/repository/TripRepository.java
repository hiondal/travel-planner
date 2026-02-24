package com.travelplanner.schedule.repository;

import com.travelplanner.schedule.domain.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 여행 일정 리포지토리.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public interface TripRepository extends JpaRepository<Trip, String> {

    Optional<Trip> findByIdAndUserId(String id, String userId);

    @Query("SELECT t FROM Trip t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    List<Trip> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
}
