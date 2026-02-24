package com.travelplanner.schedule.integration;

import com.travelplanner.schedule.client.PlaceServiceClient;
import com.travelplanner.schedule.domain.ScheduleItem;
import com.travelplanner.schedule.domain.Trip;
import com.travelplanner.schedule.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * SCHEDULE 서비스 통합 테스트 - 여행 CRUD 및 일정 아이템 관리.
 *
 * <p>외부 의존성 처리:</p>
 * <ul>
 *   <li>PlaceServiceClient — @MockBean으로 대체 (PLCE 서비스 독립 테스트)</li>
 *   <li>RedisTemplate — @MockBean으로 대체 (Redis 미기동 환경 대응)</li>
 * </ul>
 *
 * @author 조현아/가디언
 * @since 1.0.0
 */
@DisplayName("SCHEDULE API 통합 테스트")
class TripApiIntegrationTest extends IntegrationTestBase {

    private static final String TEST_USER_ID = "usr_test001";
    private static final String OTHER_USER_ID = "usr_other001";

    @MockBean
    private PlaceServiceClient placeServiceClient;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TripRepository tripRepository;

    @BeforeEach
    void setUp() {
        // PLCE 서비스 기본 응답 Mock 설정
        PlaceServiceClient.PlaceDetail mockDetail = new PlaceServiceClient.PlaceDetail();
        PlaceServiceClient.BusinessHour bh = new PlaceServiceClient.BusinessHour();
        // 영업시간 있는 장소 기본 응답
        given(placeServiceClient.getPlaceDetail(anyString())).willReturn(mockDetail);
    }

    // ===== IT-SCHD-001 ~ 005: 여행 생성 =====

    @Nested
    @DisplayName("POST /api/v1/trips")
    class CreateTripIntegrationTest {

