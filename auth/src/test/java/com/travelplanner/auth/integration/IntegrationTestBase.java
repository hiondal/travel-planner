package com.travelplanner.auth.integration;

import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.common.security.JwtProvider;
import com.travelplanner.common.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

/**
 * AUTH 서비스 통합 테스트 베이스 클래스.
 *
 * <p>Spring Boot 전체 컨텍스트를 랜덤 포트로 기동하여
 * 실제 HTTP 요청/응답 사이클을 검증한다.</p>
 *
 * @author 조현아/가디언
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JwtProvider jwtProvider;

    /**
     * 테스트용 JWT Access Token을 생성한다.
     *
     * @param userId 사용자 ID
     * @param tier   구독 등급
     * @return JWT Access Token 문자열
     */
    protected String generateTestToken(String userId, SubscriptionTier tier) {
        UserPrincipal principal = new UserPrincipal(userId, userId + "@test.com", tier);
        return jwtProvider.generateAccessToken(principal);
    }

    /**
     * 테스트용 FREE 티어 JWT Access Token을 생성한다.
     *
     * @param userId 사용자 ID
     * @return JWT Access Token 문자열
     */
    protected String generateTestToken(String userId) {
        return generateTestToken(userId, SubscriptionTier.FREE);
    }

    /**
     * Bearer 인증 헤더를 생성한다.
     *
     * @param token JWT Access Token
     * @return Authorization 헤더가 포함된 HttpHeaders
     */
    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    /**
     * FREE 티어 사용자의 인증 헤더를 생성한다.
     *
     * @param userId 사용자 ID
     * @return Authorization 헤더가 포함된 HttpHeaders
     */
    protected HttpHeaders authHeadersFor(String userId) {
        return authHeaders(generateTestToken(userId));
    }

    /**
     * 특정 티어 사용자의 인증 헤더를 생성한다.
     *
     * @param userId 사용자 ID
     * @param tier   구독 등급
     * @return Authorization 헤더가 포함된 HttpHeaders
     */
    protected HttpHeaders authHeadersFor(String userId, SubscriptionTier tier) {
        return authHeaders(generateTestToken(userId, tier));
    }
}
