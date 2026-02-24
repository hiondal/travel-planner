package com.travelplanner.auth.domain;

import com.travelplanner.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refresh Token 영속화 엔티티.
 *
 * <p>Redis TTL 만료 또는 장애 시 폴백으로 사용되는 PostgreSQL 기반 Refresh Token 저장소.
 * 주 저장소는 Redis DB1이며, 이 테이블은 내구성 보장을 위해 중복 저장된다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_tokens_refresh_token", columnList = "refresh_token", unique = true),
        @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    /** 사용자 ID (users.id 참조) */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    /** 리프레시 토큰 값 */
    @Column(name = "refresh_token", nullable = false, length = 500, unique = true)
    private String refreshToken;

    /** 만료 일시 */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 생성 일시 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Refresh Token 엔티티 생성 팩토리 메서드.
     *
     * @param userId       사용자 ID
     * @param refreshToken 리프레시 토큰 값
     * @param expiresAt    만료 일시
     * @return 생성된 RefreshToken 엔티티
     */
    public static RefreshToken create(String userId, String refreshToken, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.userId = userId;
        token.refreshToken = refreshToken;
        token.expiresAt = expiresAt;
        token.createdAt = LocalDateTime.now();
        return token;
    }

    /**
     * 토큰이 만료되었는지 확인한다.
     *
     * @return 만료 여부
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    @Override
    protected String getPrefix() {
        return "rtk";
    }
}
