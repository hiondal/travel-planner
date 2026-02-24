package com.travelplanner.place.controller;

import com.travelplanner.common.exception.ValidationException;
import com.travelplanner.common.response.ApiResponse;
import com.travelplanner.place.domain.Place;
import com.travelplanner.place.dto.internal.NearbySearchResult;
import com.travelplanner.place.dto.response.NearbyPlaceSearchResponse;
import com.travelplanner.place.dto.response.PlaceDetailResponse;
import com.travelplanner.place.dto.response.PlaceSearchResponse;
import com.travelplanner.place.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 장소 REST API 컨트롤러.
 *
 * <p>장소 검색, 상세 조회, 주변 장소 검색 엔드포인트를 제공한다.</p>
 *
 * <ul>
 *   <li>GET /api/v1/places/search — 키워드 기반 장소 검색 (PLCE-01)</li>
 *   <li>GET /api/v1/places/{place_id} — 장소 상세 조회 (PLCE-02)</li>
 *   <li>GET /api/v1/places/nearby — 주변 장소 검색 (PLCE-03)</li>
 * </ul>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Tag(name = "Places", description = "장소 검색 및 조회")
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlceController {

    private static final int MIN_KEYWORD_LENGTH = 2;
    private static final Set<Integer> ALLOWED_RADII = Set.of(1000, 2000, 3000);

    private final PlaceService placeService;

    /**
     * 키워드와 도시 기반으로 장소를 검색한다 (PLCE-01).
     *
     * @param keyword 검색 키워드 (최소 2자)
     * @param city    검색 도시
     * @return 장소 목록 (최대 10개)
     */
    @Operation(
        summary = "키워드 기반 장소 검색",
        description = "키워드와 도시를 기준으로 장소를 검색한다. Google Places Text Search API를 호출하며, 결과는 30분간 캐싱된다."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PlaceSearchResponse>> searchPlaces(
            @Parameter(description = "검색 키워드 (최소 2자)", required = true, example = "시부야 라멘")
            @RequestParam String keyword,
            @Parameter(description = "검색 도시", required = true, example = "도쿄")
            @RequestParam String city) {
        validateKeyword(keyword);

        List<Place> places = placeService.searchPlaces(keyword, city);
        PlaceSearchResponse response = PlaceSearchResponse.of(places);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 장소 상세 정보를 조회한다 (PLCE-02).
     *
     * @param placeId Google Place ID
     * @return 장소 상세 정보
     */
    @Operation(
        summary = "장소 상세 정보 조회",
        description = "특정 장소의 영업시간, 위치, 카테고리, 평점 등 상세 정보를 조회한다. 동일 장소 재조회 시 캐시를 활용한다."
    )
    @GetMapping("/{place_id}")
    public ResponseEntity<ApiResponse<PlaceDetailResponse>> getPlaceDetail(
            @Parameter(description = "장소 ID", required = true, example = "place_abc123")
            @PathVariable("place_id") String placeId) {
        Place place = placeService.getPlaceDetail(placeId);
        PlaceDetailResponse response = PlaceDetailResponse.from(place);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 좌표 기반 주변 장소를 검색한다 (PLCE-03).
     *
     * @param lat      기준 위도
     * @param lng      기준 경도
     * @param category 카테고리
     * @param radius   검색 반경 (1000/2000/3000 미터)
     * @return 주변 장소 검색 결과
     */
    @Operation(
        summary = "반경 기반 주변 장소 검색",
        description = "특정 좌표 기준 반경 내에서 카테고리별 영업 중인 장소를 검색한다. 반경은 1km/2km/3km 단계 지정 가능."
    )
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<NearbyPlaceSearchResponse>> searchNearbyPlaces(
            @Parameter(description = "기준 위도", required = true, example = "35.6595")
            @RequestParam double lat,
            @Parameter(description = "기준 경도", required = true, example = "139.7004")
            @RequestParam double lng,
            @Parameter(description = "장소 카테고리", required = true, example = "라멘")
            @RequestParam String category,
            @Parameter(description = "검색 반경 (미터, 1000/2000/3000)", required = true, example = "1000")
            @RequestParam int radius) {
        validateRadius(radius);

        NearbySearchResult result = placeService.searchNearbyPlaces(lat, lng, category, radius);
        NearbyPlaceSearchResponse response = NearbyPlaceSearchResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private void validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().length() < MIN_KEYWORD_LENGTH) {
            throw new ValidationException("keyword", "검색어는 최소 " + MIN_KEYWORD_LENGTH + "자 이상이어야 합니다.", keyword);
        }
    }

    private void validateRadius(int radius) {
        if (!ALLOWED_RADII.contains(radius)) {
            throw new ValidationException("radius", "검색 반경은 1000, 2000, 3000 중 하나여야 합니다.", radius);
        }
    }
}
