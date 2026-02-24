package com.travelplanner.auth.repository;

import com.travelplanner.auth.domain.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 동의 이력 JPA 레포지토리.
 *
 * <p>동의 이력은 append-only 방식으로 저장되며,
 * 최신 동의 정보는 {@code findLatestByUserId}로 조회한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Repository
public interface ConsentRepository extends JpaRepository<Consent, String> {

    /**
     * 특정 사용자의 가장 최신 동의 이력을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 가장 최근 동의 이력 (없으면 빈 Optional)
     */
    @Query("SELECT c FROM Consent c WHERE c.userId = :userId ORDER BY c.createdAt DESC LIMIT 1")
    Optional<Consent> findLatestByUserId(@Param("userId") String userId);

    /**
     * 특정 사용자의 동의 이력이 존재하는지 확인한다.
     *
     * @param userId 사용자 ID
     * @return 동의 이력 존재 여부
     */
    boolean existsByUserId(String userId);
}
