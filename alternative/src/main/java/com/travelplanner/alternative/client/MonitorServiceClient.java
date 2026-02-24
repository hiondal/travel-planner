package com.travelplanner.alternative.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.alternative.domain.StatusBadge;
import com.travelplanner.common.response.ApiResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MNTR 서비스 HTTP 클라이언트.
 *
 * <p>장소 상태 배지를 일괄 조회한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorServiceClient {

    private final RestTemplate restTemplate;

    @Value("${monitor-service.base-url:http://localhost:8084}")
    private String baseUrl;

    /**
     * 여러 장소의 상태 배지를 조회한다.
     *
     * <p>MNTR 서비스의 GET /api/v1/badges?place_ids={placeIds} 를 호출하고
     * ApiResponse 래퍼에서 data 를 추출하여 장소 ID → StatusBadge 맵으로 변환한다.</p>
     *
     * @param placeIds 장소 ID 목록
     * @return 장소 ID → 상태 배지 맵 (호출 실패 시 GREEN 배지 기본값 반환)
     */
    public Map<String, StatusBadge> getBadges(List<String> placeIds) {
        log.debug("MNTR 서비스 배지 일괄 조회: placeIds={}", placeIds);
        Map<String, StatusBadge> result = new HashMap<>();
        try {
            String placeIdsParam = String.join(",", placeIds);
            String url = baseUrl + "/api/v1/badges?place_ids=" + placeIdsParam;
            ResponseEntity<ApiResponse<BadgeListResponseDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<BadgeListResponseDto>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getData() != null
                    && response.getBody().getData().getBadges() != null) {
                for (BadgeItemDto item : response.getBody().getData().getBadges()) {
                    StatusBadge badge = new StatusBadge();
                    badge.setPlaceId(item.getPlaceId());
                    badge.setStatus(item.getStatus());
                    badge.setIcon(item.getIcon());
                    result.put(item.getPlaceId(), badge);
                }
                return result;
            }
        } catch (RestClientException e) {
            log.warn("MNTR 서비스 호출 실패, GREEN 배지 기본값 사용: error={}", e.getMessage());
        }
        // 호출 실패 시 모든 장소에 GREEN 배지 반환
        for (String placeId : placeIds) {
            StatusBadge badge = new StatusBadge();
            badge.setPlaceId(placeId);
            badge.setStatus("GREEN");
            badge.setIcon("CHECK");
            result.put(placeId, badge);
        }
        return result;
    }

    // ===== MNTR BadgeListResponse 역직렬화용 내부 DTO =====

    @Getter
    @Setter
    @NoArgsConstructor
    static class BadgeListResponseDto {
        @JsonProperty("badges")
        private List<BadgeItemDto> badges;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class BadgeItemDto {
        @JsonProperty("place_id")
        private String placeId;

        @JsonProperty("status")
        private String status;

        @JsonProperty("icon")
        private String icon;

        @JsonProperty("label")
        private String label;

        @JsonProperty("color_hex")
        private String colorHex;
    }
}
