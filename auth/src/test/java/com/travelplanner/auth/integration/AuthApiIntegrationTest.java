package com.travelplanner.auth.integration;

import com.travelplanner.auth.client.OAuthClient;
import com.travelplanner.auth.client.OAuthProfile;
import com.travelplanner.auth.domain.User;
import com.travelplanner.auth.repository.AuthSessionRedisRepository;
import com.travelplanner.auth.repository.ConsentRepository;
import com.travelplanner.auth.repository.RefreshTokenRepository;
import com.travelplanner.auth.repository.UserRepository;
import com.travelplanner.common.enums.OAuthProvider;
import com.travelplanner.common.enums.SubscriptionTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;

/**
 * AUTH 서비스 통합 테스트.
 *
 * <p>소셜 로그인, 토큰 관리, 사용자 동의 API 엔드포인트에 대해
 * 실제 HTTP 요청/응답 사이클과 DB 연동을 검증한다.</p>
 *
 * <p>외부 의존성 처리:</p>
 * <ul>
 *   <li>OAuthClient (Google/Apple) — @MockBean으로 대체</li>
 *   <li>AuthSessionRedisRepository — @MockBean으로 대체 (Redis 미기동 환경 대응)</li>
 * </ul>
 *
 * @author 조현아/가디언
 * @since 1.0.0
 */
@DisplayName("AUTH API 통합 테스트")
class AuthApiIntegrationTest extends IntegrationTestBase {

    @MockBean
    private OAuthClient oAuthClient;

