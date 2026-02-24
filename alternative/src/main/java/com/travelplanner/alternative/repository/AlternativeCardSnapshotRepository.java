package com.travelplanner.alternative.repository;

import com.travelplanner.alternative.domain.AlternativeCardSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 대안 카드 스냅샷 JPA 리포지토리.
 */
public interface AlternativeCardSnapshotRepository extends JpaRepository<AlternativeCardSnapshot, String> {
}
