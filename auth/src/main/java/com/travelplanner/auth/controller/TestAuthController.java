package com.travelplanner.auth.controller;

import com.travelplanner.auth.domain.User;
import com.travelplanner.auth.repository.UserRepository;
import com.travelplanner.common.security.JwtProvider;
import com.travelplanner.common.security.JwtToken;
import com.travelplanner.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 테스트 전용 인증 컨트롤러. dev 환경에서만 활성화된다.
 *
 * <p>OAuth 소셜 로그인 없이 seed.sql에 등록된 테스트 사용자로 JWT 토큰을 발급한다.</p>
 */
@Profile("dev")
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestAuthController {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /**
     * 테스트용 토큰 발급.
     * POST /api/v1/test/login
     *
     * @param body {"userId": "usr_testfree000001"} 또는 {"userId": "usr_testtrippass001"}
     * @return accessToken, refreshToken
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> testLogin(@RequestBody Map<String, String> body) {
        // userId (camelCase) 또는 user_id (snake_case) 모두 허용
        String rawId = body.get("userId");
        if (rawId == null || rawId.isBlank()) {
            rawId = body.get("user_id");
        }
        if (rawId == null || rawId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "userId is required"));
        }
        final String userId = rawId;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("테스트 사용자를 찾을 수 없습니다: " + userId));

        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), user.getTier());
        JwtToken token = jwtProvider.generateToken(principal);

        return ResponseEntity.ok(Map.of(
                "accessToken", token.getAccessToken(),
                "refreshToken", token.getRefreshToken(),
                "userId", user.getId(),
                "tier", user.getTier().name()
        ));
    }
}
