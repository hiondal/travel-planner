package com.travelplanner.briefing.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.briefing.dto.internal.SubscriptionInfo;
import com.travelplanner.common.response.ApiResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * PAY 서비스 HTTP 클라이언트.
 *
 * <p>사용자 구독 등급 및 브리핑 횟수를 조회한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayServiceClient {

    private final RestTemplate restTemplate;

    @Value("${payment-service.base-url:http://localhost:8087}")
    private String baseUrl;

    @Value("${internal.service-key:}")
    private String internalServiceKey;

    /**
     * 사용자 구독 정보를 조회한다.
     *
     * <p>PAY 서비스의 GET /api/v1/subscriptions/status?userId={userId} 를 호출하고
     * ApiResponse 래퍼에서 data 를 추출하여 SubscriptionInfo 로 변환한다.</p>
     *
     * @param userId 사용자 ID
     * @return SubscriptionInfo (호출 실패 시 FREE 티어 기본값 반환)
     */
    public SubscriptionInfo getSubscriptionInfo(String userId) {
        log.debug("PAY 서비스 구독 정보 조회 요청: userId={}", userId);
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/api/v1/subscriptions/status")
                    .queryParam("userId", userId)
                    .toUriString();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Service-Key", internalServiceKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<ApiResponse<SubscriptionStatusDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ApiResponse<SubscriptionStatusDto>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getData() != null) {
                SubscriptionStatusDto dto = response.getBody().getData();
                SubscriptionInfo info = new SubscriptionInfo();
                info.setTier(dto.getTier());
                info.setTodayBriefingCount(0);
                log.debug("PAY 서비스 응답 수신: userId={}, tier={}", userId, info.getTier());
                return info;
            }
        } catch (RestClientException e) {
            log.warn("PAY 서비스 호출 실패, 기본 구독 정보 사용: userId={}, error={}", userId, e.getMessage());
        }
        return buildDefaultSubscriptionInfo();
    }

    private SubscriptionInfo buildDefaultSubscriptionInfo() {
        SubscriptionInfo info = new SubscriptionInfo();
        info.setTier("FREE");
        info.setTodayBriefingCount(0);
        return info;
    }

    // ===== PAY SubscriptionStatusResponse 역직렬화용 내부 DTO =====

    @Getter
    @Setter
    @NoArgsConstructor
    static class SubscriptionStatusDto {
        @JsonProperty("tier")
        private String tier;

        @JsonProperty("status")
        private String status;

        @JsonProperty("subscription_id")
        private String subscriptionId;
    }
}
