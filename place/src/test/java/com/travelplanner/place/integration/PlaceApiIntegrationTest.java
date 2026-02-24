package com.travelplanner.place.integration;

import com.travelplanner.place.client.GooglePlaceDto;
import com.travelplanner.place.client.GooglePlacesClient;
import com.travelplanner.place.domain.Place;
import com.travelplanner.place.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * PLACE 서비스 통합 테스트.
 *
 * <p>외부 의존성 처리:</p>
 * <ul>
 *   <li>GooglePlacesApiClient — @MockBean으로 대체 (Google API 호출 방지)</li>
 *   <li>RedisTemplate — @MockBean으로 대체 (Redis 미기동 환경 대응)</li>
 * </ul>
 *
 * @author 조현아/가디언
 * @since 1.0.0
 */
@DisplayName("PLACE API 통합 테스트")
class PlaceApiIntegrationTest extends IntegrationTestBase {

    private static final String TEST_USER_ID = "usr_test001";

    @MockBean
    private GooglePlacesClient googlePlacesClient;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PlaceRepository placeRepository;

    @BeforeEach
    void setUp() {
        // Google Places API Mock 기본 응답: 빈 목록 반환 (GooglePlaceDto 타입)
        given(googlePlacesClient.textSearch(anyString(), anyString()))
            .willReturn(Collections.<GooglePlaceDto>emptyList());
        given(googlePlacesClient.nearbySearch(anyDouble(), anyDouble(), anyInt(), anyString()))
            .willReturn(Collections.<GooglePlaceDto>emptyList());

        // DB에 테스트 장소 저장
        Place place = Place.create(
            "place_abc123", "이치란 라멘 시부야",
            "도쿄 시부야구 도겐자카 1-22-7", "라멘",
            new BigDecimal("4.2"), new BigDecimal("35.6595000"), new BigDecimal("139.7004000"),
            "Asia/Tokyo", "https://maps.test/photo1.jpg", "도쿄"
        );
        placeRepository.save(place);

        Place nearbyPlace = Place.create(
            "place_xyz789", "후쿠로쿠 라멘",
            "도쿄 시부야구 우다가와초 13-11", "라멘",
            new BigDecimal("4.0"), new BigDecimal("35.6621000"), new BigDecimal("139.6982000"),
            "Asia/Tokyo", "https://maps.test/photo3.jpg", "도쿄"
        );
        placeRepository.save(nearbyPlace);
    }

    // ===== IT-PLCE-001 ~ 005: 장소 검색 =====

    @Nested
    @DisplayName("GET /api/v1/places/search")
    class SearchPlacesIntegrationTest {

        @Test
        @DisplayName("IT-PLCE-001: 키워드 검색 성공 시 200과 places 목록 반환")
        void givenValidKeyword_whenSearchPlaces_thenReturns200WithPlaces() {
            // given
            GooglePlaceDto mockDto = new GooglePlaceDto();
            mockDto.setPlaceId("place_abc123");
            mockDto.setName("이치란 라멘 시부야");
            mockDto.setAddress("도쿄 시부야구 도겐자카 1-22-7");
            mockDto.setCategory("라멘");
            mockDto.setRating(4.2f);
            mockDto.setLat(35.6595);
            mockDto.setLng(139.7004);
            given(googlePlacesClient.textSearch(anyString(), anyString()))
                .willReturn(List.of(mockDto));

            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/places/search?keyword=시부야 라멘&city=도쿄",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("places");
            List<?> places = (List<?>) data.get("places");
            assertThat(places).isNotEmpty();
        }

        @Test
        @DisplayName("IT-PLCE-002: 검색어 1자 입력 시 400 반환")
        void givenKeywordTooShort_whenSearchPlaces_thenReturns400() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/places/search?keyword=라&city=도쿄",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-PLCE-003: keyword 파라미터 누락 시 400 반환")
        void givenMissingKeyword_whenSearchPlaces_thenReturns400() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/places/search?city=도쿄",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-PLCE-005: 인증 없이 검색 시 401 반환")
        void givenNoToken_whenSearchPlaces_thenReturns401() {
            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/places/search?keyword=시부야 라멘&city=도쿄",
                HttpMethod.GET,
                new HttpEntity<>(null),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== IT-PLCE-006 ~ 008: 장소 상세 조회 =====

    @Nested
    @DisplayName("GET /api/v1/places/{place_id}")
    class GetPlaceDetailIntegrationTest {

        @Test
        @DisplayName("IT-PLCE-006: 존재하는 장소 상세 조회 시 200과 상세 정보 반환")
        void givenExistingPlaceId_whenGetPlaceDetail_thenReturns200() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/places/place_abc123",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("place_id")).isEqualTo("place_abc123");
            assertThat(data.get("name")).isEqualTo("이치란 라멘 시부야");
            assertThat(data.get("category")).isEqualTo("라멘");
        }

        @Test
        @DisplayName("IT-PLCE-007: 존재하지 않는 장소 조회 시 404 반환")
        void givenNonExistentPlaceId_whenGetPlaceDetail_thenReturns404() {
            // given
            String token = generateTestToken(TEST_USER_ID);
            given(googlePlacesClient.placeDetail(anyString())).willReturn(new GooglePlaceDto());

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/places/place_nonexistent",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("IT-PLCE-008: 인증 없이 상세 조회 시 401 반환")
        void givenNoToken_whenGetPlaceDetail_thenReturns401() {
            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/places/place_abc123",
                HttpMethod.GET,
                new HttpEntity<>(null),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== IT-PLCE-009 ~ 012: 주변 장소 검색 =====

    @Nested
    @DisplayName("GET /api/v1/places/nearby")
    class SearchNearbyPlacesIntegrationTest {

        @Test
        @DisplayName("IT-PLCE-009: 주변 장소 검색 성공 시 200과 장소 목록 반환")
        void givenValidParams_whenSearchNearbyPlaces_thenReturns200() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/places/nearby?lat=35.6595&lng=139.7004&category=라멘&radius=1000",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("places");
            assertThat(data).containsKey("radius_used");
        }

        @Test
        @DisplayName("IT-PLCE-010: 지원하지 않는 radius=500 입력 시 400 반환")
        void givenInvalidRadius_whenSearchNearbyPlaces_thenReturns400() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/places/nearby?lat=35.6595&lng=139.7004&category=라멘&radius=500",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-PLCE-011: lat 파라미터 누락 시 400 반환")
        void givenMissingLat_whenSearchNearbyPlaces_thenReturns400() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/places/nearby?lng=139.7004&category=라멘&radius=1000",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-PLCE-012: 인증 없이 주변 검색 시 401 반환")
        void givenNoToken_whenSearchNearbyPlaces_thenReturns401() {
            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/places/nearby?lat=35.6595&lng=139.7004&category=라멘&radius=1000",
                HttpMethod.GET,
                new HttpEntity<>(null),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
