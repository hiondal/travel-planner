package com.travelplanner.place.integration;

import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.common.security.JwtProvider;
import com.travelplanner.common.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

/**
 * PLACE 서비스 통합 테스트 베이스 클래스.
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

    protected String generateTestToken(String userId, SubscriptionTier tier) {
        UserPrincipal principal = new UserPrincipal(userId, userId + "@test.com", tier);
        return jwtProvider.generateAccessToken(principal);
    }

    protected String generateTestToken(String userId) {
        return generateTestToken(userId, SubscriptionTier.FREE);
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    protected HttpHeaders authHeadersFor(String userId) {
        return authHeaders(generateTestToken(userId));
    }
}
