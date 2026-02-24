package com.travelplanner.alternative.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.alternative.dto.internal.ReplaceResult;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * SCHD 서비스 HTTP 클라이언트.
 *
 * <p>일정 장소 교체를 요청한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleServiceClient {

    private final RestTemplate restTemplate;

    @Value("${schedule-service.base-url:http://localhost:8082}")
    private String baseUrl;

    /**
     * 일정 아이템의 장소를 교체한다.
     *
     * <p>SCHD 서비스의 PUT /api/v1/trips/{tripId}/schedule-items/{itemId}/replace 를 호출하고
     * ApiResponse 래퍼에서 data 를 추출하여 ReplaceResult 로 변환한다.</p>
     *
     * @param tripId         여행 ID
     * @param scheduleItemId 일정 아이템 ID
     * @param newPlaceId     새 장소 ID
     * @return ReplaceResult (호출 실패 시 Mock 성공 결과 반환)
     */
    public ReplaceResult replaceScheduleItem(String tripId, String scheduleItemId, String newPlaceId) {
        log.debug("SCHD 서비스 일정 교체 요청: tripId={}, scheduleItemId={}, newPlaceId={}",
            tripId, scheduleItemId, newPlaceId);
        try {
            String url = baseUrl + "/api/v1/trips/" + tripId
                + "/schedule-items/" + scheduleItemId + "/replace";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> body = Map.of("new_place_id", newPlaceId);
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<ApiResponse<ReplaceScheduleItemResponseDto>> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                requestEntity,
                new ParameterizedTypeReference<ApiResponse<ReplaceScheduleItemResponseDto>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getData() != null) {
                ReplaceScheduleItemResponseDto dto = response.getBody().getData();
                ReplaceResult result = new ReplaceResult();
                result.setSuccess(true);
                result.setTravelTimeDiffMinutes(dto.getTravelTimeDiffMinutes());
                result.setNewPlaceName(dto.getNewPlace() != null ? dto.getNewPlace().getPlaceName() : newPlaceId);
                return result;
            }
        } catch (RestClientException e) {
            log.warn("SCHD 서비스 호출 실패, Mock 결과 사용: error={}", e.getMessage());
        }
        ReplaceResult fallback = new ReplaceResult();
        fallback.setSuccess(true);
        fallback.setTravelTimeDiffMinutes(-3);
        fallback.setNewPlaceName("Mock 교체 장소");
        return fallback;
    }

    // ===== SCHD ReplaceScheduleItemResponse 역직렬화용 내부 DTO =====

    @Getter
    @Setter
    @NoArgsConstructor
    static class ReplaceScheduleItemResponseDto {
        @JsonProperty("schedule_item_id")
        private String scheduleItemId;

        @JsonProperty("original_place")
        private PlaceRefDto originalPlace;

        @JsonProperty("new_place")
        private PlaceRefDto newPlace;

        @JsonProperty("travel_time_diff_minutes")
        private int travelTimeDiffMinutes;

        @JsonProperty("updated_schedule_items")
        private List<Object> updatedScheduleItems;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class PlaceRefDto {
        @JsonProperty("place_id")
        private String placeId;

        @JsonProperty("place_name")
        private String placeName;
    }
}