        @Test
        @DisplayName("IT-SCHD-001: 여행 일정 생성 성공 시 201과 trip_id 반환")
        void givenValidRequest_whenCreateTrip_thenReturns201WithTripId() {
            // given
            String token = generateTestToken(TEST_USER_ID);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "name", "도쿄 3박4일",
                "start_date", "2026-03-15",
                "end_date", "2026-03-18",
                "city", "도쿄"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("trip_id");
            assertThat(data.get("name")).isEqualTo("도쿄 3박4일");
            assertThat(data.get("city")).isEqualTo("도쿄");
            assertThat(data.get("status")).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("IT-SCHD-002: 여행명 누락 시 400 반환")
        void givenMissingName_whenCreateTrip_thenReturns400() {
            // given
            String token = generateTestToken(TEST_USER_ID);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "start_date", "2026-03-15",
                "end_date", "2026-03-18",
                "city", "도쿄"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-SCHD-003: 여행명 50자 초과 시 400 반환")
        void givenNameExceeds50Chars_whenCreateTrip_thenReturns400() {
            // given
            String token = generateTestToken(TEST_USER_ID);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String longName = "A".repeat(51);
            Map<String, Object> request = Map.of(
                "name", longName,
                "start_date", "2026-03-15",
                "end_date", "2026-03-18",
                "city", "도쿄"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-SCHD-005: 인증 없이 여행 생성 시 401 반환")
        void givenNoToken_whenCreateTrip_thenReturns401() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "name", "도쿄 3박4일",
                "start_date", "2026-03-15",
                "end_date", "2026-03-18",
                "city", "도쿄"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== IT-SCHD-006 ~ 009: 여행 조회 및 일정표 조회 =====

    @Nested
    @DisplayName("GET /api/v1/trips/{tripId}")
    class GetTripIntegrationTest {

        @Test
        @DisplayName("IT-SCHD-006: 존재하는 여행 조회 시 200과 여행 정보 반환")
        void givenExistingTrip_whenGetTrip_thenReturns200() {
            // given - DB에 여행 저장
            Trip trip = new Trip("trip_it001", TEST_USER_ID, "도쿄 테스트여행",
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 18), "도쿄");
            tripRepository.save(trip);

            String token = generateTestToken(TEST_USER_ID);
            HttpHeaders headers = authHeadersFor(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips/trip_it001",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("trip_id")).isEqualTo("trip_it001");
            assertThat(data.get("name")).isEqualTo("도쿄 테스트여행");
            assertThat(data.get("status")).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("IT-SCHD-007: 존재하지 않는 여행 조회 시 404 반환")
        void givenNonExistentTripId_whenGetTrip_thenReturns404() {
            // given
            HttpHeaders headers = authHeadersFor(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips/trip_nonexistent",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("IT-SCHD-008: 인증 없이 여행 조회 시 401 반환")
        void givenNoToken_whenGetTrip_thenReturns401() {
            // given
            HttpHeaders headers = new HttpHeaders();

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips/trip_test001",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== IT-SCHD-009: 일정표 조회 =====

    @Nested
    @DisplayName("GET /api/v1/trips/{tripId}/schedule")
    class GetScheduleIntegrationTest {

        @Test
        @DisplayName("IT-SCHD-009: 일정표 조회 성공 시 200과 schedule_items 반환")
        void givenExistingTrip_whenGetSchedule_thenReturns200WithItems() {
            // given
            Trip trip = new Trip("trip_it002", TEST_USER_ID, "오사카 테스트여행",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 3), "오사카");
            tripRepository.save(trip);

            HttpHeaders headers = authHeadersFor(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips/trip_it002/schedule",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("trip_id")).isEqualTo("trip_it002");
            assertThat(data).containsKey("schedule_items");
        }
    }

    // ===== IT-SCHD-010 ~ 014: 장소 추가 =====

    @Nested
    @DisplayName("POST /api/v1/trips/{tripId}/schedule-items")
    class AddScheduleItemIntegrationTest {

        @Test
        @DisplayName("IT-SCHD-010: 장소 추가 성공 시 201과 schedule_item_id 반환")
        void givenValidRequest_whenAddScheduleItem_thenReturns201() {
            // given
            Trip trip = new Trip("trip_it003", TEST_USER_ID, "장소추가 테스트",
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 18), "도쿄");
            tripRepository.save(trip);

            // PLCE 서비스 응답 Mock: 영업시간 내 장소
            PlaceServiceClient.PlaceDetail detail = PlaceServiceClient.PlaceDetail.unknown("place_abc123");
            given(placeServiceClient.getPlaceDetail("place_abc123")).willReturn(detail);

            String token = generateTestToken(TEST_USER_ID);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "place_id", "place_abc123",
                "visit_datetime", "2026-03-16T12:00:00+09:00",
                "timezone", "Asia/Tokyo"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips/trip_it003/schedule-items",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("schedule_item_id");
            assertThat(data.get("place_id")).isEqualTo("place_abc123");
        }

        @Test
        @DisplayName("IT-SCHD-011: place_id 누락 시 400 반환")
        void givenMissingPlaceId_whenAddScheduleItem_thenReturns400() {
            // given
            Trip trip = new Trip("trip_it004", TEST_USER_ID, "검증 테스트",
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 18), "도쿄");
            tripRepository.save(trip);

            String token = generateTestToken(TEST_USER_ID);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "visit_datetime", "2026-03-16T12:00:00+09:00",
                "timezone", "Asia/Tokyo"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips/trip_it004/schedule-items",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-SCHD-012: 인증 없이 장소 추가 시 401 반환")
        void givenNoToken_whenAddScheduleItem_thenReturns401() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "place_id", "place_abc123",
                "visit_datetime", "2026-03-16T12:00:00+09:00",
                "timezone", "Asia/Tokyo"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips/trip_test001/schedule-items",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== IT-SCHD-015 ~ 016: 장소 삭제 =====

    @Nested
    @DisplayName("DELETE /api/v1/trips/{tripId}/schedule-items/{itemId}")
    class DeleteScheduleItemIntegrationTest {

        @Test
        @DisplayName("IT-SCHD-015: 일정 장소 삭제 성공 시 204 반환")
        void givenExistingItem_whenDeleteScheduleItem_thenReturns204() {
            // given
            Trip trip = new Trip("trip_it005", TEST_USER_ID, "삭제 테스트",
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 18), "도쿄");
            tripRepository.save(trip);

            ScheduleItem item = new ScheduleItem(
                "si_it001", "trip_it005", "place_abc123", "이치란 라멘",
                LocalDateTime.of(2026, 3, 16, 12, 0), "Asia/Tokyo", 1
            );
            trip.getScheduleItems().add(item);
            tripRepository.save(trip);

            HttpHeaders headers = authHeadersFor(TEST_USER_ID);

            // when
            ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/trips/trip_it005/schedule-items/si_it001",
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("IT-SCHD-016: 존재하지 않는 아이템 삭제 시 404 반환")
        void givenNonExistentItem_whenDeleteScheduleItem_thenReturns404() {
            // given
            Trip trip = new Trip("trip_it006", TEST_USER_ID, "삭제 테스트 2",
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 18), "도쿄");
            tripRepository.save(trip);

            HttpHeaders headers = authHeadersFor(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips/trip_it006/schedule-items/si_nonexistent",
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ===== IT-SCHD-017 ~ 018: 장소 교체 (Cross-Service) =====

    @Nested
    @DisplayName("PUT /api/v1/trips/{tripId}/schedule-items/{itemId}/replace")
    class ReplaceScheduleItemIntegrationTest {

        @Test
        @DisplayName("IT-SCHD-017: 장소 교체 성공 시 200과 교체 결과 반환")
        void givenValidReplaceRequest_whenReplaceScheduleItem_thenReturns200() {
            // given
            Trip trip = new Trip("trip_it007", TEST_USER_ID, "교체 테스트",
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 18), "도쿄");
            tripRepository.save(trip);

            ScheduleItem item = new ScheduleItem(
                "si_it002", "trip_it007", "place_abc123", "이치란 라멘",
                LocalDateTime.of(2026, 3, 16, 12, 0), "Asia/Tokyo", 1
            );
            trip.getScheduleItems().add(item);
            tripRepository.save(trip);

            // PLCE Mock: 대안 장소 정보
            PlaceServiceClient.PlaceDetail altDetail = PlaceServiceClient.PlaceDetail.unknown("place_xyz789");
            given(placeServiceClient.getPlaceDetail("place_xyz789")).willReturn(altDetail);

            HttpHeaders headers = authHeadersFor(TEST_USER_ID);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Map.of("new_place_id", "place_xyz789");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips/trip_it007/schedule-items/si_it002/replace",
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("schedule_item_id");
            assertThat(data).containsKey("original_place");
            assertThat(data).containsKey("new_place");
        }

        @Test
        @DisplayName("IT-SCHD-018 (Cross-Service): PLCE 연동 - 장소 교체 시 대안 place_name 스냅샷 확인")
        void givenValidPlaceId_whenReplaceScheduleItem_thenNewPlaceNameIsFromPlceService() {
            // given
            Trip trip = new Trip("trip_it008", TEST_USER_ID, "크로스서비스 교체",
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 18), "도쿄");
            tripRepository.save(trip);

            ScheduleItem item = new ScheduleItem(
                "si_it003", "trip_it008", "place_abc123", "이치란 라멘",
                LocalDateTime.of(2026, 3, 16, 12, 0), "Asia/Tokyo", 1
            );
            trip.getScheduleItems().add(item);
            tripRepository.save(trip);

            // PLCE Mock: 명시적 장소명 반환
            PlaceServiceClient.PlaceDetail altDetail = new PlaceServiceClient.PlaceDetail();
            // PlaceDetail.unknown은 "알 수 없는 장소"를 반환하므로 실제 이름 확인
            given(placeServiceClient.getPlaceDetail("place_xyz789"))
                .willReturn(PlaceServiceClient.PlaceDetail.unknown("place_xyz789"));

            HttpHeaders headers = authHeadersFor(TEST_USER_ID);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Map.of("new_place_id", "place_xyz789");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/trips/trip_it008/schedule-items/si_it003/replace",
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then - PLCE 서비스가 실제로 호출되어 place_name이 스냅샷됨을 확인
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            Map<String, Object> newPlace = (Map<String, Object>) data.get("new_place");
            assertThat(newPlace.get("place_id")).isEqualTo("place_xyz789");
            // PLCE Mock 응답의 name이 스냅샷됨을 확인
            assertThat(newPlace.get("place_name")).isNotNull();
        }
    }
}
