package com.travelplanner.alternative.repository;

import com.travelplanner.alternative.domain.SelectionLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 대안 선택 로그 JPA 리포지토리.
 */
public interface SelectionLogRepository extends JpaRepository<SelectionLog, String> {
}
