package com.travelplanner.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplanner.auth.config.SecurityConfig;
import com.travelplanner.auth.config.jwt.JwtAuthenticationEntryPoint;
import com.travelplanner.auth.config.jwt.JwtAuthenticationFilter;
import com.travelplanner.auth.domain.Consent;
import com.travelplanner.auth.domain.User;
import com.travelplanner.auth.dto.internal.SocialLoginResult;
import com.travelplanner.auth.dto.internal.TokenInvalidateResult;
import com.travelplanner.auth.dto.internal.TokenRefreshResult;
import com.travelplanner.auth.repository.AuthSessionRedisRepository;
import com.travelplanner.auth.service.AuthService;
import com.travelplanner.common.enums.OAuthProvider;
import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.common.security.JwtProvider;
import com.travelplanner.common.security.JwtToken;
import com.travelplanner.common.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 슬라이스 테스트.
 *
 * <p>Spring MVC 레이어만 로드하여 HTTP 요청/응답을 검증한다.
 * SecurityConfig를 직접 Import하여 실제 보안 정책을 사용한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@DisplayName("AuthController 슬라이스 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtProvider jwtProvider;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private AuthSessionRedisRepository authSessionRedisRepository;

    // ===== 소셜 로그인 테스트 =====

    @Nested
    @DisplayName("POST /api/v1/auth/social-login")
    class SocialLoginEndpointTest {

        @Test
        @DisplayName("Google 로그인 성공 시 200과 JWT 반환")
        void socialLogin_google_success() throws Exception {
            // given
            User user = User.create(OAuthProvider.GOOGLE, "sub_123",
                "user@gmail.com", "테스트유저", "https://avatar.com/pic.jpg");
            ReflectionTestUtils.setField(user, "id", "usr_test123");

            JwtToken jwtToken = new JwtToken("access.token", "refresh.token", 1800);
            SocialLoginResult result = new SocialLoginResult(jwtToken, user, true);

            given(authService.socialLogin(eq("google"), anyString())).willReturn(result);
            given(jwtProvider.validateToken(any())).willReturn(false);

            Map<String, String> requestBody = Map.of(
                "provider", "google",
                "oauth_code", "4/0AX4XfWh..."
            );

            // when & then
            mockMvc.perform(post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.access_token").value("access.token"))
                .andExpect(jsonPath("$.data.refresh_token").value("refresh.token"))
                .andExpect(jsonPath("$.data.user_profile.is_new_user").value(true));
        }

        @Test
        @DisplayName("provider 없이 요청 시 400 반환")
        void socialLogin_missingProvider_returns400() throws Exception {
            // given
            Map<String, String> requestBody = Map.of("oauth_code", "some_code");
            given(jwtProvider.validateToken(any())).willReturn(false);

            // when & then
            mockMvc.perform(post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .with(csrf()))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("oauth_code 없이 요청 시 400 반환")
        void socialLogin_missingOauthCode_returns400() throws Exception {
            // given
            Map<String, String> requestBody = Map.of("provider", "google");
            given(jwtProvider.validateToken(any())).willReturn(false);

            // when & then
            mockMvc.perform(post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .with(csrf()))
                .andExpect(status().isBadRequest());
        }
    }

    // ===== 토큰 갱신 테스트 =====

    @Nested
    @DisplayName("POST /api/v1/auth/token/refresh")
    class TokenRefreshEndpointTest {

        @Test
        @DisplayName("유효한 Refresh Token으로 Access Token 갱신 성공")
        void refreshToken_success() throws Exception {
            // given
            TokenRefreshResult result = new TokenRefreshResult("new.access.token", 1800);
            given(authService.refreshToken(anyString())).willReturn(result);
            given(jwtProvider.validateToken(any())).willReturn(false);

            Map<String, String> requestBody = Map.of("refresh_token", "valid.refresh.token");

            // when & then
            mockMvc.perform(post("/api/v1/auth/token/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").value("new.access.token"))
                .andExpect(jsonPath("$.data.expires_in").value(1800));
        }
    }

    // ===== 로그아웃 테스트 =====

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutEndpointTest {

        @Test
        @DisplayName("인증된 사용자 로그아웃 시 204 반환")
        void logout_authenticated_returns204() throws Exception {
            // given
            UserPrincipal principal = new UserPrincipal("usr_logout", "user@gmail.com", SubscriptionTier.FREE);
            willDoNothing().given(authService).logout(anyString(), anyString());
            given(jwtProvider.validateToken(any())).willReturn(false);

            Map<String, String> requestBody = Map.of("refresh_token", "some.refresh.token");

            // when & then
            mockMvc.perform(post("/api/v1/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        new UsernamePasswordAuthenticationToken(
                            principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                    ))
                    .with(csrf()))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("비인증 요청으로 로그아웃 시 401 반환")
        void logout_unauthenticated_returns401() throws Exception {
            // given
            Map<String, String> requestBody = Map.of("refresh_token", "some.token");
            given(jwtProvider.validateToken(any())).willReturn(false);

            // when & then
            mockMvc.perform(post("/api/v1/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .with(csrf()))
                .andExpect(status().isUnauthorized());
        }
    }

    // ===== 동의 저장 테스트 =====

    @Nested
    @DisplayName("POST /api/v1/users/consent")
    class ConsentEndpointTest {

        @Test
        @DisplayName("인증된 사용자 동의 저장 시 201 반환")
        void saveConsent_success() throws Exception {
            // given
            UserPrincipal principal = new UserPrincipal("usr_consent", "user@gmail.com", SubscriptionTier.FREE);
            Consent consent = Consent.create("usr_consent", true, true, LocalDateTime.now());
            ReflectionTestUtils.setField(consent, "id", "cns_test123");

            given(authService.saveConsent(anyString(), anyBoolean(), anyBoolean(), any(LocalDateTime.class)))
                .willReturn(consent);
            given(jwtProvider.validateToken(any())).willReturn(false);

            Map<String, Object> requestBody = Map.of(
                "location", true,
                "push", true,
                "timestamp", "2026-03-15T09:00:00"
            );

            // when & then
            mockMvc.perform(post("/api/v1/users/consent")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        new UsernamePasswordAuthenticationToken(
                            principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                    ))
                    .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.consent_id").value("cns_test123"))
                .andExpect(jsonPath("$.data.location").value(true))
                .andExpect(jsonPath("$.data.push").value(true));
        }
    }
}