    @MockBean
    private AuthSessionRedisRepository authSessionRedisRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        // Redis Mock 기본 동작 설정
        willDoNothing().given(authSessionRedisRepository).saveSession(anyString(), anyString(), anyString(), anyString());
        willDoNothing().given(authSessionRedisRepository).deleteSession(anyString());
        willDoNothing().given(authSessionRedisRepository).deleteSessionByUserId(anyString(), anyString());
        given(authSessionRedisRepository.isBlacklisted(anyString())).willReturn(false);
        given(authSessionRedisRepository.findUserIdByRefreshToken(anyString())).willReturn(Optional.empty());
        given(authSessionRedisRepository.findRefreshTokenByUserId(anyString())).willReturn(Optional.empty());
    }

    // ===== IT-AUTH-001: 소셜 로그인 =====

    @Nested
    @DisplayName("POST /api/v1/auth/social-login")
    class SocialLoginIntegrationTest {

        @Test
        @DisplayName("IT-AUTH-001: Google 소셜 로그인 성공 시 200과 JWT 토큰 반환")
        void givenValidGoogleOAuthCode_whenSocialLogin_thenReturns200WithTokens() {
            // given
            given(oAuthClient.verify(anyString(), anyString()))
                .willReturn(new OAuthProfile(
                    "google_sub_new_001", "newuser@gmail.com",
                    "신규유저", "https://avatar.test/new.jpg",
                    OAuthProvider.GOOGLE
                ));

            Map<String, String> request = Map.of(
                "provider", "google",
                "oauth_code", "mock_google_oauth_code"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/social-login",
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("success")).isEqualTo(true);
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            assertThat(data).containsKey("access_token");
            assertThat(data).containsKey("refresh_token");
            assertThat(data).containsKey("user_profile");
            Map<String, Object> profile = (Map<String, Object>) data.get("user_profile");
            assertThat(profile.get("is_new_user")).isEqualTo(true);
        }

        @Test
        @DisplayName("IT-AUTH-002: 기존 사용자 재로그인 시 is_new_user=false 반환")
        void givenExistingUser_whenSocialLogin_thenReturnsIsNewUserFalse() {
            // given - DB에 기존 사용자 저장
            User existingUser = User.create(OAuthProvider.GOOGLE, "google_sub_existing",
                "existing@gmail.com", "기존유저", "https://avatar.test/existing.jpg");
            userRepository.save(existingUser);

            given(oAuthClient.verify(anyString(), anyString()))
                .willReturn(new OAuthProfile(
                    "google_sub_existing", "existing@gmail.com",
                    "기존유저", "https://avatar.test/existing.jpg",
                    OAuthProvider.GOOGLE
                ));

            Map<String, String> request = Map.of(
                "provider", "google",
                "oauth_code", "mock_google_oauth_code"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/social-login",
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            Map<String, Object> profile = (Map<String, Object>) data.get("user_profile");
            assertThat(profile.get("is_new_user")).isEqualTo(false);
        }

        @Test
        @DisplayName("IT-AUTH-003: provider 필드 누락 시 400 반환")
        void givenMissingProvider_whenSocialLogin_thenReturns400() {
            // given
            Map<String, String> request = Map.of("oauth_code", "some_code");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/social-login",
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-AUTH-004: oauth_code 필드 누락 시 400 반환")
        void givenMissingOauthCode_whenSocialLogin_thenReturns400() {
            // given
            Map<String, String> request = Map.of("provider", "google");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/social-login",
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ===== IT-AUTH-006 ~ 008: 토큰 갱신 =====

    @Nested
    @DisplayName("POST /api/v1/auth/token/refresh")
    class TokenRefreshIntegrationTest {

        @Test
        @DisplayName("IT-AUTH-006: 유효한 Refresh Token으로 Access Token 갱신 성공")
        void givenValidRefreshToken_whenRefreshToken_thenReturns200WithNewAccessToken() {
            // given - DB에 사용자와 Refresh Token 저장
            User user = User.create(OAuthProvider.GOOGLE, "google_sub_refresh",
                "refresh@gmail.com", "갱신유저", "https://avatar.test/refresh.jpg");
            userRepository.save(user);

            // Refresh Token을 실제로 생성해 DB에 저장
            String refreshToken = jwtProvider.generateRefreshToken(user.getId());
            com.travelplanner.auth.domain.RefreshToken rt =
                com.travelplanner.auth.domain.RefreshToken.create(
                    user.getId(), refreshToken,
                    java.time.LocalDateTime.now().plusDays(30)
                );
            refreshTokenRepository.save(rt);

            Map<String, String> request = Map.of("refresh_token", refreshToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/token/refresh",
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("access_token");
            assertThat(data.get("expires_in")).isNotNull();
        }

        @Test
        @DisplayName("IT-AUTH-007: refresh_token 필드 누락 시 400 반환")
        void givenMissingRefreshToken_whenRefreshToken_thenReturns400() {
            // given
            Map<String, String> request = Map.of();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/token/refresh",
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ===== IT-AUTH-009 ~ 010: 로그아웃 =====

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutIntegrationTest {

        @Test
        @DisplayName("IT-AUTH-009: 인증된 사용자 로그아웃 시 204 반환")
        void givenAuthenticatedUser_whenLogout_thenReturns204() {
            // given
            String token = generateTestToken("usr_test001");
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Map.of("refresh_token", "some.refresh.token");

            // when
            ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Void.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("IT-AUTH-010: 인증 토큰 없이 로그아웃 시 401 반환")
        void givenNoToken_whenLogout_thenReturns401() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> request = Map.of("refresh_token", "some.refresh.token");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== IT-AUTH-013 ~ 016: 동의 저장 =====

    @Nested
    @DisplayName("POST /api/v1/users/consent")
    class ConsentIntegrationTest {

        @Test
        @DisplayName("IT-AUTH-013: 인증된 사용자 위치/푸시 동의 저장 시 201 반환")
        void givenAuthenticatedUser_whenSaveConsent_thenReturns201() {
            // given
            String token = generateTestToken("usr_test001");
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "location", true,
                "push", true,
                "timestamp", "2026-03-15T09:00:00"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/users/consent",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("consent_id");
            assertThat(data.get("location")).isEqualTo(true);
            assertThat(data.get("push")).isEqualTo(true);
        }

        @Test
        @DisplayName("IT-AUTH-014: 인증 없이 동의 저장 시 401 반환")
        void givenNoToken_whenSaveConsent_thenReturns401() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> request = Map.of(
                "location", true,
                "push", true,
                "timestamp", "2026-03-15T09:00:00"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/users/consent",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("IT-AUTH-015: timestamp 필드 누락 시 400 반환")
        void givenMissingTimestamp_whenSaveConsent_thenReturns400() {
            // given
            String token = generateTestToken("usr_test001");
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "location", true,
                "push", true
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/users/consent",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
