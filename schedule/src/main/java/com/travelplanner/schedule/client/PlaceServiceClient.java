package com.travelplanner.schedule.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.common.response.ApiResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PLCE 서비스 클라이언트.
 *
 * <p>장소 상세 정보(영업시간 포함)를 PLCE 서비스에서 조회한다.
 * Phase 1에서는 RestTemplate으로 localhost:8083 호출한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlaceServiceClient {

    private final RestTemplate restTemplate;

    @Value("${place-service.base-url:http://localhost:8083}")
    private String baseUrl;

    /**
     * 장소 상세 정보를 조회한다.
     *
     * @param placeId 장소 ID
     * @return 장소 상세 정보 (실패 시 기본값 반환)
     */
    public PlaceDetail getPlaceDetail(String placeId) {
        try {
            ResponseEntity<ApiResponse<PlaceDetail>> response = restTemplate.exchange(
                baseUrl + "/api/v1/places/" + placeId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<PlaceDetail>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
        } catch (RestClientException e) {
            log.warn("PLCE 서비스 호출 실패 - placeId: {}, 오류: {}", placeId, e.getMessage());
        }
        return PlaceDetail.unknown(placeId);
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaceDetail {
        @JsonProperty("place_id")
        private String placeId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("business_hours")
        private List<BusinessHour> businessHours = new ArrayList<>();

        @JsonProperty("timezone")
        private String timezone;

        @JsonProperty("coordinates")
        private Coordinates coordinates;

        public double getLat() {
            return coordinates != null ? coordinates.lat : 0;
        }

        public double getLng() {
            return coordinates != null ? coordinates.lng : 0;
        }

        public static PlaceDetail unknown(String placeId) {
            PlaceDetail detail = new PlaceDetail();
            detail.placeId = placeId;
            detail.name = "알 수 없는 장소";
            detail.businessHours = new ArrayList<>();
            detail.timezone = "Asia/Seoul";
            return detail;
        }
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Coordinates {
        private double lat;
        private double lng;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BusinessHour {
        @JsonProperty("day")
        private String day;

        @JsonProperty("open")
        private String open;

        @JsonProperty("close")
        private String close;

        public boolean isWithinHours(LocalTime time) {
            if (open == null || close == null) {
                return true;
            }
            try {
                LocalTime openTime = LocalTime.parse(open);
                LocalTime closeTime = LocalTime.parse(close);
                if (closeTime.isBefore(openTime)) {
                    return !time.isBefore(openTime) || !time.isAfter(closeTime);
                }
                return !time.isBefore(openTime) && !time.isAfter(closeTime);
            } catch (Exception e) {
                return true;
            }
        }
    }
}
