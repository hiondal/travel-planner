package com.travelplanner.briefing.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.common.response.ApiResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
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
 * PLCE 서비스 클라이언트.
 *
 * <p>장소 이름을 조회하기 위해 PLCE 서비스를 호출한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceServiceClient {

    private final RestTemplate restTemplate;

    @Value("${place-service.base-url:http://localhost:8083}")
    private String baseUrl;

    @Value("${internal.service-key:}")
    private String internalServiceKey;

    /**
     * 장소 이름을 조회한다.
     *
     * @param placeId 장소 ID
     * @return 장소 이름 (실패 시 null)
     */
    public String getPlaceName(String placeId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Service-Key", internalServiceKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/api/v1/places/{placeId}")
                    .buildAndExpand(placeId)
                    .toUriString();
            ResponseEntity<ApiResponse<PlaceDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ApiResponse<PlaceDto>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getData() != null) {
                return response.getBody().getData().getName();
            }
        } catch (RestClientException e) {
            log.warn("PLCE 서비스 장소 이름 조회 실패: placeId={}, error={}", placeId, e.getMessage());
        }
        return null;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PlaceDto {
        @JsonProperty("name")
        private String name;
    }
}
