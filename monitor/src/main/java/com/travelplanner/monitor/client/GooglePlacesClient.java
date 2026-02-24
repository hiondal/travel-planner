package com.travelplanner.monitor.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.monitor.domain.BusinessStatusData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Google Places API 클라이언트.
 *
 * <p>장소의 영업 상태와 혼잡도를 조회한다.
 * API 키가 없으면 Mock 데이터를 반환한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesClient {

    private final RestTemplate restTemplate;

    @Value("${google.places.api-key:}")
    private String apiKey;

    @Value("${google.places.base-url:https://maps.googleapis.com/maps/api}")
    private String baseUrl;

    /**
     * 영업 상태를 조회한다.
     *
     * @param placeId Google Places ID
     * @return 영업 상태 데이터
     */
    public BusinessStatusData getBusinessStatus(String placeId) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Google Places API 키 없음 - Mock 데이터 반환 (placeId={})", placeId);
            return new BusinessStatusData("OPEN", false);
        }

        try {
            String url = baseUrl + "/place/details/json?place_id=" + placeId
                + "&fields=business_status&key=" + apiKey;
            PlaceDetailsResponse response =
                restTemplate.getForObject(url, PlaceDetailsResponse.class);

            if (response != null && response.getResult() != null) {
                String status = response.getResult().getBusinessStatus();
                return new BusinessStatusData(
                    status != null ? status : "UNKNOWN", false);
            }
        } catch (RestClientException e) {
            log.warn("Google Places API 호출 실패 (placeId={}): {}", placeId, e.getMessage());
        }

        return BusinessStatusData.unknown();
    }

    /**
     * 현재 혼잡도를 조회한다.
     *
     * <p>Phase 1에서는 규칙 기반 Mock 데이터를 반환한다.</p>
     *
     * @param placeId Google Places ID
     * @return 혼잡도 (0~100), null이면 미확인
     */
    public Integer getCurrentPopularity(String placeId) {
        log.debug("혼잡도 Phase 1 Mock 반환 (placeId={})", placeId);
        return null;
    }

    @Getter
    private static class PlaceDetailsResponse {
        private PlaceResult result;

        @Getter
        private static class PlaceResult {
            @JsonProperty("business_status")
            private String businessStatus;
        }
    }
}
