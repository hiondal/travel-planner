package com.travelplanner.place.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplanner.common.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Places API 클라이언트.
 *
 * <p>Text Search, Nearby Search, Place Details API를 호출하여
 * 장소 정보를 조회한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Slf4j
@Component
public class GooglePlacesClient {

    private static final String PROVIDER = "Google Places API";
    private static final int MAX_RESULTS = 10;

    private final String apiKey;
    private final WebClient webClient;

    public GooglePlacesClient(
            @Value("${google.places.api-key:}") String apiKey,
            @Value("${google.places.base-url:https://maps.googleapis.com/maps/api}") String baseUrl,
            WebClient.Builder webClientBuilder) {
        this.apiKey = apiKey;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * 키워드와 도시 기반 텍스트 검색을 수행한다.
     *
     * @param keyword 검색 키워드
     * @param city    도시명
     * @return 장소 DTO 목록
     */
    public List<GooglePlaceDto> textSearch(String keyword, String city) {
        String query = keyword + " " + city;
        log.info("Google Places Text Search: query={}", query);

        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/place/textsearch/json")
                            .queryParam("query", query)
                            .queryParam("key", apiKey)
                            .queryParam("language", "ko")
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return parseTextSearchResults(response, city);
        } catch (WebClientResponseException e) {
            log.error("Google Places Text Search failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalApiException(PROVIDER, "Text Search 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Google Places Text Search error: {}", e.getMessage());
            throw new ExternalApiException(PROVIDER, "Text Search 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 좌표 기반 주변 장소 검색을 수행한다.
     *
     * @param lat      위도
     * @param lng      경도
     * @param radiusM  검색 반경 (미터)
     * @param category 카테고리
     * @return 장소 DTO 목록
     */
    public List<GooglePlaceDto> nearbySearch(double lat, double lng, int radiusM, String category) {
        log.info("Google Places Nearby Search: lat={}, lng={}, radius={}, category={}", lat, lng, radiusM, category);

        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/place/nearbysearch/json")
                            .queryParam("location", lat + "," + lng)
                            .queryParam("radius", radiusM)
                            .queryParam("keyword", category)
                            .queryParam("key", apiKey)
                            .queryParam("language", "ko")
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return parseNearbySearchResults(response, lat, lng);
        } catch (WebClientResponseException e) {
            log.error("Google Places Nearby Search failed: status={}", e.getStatusCode());
            throw new ExternalApiException(PROVIDER, "Nearby Search 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Google Places Nearby Search error: {}", e.getMessage());
            throw new ExternalApiException(PROVIDER, "Nearby Search 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 장소 상세 정보를 조회한다.
     *
     * @param placeId Google Place ID
     * @return 장소 DTO
     */
    public GooglePlaceDto placeDetail(String placeId) {
        log.info("Google Places Detail: placeId={}", placeId);

        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/place/details/json")
                            .queryParam("place_id", placeId)
                            .queryParam("fields", "place_id,name,formatted_address,rating,opening_hours,geometry,photos,types,utc_offset_minutes")
                            .queryParam("key", apiKey)
                            .queryParam("language", "ko")
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return parsePlaceDetail(response);
        } catch (WebClientResponseException e) {
            log.error("Google Places Detail failed: status={}", e.getStatusCode());
            throw new ExternalApiException(PROVIDER, "Place Detail 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Google Places Detail error: {}", e.getMessage());
            throw new ExternalApiException(PROVIDER, "Place Detail 오류: " + e.getMessage(), e);
        }
    }

    private List<GooglePlaceDto> parseTextSearchResults(JsonNode response, String city) {
        List<GooglePlaceDto> results = new ArrayList<>();
        if (response == null || !response.has("results")) {
            return results;
        }

        JsonNode resultsNode = response.get("results");
        int count = Math.min(resultsNode.size(), MAX_RESULTS);

        for (int i = 0; i < count; i++) {
            JsonNode item = resultsNode.get(i);
            GooglePlaceDto dto = parseBasicPlace(item);
            results.add(dto);
        }
        return results;
    }

    private List<GooglePlaceDto> parseNearbySearchResults(JsonNode response, double baseLat, double baseLng) {
        List<GooglePlaceDto> results = new ArrayList<>();
        if (response == null || !response.has("results")) {
            return results;
        }

        JsonNode resultsNode = response.get("results");
        int count = Math.min(resultsNode.size(), MAX_RESULTS);

        for (int i = 0; i < count; i++) {
            JsonNode item = resultsNode.get(i);
            GooglePlaceDto dto = parseBasicPlace(item);
            results.add(dto);
        }
        return results;
    }

    private GooglePlaceDto parsePlaceDetail(JsonNode response) {
        if (response == null || !response.has("result")) {
            return new GooglePlaceDto();
        }
        JsonNode result = response.get("result");
        GooglePlaceDto dto = parseBasicPlace(result);

        // 영업시간 파싱
        if (result.has("opening_hours")) {
            JsonNode openingHours = result.get("opening_hours");
            if (openingHours.has("weekday_text")) {
                List<GooglePlaceDto.BusinessHourData> hours = parseWeekdayText(openingHours.get("weekday_text"));
                dto.setBusinessHours(hours);
            }
        }
        return dto;
    }

    private GooglePlaceDto parseBasicPlace(JsonNode item) {
        GooglePlaceDto dto = new GooglePlaceDto();

        if (item.has("place_id")) {
            dto.setPlaceId(item.get("place_id").asText());
        }
        if (item.has("name")) {
            dto.setName(item.get("name").asText());
        }
        if (item.has("formatted_address")) {
            dto.setAddress(item.get("formatted_address").asText());
        } else if (item.has("vicinity")) {
            dto.setAddress(item.get("vicinity").asText());
        }
        if (item.has("rating")) {
            dto.setRating(item.get("rating").floatValue());
        }

        // 좌표 파싱
        if (item.has("geometry") && item.get("geometry").has("location")) {
            JsonNode location = item.get("geometry").get("location");
            dto.setLat(location.get("lat").asDouble());
            dto.setLng(location.get("lng").asDouble());
        }

        // 카테고리 파싱
        if (item.has("types") && item.get("types").isArray() && item.get("types").size() > 0) {
            dto.setCategory(item.get("types").get(0).asText());
        }

        // 대표 사진 URL 파싱
        if (item.has("photos") && item.get("photos").isArray() && item.get("photos").size() > 0) {
            String photoRef = item.get("photos").get(0).get("photo_reference").asText();
            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                    + "?maxwidth=400&photo_reference=" + photoRef + "&key=" + apiKey;
            dto.setPhotoUrl(photoUrl);
        }

        return dto;
    }

    private List<GooglePlaceDto.BusinessHourData> parseWeekdayText(JsonNode weekdayText) {
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        List<GooglePlaceDto.BusinessHourData> hours = new ArrayList<>();

        if (weekdayText.isArray()) {
            for (int i = 0; i < Math.min(weekdayText.size(), days.length); i++) {
                String text = weekdayText.get(i).asText();
                String[] parts = text.split(": ", 2);
                if (parts.length == 2) {
                    String timeRange = parts[1];
                    if (!timeRange.equals("휴무일") && !timeRange.equals("Closed")) {
                        String[] timeParts = timeRange.split(" – ");
                        if (timeParts.length == 2) {
                            hours.add(new GooglePlaceDto.BusinessHourData(
                                    days[i],
                                    normalizeTime(timeParts[0]),
                                    normalizeTime(timeParts[1])
                            ));
                        }
                    }
                }
            }
        }
        return hours;
    }

    private String normalizeTime(String time) {
        // "오전 11:00" → "11:00", "오후 10:00" → "22:00" 형태 변환
        time = time.trim();
        if (time.startsWith("오전 ")) {
            return time.substring(3);
        } else if (time.startsWith("오후 ")) {
            String[] parts = time.substring(3).split(":");
            if (parts.length == 2) {
                int hour = Integer.parseInt(parts[0]);
                if (hour != 12) hour += 12;
                return String.format("%02d:%s", hour, parts[1]);
            }
        }
        return time;
    }
}
