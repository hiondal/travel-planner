package com.travelplanner.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 세션 정보 Redis 레포지토리.
 *
 * <p>Redis DB1을 사용하여 Refresh Token 세션 정보를 관리한다.
 * 키 패턴: {@code auth:session:{refreshToken}}, {@code auth:refresh:{userId}}</p>
 *
 * <ul>
 *   <li>{@code auth:session:{refreshToken}} — Hash: userId, tier, expiresAt</li>
 *   <li>{@code auth:refresh:{userId}} — String: refreshToken (역방향 조회)</li>
 * </ul>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class AuthSessionRedisRepository {

    private static final String SESSION_KEY_PREFIX = "auth:session:";
    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";
    private static final long SESSION_TTL_DAYS = 30L;

    /** Redis DB1 전용 템플릿 (세션 저장) */
    private final RedisTemplate<String, String> authRedisTemplate;

    /** Redis DB0 전용 템플릿 (JWT 블랙리스트) */
    private final RedisTemplate<String, String> blacklistRedisTemplate;

    /**
     * 세션 정보를 Redis에 저장한다.
     *
     * @param userId       사용자 ID
     * @param refreshToken 리프레시 토큰
     * @param tier         구독 등급
     * @param expiresAt    만료 일시 (ISO 8601 문자열)
     */
    public void saveSession(String userId, String refreshToken, String tier, String expiresAt) {
        String sessionKey = SESSION_KEY_PREFIX + refreshToken;
        String refreshKey = REFRESH_KEY_PREFIX + userId;

        authRedisTemplate.opsForHash().putAll(sessionKey, Map.of(
            "userId", userId,
            "tier", tier,
            "expiresAt", expiresAt
        ));
        authRedisTemplate.expire(sessionKey, SESSION_TTL_DAYS, TimeUnit.DAYS);

        authRedisTemplate.opsForValue().set(refreshKey, refreshToken);
        authRedisTemplate.expire(refreshKey, SESSION_TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * 리프레시 토큰으로 세션의 사용자 ID를 조회한다.
     *
     * @param refreshToken 리프레시 토큰
     * @return 사용자 ID (없으면 빈 Optional)
     */
    public Optional<String> findUserIdByRefreshToken(String refreshToken) {
        String sessionKey = SESSION_KEY_PREFIX + refreshToken;
        Object userId = authRedisTemplate.opsForHash().get(sessionKey, "userId");
        return Optional.ofNullable(userId != null ? userId.toString() : null);
    }

    /**
     * 사용자 ID로 Refresh Token을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 리프레시 토큰 (없으면 빈 Optional)
     */
    public Optional<String> findRefreshTokenByUserId(String userId) {
        String refreshKey = REFRESH_KEY_PREFIX + userId;
        String token = authRedisTemplate.opsForValue().get(refreshKey);
        return Optional.ofNullable(token);
    }

    /**
     * 리프레시 토큰에 해당하는 세션을 삭제한다.
     *
     * @param refreshToken 리프레시 토큰
     */
    public void deleteSession(String refreshToken) {
        String sessionKey = SESSION_KEY_PREFIX + refreshToken;
        Object userId = authRedisTemplate.opsForHash().get(sessionKey, "userId");
        if (userId != null) {
            authRedisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        }
        authRedisTemplate.delete(sessionKey);
    }

    /**
     * 사용자 ID에 해당하는 모든 세션을 삭제한다.
     *
     * @param userId       사용자 ID
     * @param refreshToken 삭제할 리프레시 토큰
     */
    public void deleteSessionByUserId(String userId, String refreshToken) {
        authRedisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        if (refreshToken != null) {
            authRedisTemplate.delete(SESSION_KEY_PREFIX + refreshToken);
        }
    }

    /**
     * Access Token의 JTI를 블랙리스트에 등록한다 (JWT 무효화).
     *
     * @param jti        JWT ID (JTI 클레임)
     * @param ttlSeconds 블랙리스트 TTL (Access Token 잔여 유효시간)
     */
    public void addToBlacklist(String jti, long ttlSeconds) {
        String key = BLACKLIST_KEY_PREFIX + jti;
        blacklistRedisTemplate.opsForValue().set(key, "1");
        blacklistRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * JWT JTI가 블랙리스트에 등록되어 있는지 확인한다.
     *
     * @param jti JWT ID
     * @return 블랙리스트 등록 여부
     */
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(blacklistRedisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
    }
}
