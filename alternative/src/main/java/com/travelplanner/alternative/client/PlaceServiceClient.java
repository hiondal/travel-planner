package com.travelplanner.alternative.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.alternative.domain.PlaceCandidate;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PLCE 서비스 HTTP 클라이언트.
 *
 * <p>주변 장소를 검색한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceServiceClient {

    private final RestTemplate restTemplate;

    @Value("${place-service.base-url:http://localhost:8083}")
    private String baseUrl;

    /**
     * 주변 장소를 검색한다.
     *
     * <p>PLCE 서비스의 GET /api/v1/places/nearby 를 호출하고 ApiResponse 래퍼에서
     * data 를 추출하여 PlaceCandidate 목록으로 변환한다.</p>
     *
     * @param lat      위도
     * @param lng      경도
     * @param category 카테고리
     * @param radiusM  검색 반경 (미터)
     * @return 주변 장소 후보 목록 (호출 실패 시 Mock 데이터 반환)
     */
    public List<PlaceCandidate> searchNearby(double lat, double lng, String category, int radiusM) {
        log.debug("PLCE 서비스 주변 장소 검색: lat={}, lng={}, category={}, radius={}", lat, lng, category, radiusM);
        try {
            String url = String.format("%s/api/v1/places/nearby?lat=%f&lng=%f&category=%s&radius=%d",
                    baseUrl, lat, lng, category, radiusM);
            ResponseEntity<ApiResponse<NearbyPlaceSearchResponseDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<NearbyPlaceSearchResponseDto>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getData() != null
                    && response.getBody().getData().getPlaces() != null
                    && !response.getBody().getData().getPlaces().isEmpty()) {
                return response.getBody().getData().getPlaces().stream()
                    .map(dto -> {
                        PlaceCandidate candidate = new PlaceCandidate();
                        candidate.setPlaceId(dto.getPlaceId());
                        candidate.setName(dto.getName());
                        candidate.setDistanceM(dto.getDistanceM());
                        candidate.setRating(dto.getRating() != null ? dto.getRating() : 0f);
                        candidate.setCategory(dto.getCategory());
                        candidate.setOpen(dto.isOpen());
                        if (dto.getCoordinates() != null) {
                            candidate.setLat(dto.getCoordinates().getLat());
                            candidate.setLng(dto.getCoordinates().getLng());
                        }
                        return candidate;
                    })
                    .collect(Collectors.toList());
            }
        } catch (RestClientException e) {
            log.warn("PLCE 서비스 호출 실패, Mock 데이터 사용: error={}", e.getMessage());
        }
        return buildMockCandidates(lat, lng, category);
    }

    private List<PlaceCandidate> buildMockCandidates(double lat, double lng, String category) {
        List<PlaceCandidate> candidates = new ArrayList<>();

        PlaceCandidate c1 = new PlaceCandidate();
        c1.setPlaceId("mock_place_001");
        c1.setName("Mock " + category + " 1호점");
        c1.setDistanceM(320);
        c1.setRating(4.0f);
        c1.setLat(lat + 0.002);
        c1.setLng(lng - 0.002);
        c1.setCategory(category);
        c1.setOpen(true);
        c1.setCongestion("낮음");
        c1.setWalkingMinutes(5);

        PlaceCandidate c2 = new PlaceCandidate();
        c2.setPlaceId("mock_place_002");
        c2.setName("Mock " + category + " 2호점");
        c2.setDistanceM(650);
        c2.setRating(4.3f);
        c2.setLat(lat - 0.003);
        c2.setLng(lng + 0.003);
        c2.setCategory(category);
        c2.setOpen(true);
        c2.setCongestion("보통");
        c2.setWalkingMinutes(9);
        c2.setTransitMinutes(4);

        PlaceCandidate c3 = new PlaceCandidate();
        c3.setPlaceId("mock_place_003");
        c3.setName("Mock " + category + " 3호점");
        c3.setDistanceM(820);
        c3.setRating(3.8f);
        c3.setLat(lat + 0.005);
        c3.setLng(lng + 0.005);
        c3.setCategory(category);
        c3.setOpen(true);
        c3.setCongestion("낮음");
        c3.setWalkingMinutes(12);
        c3.setTransitMinutes(5);

        candidates.add(c1);
        candidates.add(c2);
        candidates.add(c3);
        return candidates;
    }

    // ===== PLCE NearbyPlaceSearchResponse 역직렬화용 내부 DTO =====

    @Getter
    @Setter
    @NoArgsConstructor
    static class NearbyPlaceSearchResponseDto {
        @JsonProperty("places")
        private List<NearbyPlaceItemDto> places;

        @JsonProperty("radius_used")
        private int radiusUsed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class NearbyPlaceItemDto {
        @JsonProperty("place_id")
        private String placeId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("address")
        private String address;

        @JsonProperty("distance_m")
        private int distanceM;

        @JsonProperty("rating")
        private Float rating;

        @JsonProperty("category")
        private String category;

        @JsonProperty("coordinates")
        private CoordinatesDto coordinates;

        @JsonProperty("is_open")
        private boolean open;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class CoordinatesDto {
        @JsonProperty("lat")
        private double lat;

        @JsonProperty("lng")
        private double lng;
    }
}
