package com.travelplanner.monitor.client;

import com.travelplanner.monitor.domain.TravelTimeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Google Directions API 클라이언트.
 *
 * <p>이동시간과 거리를 조회한다. API 키가 없으면 Mock 데이터를 반환한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleDirectionsClient {

    private final RestTemplate restTemplate;

    @Value("${google.directions.api-key:}")
    private String apiKey;

    @Value("${google.directions.base-url:https://maps.googleapis.com/maps/api}")
    private String baseUrl;

    /**
     * 이동시간 데이터를 조회한다.
     *
     * @param originLat 출발지 위도
     * @param originLng 출발지 경도
     * @param destLat 목적지 위도
     * @param destLng 목적지 경도
     * @return 이동시간 데이터
     */
    public TravelTimeData getTravelTime(double originLat, double originLng,
                                        double destLat, double destLng) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Google Directions API 키 없음 - Mock 데이터 반환");
            return mockTravelTimeData(originLat, originLng, destLat, destLng);
        }

        try {
            String origin = originLat + "," + originLng;
            String destination = destLat + "," + destLng;

            String walkingUrl = baseUrl + "/directions/json?origin=" + origin
                + "&destination=" + destination + "&mode=walking&key=" + apiKey;

            Map<?, ?> walkingResponse =
                restTemplate.getForObject(walkingUrl, Map.class);

            int walkingMinutes = extractDuration(walkingResponse);
            int distanceM = extractDistance(walkingResponse);

            int transitMinutes = 0;
            double distanceKm = distanceM / 1000.0;
            if (distanceKm >= 0.5) {
                String transitUrl = baseUrl + "/directions/json?origin=" + origin
                    + "&destination=" + destination + "&mode=transit&key=" + apiKey;
                Map<?, ?> transitResponse =
                    restTemplate.getForObject(transitUrl, Map.class);
                transitMinutes = extractDuration(transitResponse);
            }

            return new TravelTimeData(walkingMinutes,
                distanceKm >= 0.5 ? transitMinutes : null, distanceM, false);

        } catch (RestClientException e) {
            log.warn("Google Directions API 호출 실패: {}", e.getMessage());
        }

        return TravelTimeData.unknown();
    }

    private TravelTimeData mockTravelTimeData(double originLat, double originLng,
                                               double destLat, double destLng) {
        double deltaLat = destLat - originLat;
        double deltaLng = destLng - originLng;
        double distanceM = Math.sqrt(deltaLat * deltaLat + deltaLng * deltaLng) * 111000;
        int walkingMinutes = (int) (distanceM / 80);
        Integer transitMinutes = distanceM >= 500 ? (int) (distanceM / 300) : null;
        return new TravelTimeData(walkingMinutes, transitMinutes, (int) distanceM, false);
    }

    @SuppressWarnings("unchecked")
    private int extractDuration(Map<?, ?> response) {
        try {
            List<?> routes = (List<?>) response.get("routes");
            if (routes != null && !routes.isEmpty()) {
                Map<?, ?> route = (Map<?, ?>) routes.get(0);
                List<?> legs = (List<?>) route.get("legs");
                if (legs != null && !legs.isEmpty()) {
                    Map<?, ?> leg = (Map<?, ?>) legs.get(0);
                    Map<?, ?> duration = (Map<?, ?>) leg.get("duration");
                    if (duration != null) {
                        return ((Number) duration.get("value")).intValue() / 60;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("duration 추출 실패: {}", e.getMessage());
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private int extractDistance(Map<?, ?> response) {
        try {
            List<?> routes = (List<?>) response.get("routes");
            if (routes != null && !routes.isEmpty()) {
                Map<?, ?> route = (Map<?, ?>) routes.get(0);
                List<?> legs = (List<?>) route.get("legs");
                if (legs != null && !legs.isEmpty()) {
                    Map<?, ?> leg = (Map<?, ?>) legs.get(0);
                    Map<?, ?> distance = (Map<?, ?>) leg.get("distance");
                    if (distance != null) {
                        return ((Number) distance.get("value")).intValue();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("distance 추출 실패: {}", e.getMessage());
        }
        return 0;
    }
}
