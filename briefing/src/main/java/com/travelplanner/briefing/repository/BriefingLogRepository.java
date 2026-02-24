package com.travelplanner.briefing.repository;

import com.travelplanner.briefing.domain.BriefingLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 브리핑 로그 JPA 리포지토리.
 */
public interface BriefingLogRepository extends JpaRepository<BriefingLog, String> {
}
