package com.travelplanner.place.controller;

import com.travelplanner.common.exception.GlobalExceptionHandler;
import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.common.exception.ValidationException;
import com.travelplanner.place.config.PlaceExceptionHandler;
import com.travelplanner.place.config.SecurityConfig;
import com.travelplanner.place.config.SwaggerConfig;
import com.travelplanner.place.domain.Place;
import com.travelplanner.place.dto.internal.NearbyPlace;
import com.travelplanner.place.dto.internal.NearbySearchResult;
import com.travelplanner.place.service.PlaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PlceController 단위 테스트.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@WebMvcTest(controllers = PlceController.class)
@Import({SecurityConfig.class, SwaggerConfig.class, GlobalExceptionHandler.class, PlaceExceptionHandler.class})
class PlceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlaceService placeService;

    private Place samplePlace;
    private Place anotherPlace;

    @BeforeEach
    void setUp() {
        samplePlace = Place.create(
                "place_abc123",
                "이치란 라멘 시부야",
                "도쿄 시부야구 도겐자카 1-22-7",
                "restaurant",
                4.2f,
                35.6595,
                139.7004,
                "Asia/Tokyo",
                "https://example.com/photo.jpg",
                "도쿄"
        );

        anotherPlace = Place.create(
                "place_def456",
                "후쿠로쿠 라멘",
                "도쿄 시부야구 우다가와초 13-11",
                "restaurant",
                4.0f,
                35.6621,
                139.6982,
                "Asia/Tokyo",
                null,
                "도쿄"
        );
    }

    // ===== GET /api/v1/places/search 테스트 =====

    @Test
    @DisplayName("장소 검색 - 정상 요청 200 반환")
    void searchPlaces_validRequest_returns200() throws Exception {
        // given
        given(placeService.searchPlaces("라멘", "도쿄"))
                .willReturn(Arrays.asList(samplePlace, anotherPlace));

        // when & then
        mockMvc.perform(get("/api/v1/places/search")
                        .param("keyword", "라멘")
                        .param("city", "도쿄")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.places").isArray())
                .andExpect(jsonPath("$.data.places[0].place_id").value("place_abc123"))
                .andExpect(jsonPath("$.data.places[0].name").value("이치란 라멘 시부야"))
                .andExpect(jsonPath("$.data.places[0].rating").value(4.2))
                .andExpect(jsonPath("$.data.places[0].coordinates.lat").value(35.6595))
                .andExpect(jsonPath("$.data.places[0].coordinates.lng").value(139.7004));
    }

    @Test
    @DisplayName("장소 검색 - keyword 파라미터 없을 때 400 반환")
    void searchPlaces_missingKeyword_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/places/search")
                        .param("city", "도쿄")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("장소 검색 - keyword 1자 이하일 때 400 반환")
    void searchPlaces_shortKeyword_returns400() throws Exception {
        // 컨트롤러의 validateKeyword에서 ValidationException 발생 (service 호출 전)
        mockMvc.perform(get("/api/v1/places/search")
                        .param("keyword", "a")
                        .param("city", "도쿄")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("장소 검색 - city 파라미터 없을 때 400 반환")
    void searchPlaces_missingCity_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/places/search")
                        .param("keyword", "라멘")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("장소 검색 - 빈 결과 200 반환")
    void searchPlaces_emptyResult_returns200() throws Exception {
        // given
        given(placeService.searchPlaces(anyString(), anyString())).willReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/v1/places/search")
                        .param("keyword", "없는장소")
                        .param("city", "도쿄")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.places").isArray())
                .andExpect(jsonPath("$.data.places").isEmpty());
    }

    // ===== GET /api/v1/places/{place_id} 테스트 =====

    @Test
    @DisplayName("장소 상세 조회 - 정상 요청 200 반환")
    void getPlaceDetail_validRequest_returns200() throws Exception {
        // given
        given(placeService.getPlaceDetail("place_abc123")).willReturn(samplePlace);

        // when & then
        mockMvc.perform(get("/api/v1/places/place_abc123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.place_id").value("place_abc123"))
                .andExpect(jsonPath("$.data.name").value("이치란 라멘 시부야"))
                .andExpect(jsonPath("$.data.category").value("restaurant"))
                .andExpect(jsonPath("$.data.rating").value(4.2))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Tokyo"))
                .andExpect(jsonPath("$.data.coordinates.lat").value(35.6595));
    }

    @Test
    @DisplayName("장소 상세 조회 - 존재하지 않는 장소 404 반환")
    void getPlaceDetail_notFound_returns404() throws Exception {
        // given
        given(placeService.getPlaceDetail("not_exist"))
                .willThrow(new ResourceNotFoundException("Place", "not_exist"));

        // when & then
        mockMvc.perform(get("/api/v1/places/not_exist")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ===== GET /api/v1/places/nearby 테스트 =====

    @Test
    @DisplayName("주변 장소 검색 - 정상 요청 200 반환")
    void searchNearbyPlaces_validRequest_returns200() throws Exception {
        // given
        NearbyPlace nearbyPlace = new NearbyPlace(samplePlace, 320, true);
        NearbySearchResult result = new NearbySearchResult(List.of(nearbyPlace), 1000);
        given(placeService.searchNearbyPlaces(35.6595, 139.7004, "라멘", 1000))
                .willReturn(result);

        // when & then
        mockMvc.perform(get("/api/v1/places/nearby")
                        .param("lat", "35.6595")
                        .param("lng", "139.7004")
                        .param("category", "라멘")
                        .param("radius", "1000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.places").isArray())
                .andExpect(jsonPath("$.data.places[0].place_id").value("place_abc123"))
                .andExpect(jsonPath("$.data.places[0].distance_m").value(320))
                .andExpect(jsonPath("$.data.places[0].is_open").value(true))
                .andExpect(jsonPath("$.data.radius_used").value(1000));
    }

    @Test
    @DisplayName("주변 장소 검색 - 유효하지 않은 반경 400 반환")
    void searchNearbyPlaces_invalidRadius_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/places/nearby")
                        .param("lat", "35.6595")
                        .param("lng", "139.7004")
                        .param("category", "라멘")
                        .param("radius", "500")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주변 장소 검색 - lat 파라미터 없을 때 400 반환")
    void searchNearbyPlaces_missingLat_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/places/nearby")
                        .param("lng", "139.7004")
                        .param("category", "라멘")
                        .param("radius", "1000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주변 장소 검색 - 2km 반경으로 검색 200 반환")
    void searchNearbyPlaces_radius2km_returns200() throws Exception {
        // given
        NearbySearchResult result = new NearbySearchResult(Collections.emptyList(), 2000);
        given(placeService.searchNearbyPlaces(35.6595, 139.7004, "카페", 2000))
                .willReturn(result);

        // when & then
        mockMvc.perform(get("/api/v1/places/nearby")
                        .param("lat", "35.6595")
                        .param("lng", "139.7004")
                        .param("category", "카페")
                        .param("radius", "2000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.radius_used").value(2000));
    }
}
