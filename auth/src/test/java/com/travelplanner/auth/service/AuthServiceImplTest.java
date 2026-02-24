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
import com.travelplanner.common.enums.OAuthProvider;
import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.common.exception.BusinessException;
import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.common.security.JwtProvider;
import com.travelplanner.common.security.JwtToken;
import com.travelplanner.common.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * AuthServiceImpl 단위 테스트.
 *
 * <p>소셜 로그인, 토큰 갱신, 로그아웃, 토큰 무효화, 동의 저장의
 * 핵심 비즈니스 로직을 검증한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl 단위 테스트")
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConsentRepository consentRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuthSessionRedisRepository authSessionRedisRepository;

    @Mock
    private OAuthClient oAuthClient;

    @Mock
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenValiditySeconds", 2592000);
    }

    // ===== 소셜 로그인 테스트 =====

    @Nested
    @DisplayName("소셜 로그인")
    class SocialLoginTest {

        @Test
        @DisplayName("신규 사용자 Google 로그인 성공 시 새 사용자 생성 후 JWT 발급")
        void socialLogin_newUser_success() {
            // given
            String provider = "google";
            String oauthCode = "auth_code_123";

            OAuthProfile profile = new OAuthProfile(
                "google_sub_123", "user@gmail.com", "테스트유저",
                "https://avatar.com/pic.jpg", OAuthProvider.GOOGLE
            );
            given(oAuthClient.verify(provider, oauthCode)).willReturn(profile);
            given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google_sub_123"))
                .willReturn(Optional.empty());

            User newUser = User.create(OAuthProvider.GOOGLE, "google_sub_123",
                "user@gmail.com", "테스트유저", "https://avatar.com/pic.jpg");
            ReflectionTestUtils.setField(newUser, "id", "usr_test123");
            given(userRepository.save(any(User.class))).willReturn(newUser);

            JwtToken jwtToken = new JwtToken("access.token.here", "refresh.token.here", 1800);
            given(jwtProvider.generateToken(any(UserPrincipal.class))).willReturn(jwtToken);

            // when
            SocialLoginResult result = authService.socialLogin(provider, oauthCode);

            // then
            assertThat(result).isNotNull();
            assertThat(result.isNewUser()).isTrue();
            assertThat(result.getJwtToken().getAccessToken()).isEqualTo("access.token.here");
            assertThat(result.getUser().getEmail()).isEqualTo("user@gmail.com");

            then(userRepository).should().save(any(User.class));
            then(authSessionRedisRepository).should().saveSession(
                anyString(), anyString(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("기존 사용자 로그인 시 프로파일 업데이트 후 JWT 발급")
        void socialLogin_existingUser_success() {
            // given
            String provider = "google";
            String oauthCode = "auth_code_456";

            OAuthProfile profile = new OAuthProfile(
                "google_sub_456", "existing@gmail.com", "기존유저",
                "https://new-avatar.com/pic.jpg", OAuthProvider.GOOGLE
            );
            given(oAuthClient.verify(provider, oauthCode)).willReturn(profile);

            User existingUser = User.create(OAuthProvider.GOOGLE, "google_sub_456",
                "existing@gmail.com", "기존닉네임", "https://old-avatar.com/pic.jpg");
            ReflectionTestUtils.setField(existingUser, "id", "usr_existing");
            given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google_sub_456"))
                .willReturn(Optional.of(existingUser));

            JwtToken jwtToken = new JwtToken("new.access.token", "new.refresh.token", 1800);
            given(jwtProvider.generateToken(any(UserPrincipal.class))).willReturn(jwtToken);

            // when
            SocialLoginResult result = authService.socialLogin(provider, oauthCode);

            // then
            assertThat(result.isNewUser()).isFalse();
            assertThat(result.getUser().getNickname()).isEqualTo("기존유저");

            then(userRepository).should(never()).save(any(User.class));
        }
    }

    // ===== 토큰 갱신 테스트 =====

    @Nested
    @DisplayName("토큰 갱신")
    class TokenRefreshTest {

        @Test
        @DisplayName("Redis에 세션이 있을 때 Access Token 갱신 성공")
        void refreshToken_fromRedis_success() {
            // given
            String refreshToken = "valid.refresh.token";
            String userId = "usr_test123";

            given(authSessionRedisRepository.findUserIdByRefreshToken(refreshToken))
                .willReturn(Optional.of(userId));

            User user = User.create(OAuthProvider.GOOGLE, "sub123",
                "user@gmail.com", "테스트유저", null);
            ReflectionTestUtils.setField(user, "id", userId);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            given(jwtProvider.generateAccessToken(any(UserPrincipal.class)))
                .willReturn("new.access.token");

            // when
            TokenRefreshResult result = authService.refreshToken(refreshToken);

            // then
            assertThat(result.getAccessToken()).isEqualTo("new.access.token");
            assertThat(result.getExpiresIn()).isEqualTo(1800);
        }

        @Test
        @DisplayName("Redis 미스 시 PostgreSQL 폴백으로 토큰 갱신")
        void refreshToken_fallbackToDb_success() {
            // given
            String refreshToken = "db.refresh.token";
            String userId = "usr_fallback";

            given(authSessionRedisRepository.findUserIdByRefreshToken(refreshToken))
                .willReturn(Optional.empty());

            RefreshToken tokenEntity = RefreshToken.create(userId, refreshToken,
                LocalDateTime.now().plusDays(30));
            given(refreshTokenRepository.findByRefreshToken(refreshToken))
                .willReturn(Optional.of(tokenEntity));

            User user = User.create(OAuthProvider.GOOGLE, "sub456",
                "fallback@gmail.com", "폴백유저", null);
            ReflectionTestUtils.setField(user, "id", userId);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            given(jwtProvider.generateAccessToken(any(UserPrincipal.class)))
                .willReturn("fallback.access.token");

            // when
            TokenRefreshResult result = authService.refreshToken(refreshToken);

            // then
            assertThat(result.getAccessToken()).isEqualTo("fallback.access.token");
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token으로 갱신 시 예외 발생")
        void refreshToken_invalidToken_throwsException() {
            // given
            String invalidToken = "invalid.token";
            given(authSessionRedisRepository.findUserIdByRefreshToken(invalidToken))
                .willReturn(Optional.empty());
            given(refreshTokenRepository.findByRefreshToken(invalidToken))
                .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 Refresh Token");
        }

        @Test
        @DisplayName("만료된 Refresh Token으로 갱신 시 예외 발생")
        void refreshToken_expiredToken_throwsException() {
            // given
            String expiredToken = "expired.token";
            given(authSessionRedisRepository.findUserIdByRefreshToken(expiredToken))
                .willReturn(Optional.empty());

            RefreshToken expiredEntity = RefreshToken.create("usr_expired", expiredToken,
                LocalDateTime.now().minusDays(1));
            given(refreshTokenRepository.findByRefreshToken(expiredToken))
                .willReturn(Optional.of(expiredEntity));

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(expiredToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("만료");
        }
    }

    // ===== 로그아웃 테스트 =====

    @Nested
    @DisplayName("로그아웃")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 성공 시 Redis 세션 삭제 및 DB Refresh Token 삭제")
        void logout_success() {
            // given
            String refreshToken = "logout.refresh.token";
            String userId = "usr_logout";

            // when
            authService.logout(refreshToken, userId);

            // then
            then(authSessionRedisRepository).should().deleteSessionByUserId(userId, refreshToken);
            then(refreshTokenRepository).should().deleteByRefreshToken(refreshToken);
        }
    }

    // ===== 토큰 무효화 테스트 =====

    @Nested
    @DisplayName("토큰 즉시 무효화 및 재발급")
    class TokenInvalidateTest {

        @Test
        @DisplayName("구독 등급 변경 시 기존 토큰 무효화 후 새 토큰 발급 성공")
        void invalidateAndReissue_success() {
            // given
            String userId = "usr_pay123";
            String newTier = "TRIP_PASS";

            User user = User.create(OAuthProvider.GOOGLE, "sub789",
                "pay@gmail.com", "결제유저", null);
            ReflectionTestUtils.setField(user, "id", userId);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            given(authSessionRedisRepository.findRefreshTokenByUserId(userId))
                .willReturn(Optional.of("old.refresh.token"));

            JwtToken newToken = new JwtToken("new.trip.access.token", "new.trip.refresh.token", 1800);
            given(jwtProvider.generateToken(any(UserPrincipal.class))).willReturn(newToken);

            // when
            TokenInvalidateResult result = authService.invalidateAndReissueToken(userId, newTier);

            // then
            assertThat(result.getAccessToken()).isEqualTo("new.trip.access.token");
            assertThat(result.getTier()).isEqualTo("TRIP_PASS");
            assertThat(user.getTier()).isEqualTo(SubscriptionTier.TRIP_PASS);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 요청 시 예외 발생")
        void invalidateAndReissue_userNotFound_throwsException() {
            // given
            String nonExistentUserId = "usr_notfound";
            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.invalidateAndReissueToken(nonExistentUserId, "TRIP_PASS"))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ===== 동의 저장 테스트 =====

    @Nested
    @DisplayName("사용자 동의 저장")
    class SaveConsentTest {

        @Test
        @DisplayName("동의 저장 성공")
        void saveConsent_success() {
            // given
            String userId = "usr_consent";
            boolean location = true;
            boolean push = true;
            LocalDateTime consentedAt = LocalDateTime.now();

            Consent consent = Consent.create(userId, location, push, consentedAt);
            ReflectionTestUtils.setField(consent, "id", "cns_test123");
            given(consentRepository.save(any(Consent.class))).willReturn(consent);

            // when
            Consent result = authService.saveConsent(userId, location, push, consentedAt);

            // then
            assertThat(result).isNotNull();
            assertThat(result.isLocation()).isTrue();
            assertThat(result.isPush()).isTrue();
            assertThat(result.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("존재하지 않는 사용자도 동의 저장 성공 (사용자 존재 검증 없음)")
        void saveConsent_noUserExistenceCheck() {
            // given — 현재 구현은 userRepository.existsById를 호출하지 않으므로
            // 존재하지 않는 사용자 ID로도 동의 저장이 성공한다.
            String userId = "usr_notexist";
            LocalDateTime consentedAt = LocalDateTime.now();

            Consent consent = Consent.create(userId, true, true, consentedAt);
            ReflectionTestUtils.setField(consent, "id", "cns_new123");
            given(consentRepository.save(any(Consent.class))).willReturn(consent);

            // when
            Consent result = authService.saveConsent(userId, true, true, consentedAt);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            then(userRepository).should(never()).existsById(anyString());
        }
    }
}
