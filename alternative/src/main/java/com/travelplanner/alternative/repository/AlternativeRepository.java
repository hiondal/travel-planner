package com.travelplanner.alternative.repository;

import com.travelplanner.alternative.domain.Alternative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 대안 JPA 리포지토리.
 */
public interface AlternativeRepository extends JpaRepository<Alternative, String> {

    List<Alternative> findByOriginalPlaceId(String originalPlaceId);
}
