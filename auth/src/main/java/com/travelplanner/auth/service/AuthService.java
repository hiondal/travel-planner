package com.travelplanner.auth.service;

import com.travelplanner.auth.domain.Consent;
import com.travelplanner.auth.dto.internal.SocialLoginResult;
import com.travelplanner.auth.dto.internal.TokenInvalidateResult;
import com.travelplanner.auth.dto.internal.TokenRefreshResult;

import java.time.LocalDateTime;

/**
 * 인증 서비스 인터페이스.
 *
 * <p>소셜 로그인, JWT 토큰 관리, 사용자 동의 처리의 계약을 정의한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public interface AuthService {

    /**
     * 소셜 로그인을 처리하고 JWT 토큰을 발급한다.
     *
     * <p>신규 사용자는 자동으로 생성되며, 기존 사용자는 프로파일이 업데이트된다.</p>
     *
     * @param provider   OAuth 프로바이더 식별자 (google)
     * @param oauthCode  OAuth Authorization Code
     * @return 로그인 결과 (JWT 토큰 쌍, 사용자 정보, 신규 여부)
     */
    SocialLoginResult socialLogin(String provider, String oauthCode);

    /**
     * Refresh Token으로 새 Access Token을 발급한다.
     *
     * @param refreshToken 유효한 Refresh Token
     * @return 새 Access Token과 만료 시간
     */
    TokenRefreshResult refreshToken(String refreshToken);

    /**
     * 로그아웃을 처리한다.
     *
     * <p>Refresh Token을 무효화하고 Redis 세션을 삭제한다.
     * Access Token의 JTI를 블랙리스트에 등록한다.</p>
     *
     * @param refreshToken 무효화할 Refresh Token
     * @param userId       로그아웃하는 사용자 ID
     */
    void logout(String refreshToken, String userId);

    /**
     * 구독 티어 변경 시 기존 토큰을 즉시 무효화하고 새 토큰을 발급한다.
     *
     * <p>PAY 서비스에서 구독 완료 후 내부 호출한다.</p>
     *
     * @param userId  대상 사용자 ID
     * @param newTier 변경된 구독 등급
     * @return 새 Access Token과 구독 등급
     */
    TokenInvalidateResult invalidateAndReissueToken(String userId, String newTier);

    /**
     * 사용자 동의 이력을 저장한다.
     *
     * @param userId      사용자 ID
     * @param location    위치정보 수집 동의 여부
     * @param push        Push 알림 동의 여부
     * @param consentedAt 동의 일시
     * @return 저장된 동의 엔티티
     */
    Consent saveConsent(String userId, boolean location, boolean push, LocalDateTime consentedAt);
}
