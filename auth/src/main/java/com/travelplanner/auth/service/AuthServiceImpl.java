package com.travelplanner.auth.service;

import com.travelplanner.auth.client.OAuthClient;
import com.travelplanner.auth.client.OAuthProfile;
import com.travelplanner.auth.domain.Consent;
import com.travelplanner.auth.domain.RefreshToken;
import com.travelplanner.auth.domain.User;
import com.travelplanner.auth.dto.internal.SocialLoginResult;
import com.travelplanner.auth.dto.internal.TokenInvalidateResult;
import com.travelplanner.auth.dto.internal.TokenRefreshResult;
import com.travelplanner.auth.repository.AuthSessionRedisRepository;
import com.travelplanner.auth.repository.ConsentRepository;
import com.travelplanner.auth.repository.RefreshTokenRepository;
import com.travelplanner.auth.repository.UserRepository;
import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.common.exception.BusinessException;
import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.common.security.JwtProvider;
import com.travelplanner.common.security.JwtToken;
import com.travelplanner.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 인증 서비스 구현체.
 *
 * <p>Google OAuth2 소셜 로그인, JWT 토큰 관리, 사용자 동의 처리를 담당한다.
 * Refresh Token은 Redis(주 저장소)와 PostgreSQL(폴백)에 이중 저장된다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ConsentRepository consentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthSessionRedisRepository authSessionRedisRepository;
    private final OAuthClient oAuthClient;
    private final JwtProvider jwtProvider;

    @Value("${jwt.refresh-token-validity:2592000}")
    private int refreshTokenValiditySeconds;

    /**
     * {@inheritDoc}
     *
     * <p>처리 순서:</p>
     * <ol>
     *   <li>OAuth 코드로 프로파일 조회 (Google API 호출)</li>
     *   <li>신규 사용자 생성 또는 기존 사용자 프로파일 업데이트</li>
     *   <li>JWT Access Token + Refresh Token 생성</li>
     *   <li>Refresh Token Redis 저장 (주) + PostgreSQL 저장 (폴백)</li>
     * </ol>
     */
    @Override
    @Transactional
    public SocialLoginResult socialLogin(String provider, String oauthCode) {
        OAuthProfile profile = oAuthClient.verify(provider, oauthCode);
        log.info("OAuth 검증 성공: provider={}, providerId={}", profile.getProvider(), profile.getProviderId());

        boolean isNewUser;
        User user;

        Optional<User> existingUser = userRepository.findByProviderAndProviderId(
            profile.getProvider(), profile.getProviderId()
        );

        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.updateProfile(profile.getName(), profile.getAvatarUrl());
            isNewUser = false;
            log.debug("기존 사용자 로그인: userId={}", user.getId());
        } else {
            user = createNewUser(profile);
            isNewUser = true;
            log.info("신규 사용자 생성: email={}", profile.getEmail());
        }

        JwtToken jwtToken = jwtProvider.generateToken(buildUserPrincipal(user));
        saveRefreshToken(user.getId(), jwtToken.getRefreshToken(), user.getTier());

        return new SocialLoginResult(jwtToken, user, isNewUser);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Redis에서 세션을 우선 조회하고, 없으면 PostgreSQL 폴백으로 조회한다.</p>
     */
    @Override
    @Transactional
    public TokenRefreshResult refreshToken(String refreshToken) {
        String userId = resolveUserIdFromRefreshToken(refreshToken);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER", userId));

        UserPrincipal principal = buildUserPrincipal(user);
        String newAccessToken = jwtProvider.generateAccessToken(principal);

        log.debug("Access Token 갱신: userId={}", userId);
        return new TokenRefreshResult(newAccessToken, 1800);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void logout(String refreshToken, String userId) {
        authSessionRedisRepository.deleteSessionByUserId(userId, refreshToken);
        refreshTokenRepository.deleteByRefreshToken(refreshToken);
        log.info("로그아웃 처리 완료: userId={}", userId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>처리 순서:</p>
     * <ol>
     *   <li>사용자의 구독 등급 업데이트</li>
     *   <li>기존 Refresh Token 무효화</li>
     *   <li>새 JWT 발급 (변경된 tier 클레임 포함)</li>
     *   <li>새 Refresh Token 저장</li>
     * </ol>
     */
    @Override
    @Transactional
    public TokenInvalidateResult invalidateAndReissueToken(String userId, String newTier) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER", userId));

        SubscriptionTier tier = SubscriptionTier.valueOf(newTier);
        user.updateTier(tier);

        // 기존 Refresh Token 삭제
        authSessionRedisRepository.findRefreshTokenByUserId(userId)
            .ifPresent(oldToken -> {
                authSessionRedisRepository.deleteSessionByUserId(userId, oldToken);
                refreshTokenRepository.deleteByUserId(userId);
            });

        JwtToken newToken = jwtProvider.generateToken(buildUserPrincipal(user));
        saveRefreshToken(userId, newToken.getRefreshToken(), tier);

        log.info("토큰 재발급 완료: userId={}, tier={}", userId, newTier);
        return new TokenInvalidateResult(newToken.getAccessToken(), newTier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Consent saveConsent(String userId, boolean location, boolean push, LocalDateTime consentedAt) {
        Consent consent = Consent.create(userId, location, push, consentedAt);
        Consent saved = consentRepository.save(consent);
        log.info("동의 저장 완료: userId={}, location={}, push={}", userId, location, push);
        return saved;
    }

    /**
     * OAuth 프로파일로 신규 사용자를 생성한다.
     *
     * @param profile OAuth 프로파일
     * @return 생성된 User 엔티티
     */
    private User createNewUser(OAuthProfile profile) {
        User user = User.create(
            profile.getProvider(),
            profile.getProviderId(),
            profile.getEmail(),
            profile.getName(),
            profile.getAvatarUrl()
        );
        return userRepository.save(user);
    }

    /**
     * User 엔티티로 UserPrincipal을 생성한다.
     *
     * @param user 사용자 엔티티
     * @return UserPrincipal 인스턴스
     */
    private UserPrincipal buildUserPrincipal(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getTier());
    }

    /**
     * Refresh Token을 Redis와 PostgreSQL에 저장한다.
     *
     * @param userId       사용자 ID
     * @param refreshToken 리프레시 토큰
     * @param tier         구독 등급
     */
    private void saveRefreshToken(String userId, String refreshToken, SubscriptionTier tier) {
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenValiditySeconds);

        // Redis 저장 (주 저장소)
        authSessionRedisRepository.saveSession(
            userId,
            refreshToken,
            tier.name(),
            expiresAt.toString()
        );

        // PostgreSQL 저장 (폴백)
        refreshTokenRepository.deleteByUserId(userId);
        RefreshToken token = RefreshToken.create(userId, refreshToken, expiresAt);
        refreshTokenRepository.save(token);
    }

    /**
     * Refresh Token으로 사용자 ID를 조회한다.
     * Redis 조회 실패 시 PostgreSQL 폴백으로 조회한다.
     *
     * @param refreshToken 리프레시 토큰
     * @return 사용자 ID
     * @throws BusinessException 유효하지 않은 Refresh Token인 경우
     */
    private String resolveUserIdFromRefreshToken(String refreshToken) {
        // Redis 우선 조회
        Optional<String> userIdFromRedis = authSessionRedisRepository.findUserIdByRefreshToken(refreshToken);
        if (userIdFromRedis.isPresent()) {
            return userIdFromRedis.get();
        }

        // PostgreSQL 폴백 조회
        RefreshToken tokenEntity = refreshTokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN",
                "유효하지 않은 Refresh Token입니다.", 401));

        if (tokenEntity.isExpired()) {
            throw new BusinessException("EXPIRED_REFRESH_TOKEN", "Refresh Token이 만료되었습니다.", 401);
        }

        return tokenEntity.getUserId();
    }
}
