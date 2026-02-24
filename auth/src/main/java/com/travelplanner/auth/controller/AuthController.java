package com.travelplanner.auth.controller;

import com.travelplanner.auth.domain.Consent;
import com.travelplanner.auth.dto.internal.SocialLoginResult;
import com.travelplanner.auth.dto.internal.TokenInvalidateResult;
import com.travelplanner.auth.dto.internal.TokenRefreshResult;
import com.travelplanner.auth.dto.request.*;
import com.travelplanner.auth.dto.response.*;
import com.travelplanner.auth.service.AuthService;
import com.travelplanner.common.response.ApiResponse;
import com.travelplanner.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 REST API 컨트롤러.
 *
 * <p>소셜 로그인, JWT 토큰 관리, 사용자 동의 처리 엔드포인트를 제공한다.</p>
 *
 * <p>엔드포인트 목록:</p>
 * <ul>
 *   <li>POST /api/v1/auth/social-login — Google OAuth 로그인</li>
 *   <li>POST /api/v1/auth/token/refresh — Access Token 갱신</li>
 *   <li>POST /api/v1/auth/logout — 로그아웃</li>
 *   <li>POST /api/v1/auth/token/invalidate — 토큰 즉시 무효화 (PAY 서비스 내부 호출)</li>
 *   <li>POST /api/v1/users/consent — 사용자 동의 저장</li>
 * </ul>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Tag(name = "Auth", description = "소셜 로그인 및 JWT 토큰 관리")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Google OAuth 소셜 로그인을 처리하고 JWT 토큰을 발급한다.
     *
     * <p>최초 로그인 시 사용자 프로파일이 자동 생성되며,
     * {@code is_new_user} 플래그로 온보딩 화면 분기를 처리한다.</p>
     *
     * @param request 소셜 로그인 요청 (provider, oauth_code)
     * @return 200 OK — Access Token, Refresh Token, 사용자 프로파일
     */
    @Operation(summary = "소셜 로그인 (Google OAuth)", description = "Google OAuth 코드를 검증하고 JWT 토큰을 발급한다.")
    @PostMapping("/auth/social-login")
    public ResponseEntity<ApiResponse<SocialLoginResponse>> socialLogin(
            @Valid @RequestBody SocialLoginRequest request) {
        SocialLoginResult result = authService.socialLogin(request.getProvider(), request.getOauthCode());
        SocialLoginResponse response = SocialLoginResponse.of(result.getJwtToken(), result.getUser(), result.isNewUser());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Refresh Token으로 새 Access Token을 발급한다.
     *
     * @param request 토큰 갱신 요청 (refresh_token)
     * @return 200 OK — 새 Access Token과 만료 시간
     */
    @Operation(summary = "Access Token 갱신", description = "Refresh Token으로 새 Access Token을 발급한다.")
    @PostMapping("/auth/token/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResult result = authService.refreshToken(request.getRefreshToken());
        TokenRefreshResponse response = TokenRefreshResponse.of(result.getAccessToken(), result.getExpiresIn());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 로그아웃을 처리한다.
     *
     * <p>Refresh Token을 무효화하고 Redis 세션을 삭제한다.</p>
     *
     * @param request   로그아웃 요청 (refresh_token)
     * @param principal 인증된 사용자 정보
     * @return 204 No Content
     */
    @Operation(summary = "로그아웃", security = @SecurityRequirement(name = "BearerAuth"),
               description = "Refresh Token을 무효화하고 세션을 종료한다.")
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(request.getRefreshToken(), principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 구독 티어 변경 시 기존 토큰을 즉시 무효화하고 새 토큰을 발급한다.
     *
     * <p>PAY 서비스에서 구독 완료 후 내부 호출한다.</p>
     *
     * @param request   토큰 무효화 요청 (user_id, new_tier)
     * @param principal 인증된 사용자 정보
     * @return 200 OK — 새 Access Token과 구독 등급
     */
    @Operation(summary = "토큰 즉시 무효화 (구독 티어 변경)", security = @SecurityRequirement(name = "BearerAuth"),
               description = "PAY 서비스에서 구독 티어 변경 후 내부 호출. 기존 토큰 무효화 후 새 토큰 발급.")
    @PostMapping("/auth/token/invalidate")
    public ResponseEntity<ApiResponse<TokenInvalidateResponse>> invalidateAndReissueToken(
            @Valid @RequestBody TokenInvalidateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TokenInvalidateResult result = authService.invalidateAndReissueToken(
            request.getUserId(), request.getNewTier()
        );
        TokenInvalidateResponse response = TokenInvalidateResponse.of(result.getAccessToken(), result.getTier());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 사용자 동의 이력을 저장한다.
     *
     * <p>위치정보 수집 및 Push 알림 권한 동의 결과를 서버에 저장한다.
     * 동의 처리는 클라이언트에서 수행하고 결과만 전송한다.</p>
     *
     * @param request   동의 요청 (location, push, timestamp)
     * @param principal 인증된 사용자 정보
     * @return 201 Created — 저장된 동의 정보
     */
    @Operation(summary = "사용자 동의 이력 저장", security = @SecurityRequirement(name = "BearerAuth"),
               description = "위치정보 수집 및 Push 알림 권한 동의 결과를 서버에 저장한다.")
    @PostMapping("/users/consent")
    public ResponseEntity<ApiResponse<ConsentResponse>> saveConsent(
            @Valid @RequestBody ConsentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Consent consent = authService.saveConsent(
            principal.getUserId(),
            request.getLocation(),
            request.getPush(),
            request.getTimestamp()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(ConsentResponse.from(consent)));
    }
}
