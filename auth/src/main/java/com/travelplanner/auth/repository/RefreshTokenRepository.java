package com.travelplanner.auth.repository;

import com.travelplanner.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token 영속화 JPA 레포지토리.
 *
 * <p>Redis TTL 만료 시 폴백용으로 사용되는 PostgreSQL Refresh Token 저장소.
 * 만료된 토큰은 주기적 배치 삭제를 통해 정리된다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * 토큰 값으로 Refresh Token을 조회한다.
     *
     * @param refreshToken 리프레시 토큰 값
     * @return 조회된 Refresh Token (없으면 빈 Optional)
     */
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    /**
     * 사용자 ID로 Refresh Token을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 조회된 Refresh Token (없으면 빈 Optional)
     */
    Optional<RefreshToken> findByUserId(String userId);

    /**
     * 특정 토큰 값에 해당하는 레코드를 삭제한다.
     *
     * @param refreshToken 삭제할 리프레시 토큰 값
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.refreshToken = :refreshToken")
    void deleteByRefreshToken(@Param("refreshToken") String refreshToken);

    /**
     * 특정 사용자의 모든 Refresh Token을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(String userId);

    /**
     * 만료된 Refresh Token을 일괄 삭제한다 (배치용).
     *
     * @param now 현재 시각
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
