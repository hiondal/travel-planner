package com.travelplanner.payment.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AUTH 서비스 HTTP 클라이언트.
 *
 * <p>구독 티어 변경 후 기존 토큰을 무효화하고 새 토큰을 발급한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${auth-service.base-url:http://localhost:8081}")
    private String baseUrl;

    /**
     * 기존 토큰을 무효화하고 새 구독 등급이 반영된 토큰을 발급한다.
     *
     * @param userId  사용자 ID
     * @param newTier 새 구독 등급
     * @return 새 Access Token (Mock)
     */
    public String invalidateAndReissueToken(String userId, String newTier) {
        log.info("AUTH 서비스 토큰 재발급 요청: userId={}, newTier={}", userId, newTier);
        // Phase 1: Mock 구현
        try {
            String url = baseUrl + "/api/v1/auth/token/invalidate";
            Map<String, String> request = Map.of("user_id", userId, "new_tier", newTier);
            Map result = restTemplate.postForObject(url, request, Map.class);
            if (result != null && result.containsKey("access_token")) {
                return (String) result.get("access_token");
            }
        } catch (RestClientException e) {
            log.warn("AUTH 서비스 호출 실패, Mock 토큰 반환: userId={}, error={}", userId, e.getMessage());
        }
        return "mock_access_token_" + userId + "_" + newTier;
    }
}
