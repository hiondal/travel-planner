package com.travelplanner.briefing.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.briefing.dto.internal.MonitorData;
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

/**
 * MNTR 서비스 HTTP 클라이언트.
 *
 * <p>장소 최신 상태 정보를 조회한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorServiceClient {

    private final RestTemplate restTemplate;

    @Value("${monitor-service.base-url:http://localhost:8084}")
    private String baseUrl;

    /**
     * 장소의 최신 상태를 조회한다.
     *
     * <p>MNTR 서비스의 GET /api/v1/badges/{placeId}/detail 를 호출하고
     * ApiResponse 래퍼에서 data 를 추출하여 MonitorData 로 변환한다.</p>
     *
     * @param placeId 장소 ID
     * @return MonitorData (호출 실패 시 기본 상태 반환)
     */
    public MonitorData getLatestStatus(String placeId) {
        log.debug("MNTR 서비스 장소 상태 조회 요청: placeId={}", placeId);
        try {
            String url = baseUrl + "/api/v1/badges/" + placeId + "/detail";
            ResponseEntity<ApiResponse<StatusDetailDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<StatusDetailDto>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getData() != null) {
                MonitorData data = toMonitorData(response.getBody().getData());
                log.debug("MNTR 서비스 응답 수신: placeId={}, status={}", placeId, data.getOverallStatus());
                return data;
            }
        } catch (RestClientException e) {
            log.warn("MNTR 서비스 호출 실패, 기본 상태 사용: placeId={}, error={}", placeId, e.getMessage());
        }
        return buildDefaultMonitorData(placeId);
    }

    private MonitorData toMonitorData(StatusDetailDto dto) {
        MonitorData data = new MonitorData();
        data.setPlaceId(dto.getPlaceId());
        data.setPlaceName(dto.getPlaceName());
        data.setOverallStatus(dto.getOverallStatus());

        if (dto.getDetails() != null) {
            StatusDetailsDto details = dto.getDetails();

            if (details.getBusinessStatus() != null) {
                data.setBusinessStatus(details.getBusinessStatus().getValue());
            }
            if (details.getCongestion() != null) {
                data.setCongestion(details.getCongestion().getValue());
            }
            if (details.getWeather() != null) {
                data.setWeather(details.getWeather().getValue());
                data.setPrecipitationProb(details.getWeather().getPrecipitationProb());
            }
            if (details.getTravelTime() != null) {
                data.setWalkingMinutes(details.getTravelTime().getWalkingMinutes());
                data.setTransitMinutes(details.getTravelTime().getTransitMinutes());
                data.setDistanceM(details.getTravelTime().getDistanceM());
            }
        }
        return data;
    }

    private MonitorData buildDefaultMonitorData(String placeId) {
        MonitorData data = new MonitorData();
        data.setPlaceId(placeId);
        data.setBusinessStatus("영업 중");
        data.setCongestion("보통");
        data.setWeather("맑음");
        data.setPrecipitationProb(0);
        data.setWalkingMinutes(15);
        data.setTransitMinutes(null);
        data.setDistanceM(420);
        data.setOverallStatus("SAFE");
        return data;
    }

    // ===== MNTR StatusDetailResponse 역직렬화용 내부 DTO =====

    @Getter
    @Setter
    @NoArgsConstructor
    static class StatusDetailDto {
        @JsonProperty("place_id")
        private String placeId;

        @JsonProperty("place_name")
        private String placeName;

        @JsonProperty("overall_status")
        private String overallStatus;

        @JsonProperty("details")
        private StatusDetailsDto details;

        @JsonProperty("reason")
        private String reason;

        @JsonProperty("show_alternative_button")
        private boolean showAlternativeButton;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class StatusDetailsDto {
        @JsonProperty("business_status")
        private BusinessStatusItem businessStatus;

        @JsonProperty("congestion")
        private CongestionItem congestion;

        @JsonProperty("weather")
        private WeatherItem weather;

        @JsonProperty("travel_time")
        private TravelTimeItem travelTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class BusinessStatusItem {
        @JsonProperty("status")
        private String status;

        @JsonProperty("value")
        private String value;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class CongestionItem {
        @JsonProperty("status")
        private String status;

        @JsonProperty("value")
        private String value;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class WeatherItem {
        @JsonProperty("status")
        private String status;

        @JsonProperty("value")
        private String value;

        @JsonProperty("precipitation_prob")
        private int precipitationProb;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class TravelTimeItem {
        @JsonProperty("status")
        private String status;

        @JsonProperty("walking_minutes")
        private int walkingMinutes;

        @JsonProperty("transit_minutes")
        private Integer transitMinutes;

        @JsonProperty("distance_m")
        private int distanceM;
    }
}
