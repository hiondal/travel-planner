package com.travelplanner.briefing.repository;

import com.travelplanner.briefing.domain.Briefing;
import com.travelplanner.briefing.domain.BriefingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 브리핑 JPA 리포지토리.
 */
public interface BriefingRepository extends JpaRepository<Briefing, String> {

    Optional<Briefing> findByIdAndUserId(String id, String userId);

    Optional<Briefing> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT b FROM Briefing b WHERE b.userId = :userId AND CAST(b.createdAt AS date) = :date ORDER BY b.createdAt DESC")
    List<Briefing> findByUserIdAndDate(@Param("userId") String userId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(b) FROM Briefing b WHERE b.userId = :userId AND CAST(b.createdAt AS date) = :date")
    int countByUserIdAndCreatedAtDate(@Param("userId") String userId, @Param("date") LocalDate date);
}
