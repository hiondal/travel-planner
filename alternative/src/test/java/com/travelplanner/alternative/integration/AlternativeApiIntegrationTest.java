package com.travelplanner.alternative.integration;

import com.travelplanner.alternative.client.MonitorServiceClient;
import com.travelplanner.alternative.client.PlaceServiceClient;
import com.travelplanner.alternative.client.ScheduleServiceClient;
import com.travelplanner.alternative.domain.Alternative;
import com.travelplanner.alternative.repository.AlternativeRepository;
import com.travelplanner.common.enums.SubscriptionTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * ALTERNATIVE 서비스 통합 테스트.
 *
 * <p>대안 검색(Paywall 포함), 대안 카드 선택 API를 검증한다.</p>
 *
 * <p>외부 의존성 처리:</p>
 * <ul>
 *   <li>PlaceServiceClient — @MockBean (서비스 간 HTTP 호출 차단)</li>
 *   <li>MonitorServiceClient — @MockBean (서비스 간 HTTP 호출 차단)</li>
 *   <li>ScheduleServiceClient — @MockBean (서비스 간 HTTP 호출 차단)</li>
 *   <li>RedisTemplate — @MockBean (Redis 미기동 환경 대응)</li>
 * </ul>
 *
 * @author 조현아/가디언
 * @since 1.0.0
 */
@DisplayName("ALTERNATIVE API 통합 테스트")
class AlternativeApiIntegrationTest extends IntegrationTestBase {

    private static final String TEST_USER_ID  = "usr_test001";
    private static final String PASS_USER_ID  = "usr_pass001";

    @MockBean
    private PlaceServiceClient placeServiceClient;

    @MockBean
    private MonitorServiceClient monitorServiceClient;

    @MockBean
    private ScheduleServiceClient scheduleServiceClient;

    @MockBean(name = "redisTemplate")
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @Autowired
    private AlternativeRepository alternativeRepository;

    @BeforeEach
    void setUp() {
        // RedisTemplate 기본 동작 설정 — null 반환으로 캐시 미스 유도
        ValueOperations<String, String> valueOps = org.mockito.Mockito.mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn(null);
        given(redisTemplate.keys(anyString())).willReturn(null);

        // PlaceServiceClient — Mock 후보 3개 반환
        com.travelplanner.alternative.domain.PlaceCandidate c1 = buildCandidate(
            "place_nearby001", "Mock 라멘 1호점", 300, 4.2f, "라멘", "낮음");
        com.travelplanner.alternative.domain.PlaceCandidate c2 = buildCandidate(
            "place_nearby002", "Mock 라멘 2호점", 600, 4.0f, "라멘", "보통");
        com.travelplanner.alternative.domain.PlaceCandidate c3 = buildCandidate(
            "place_nearby003", "Mock 라멘 3호점", 900, 3.8f, "라멘", "낮음");

        given(placeServiceClient.searchNearby(
            org.mockito.ArgumentMatchers.anyDouble(),
            org.mockito.ArgumentMatchers.anyDouble(),
            anyString(),
            org.mockito.ArgumentMatchers.anyInt()))
            .willReturn(java.util.List.of(c1, c2, c3));

        // MonitorServiceClient — GREEN 배지 반환 (모든 후보 통과)
        com.travelplanner.alternative.domain.StatusBadge greenBadge1 = buildBadge("place_nearby001", "GREEN");
        com.travelplanner.alternative.domain.StatusBadge greenBadge2 = buildBadge("place_nearby002", "GREEN");
        com.travelplanner.alternative.domain.StatusBadge greenBadge3 = buildBadge("place_nearby003", "GREEN");
        given(monitorServiceClient.getBadges(org.mockito.ArgumentMatchers.anyList()))
            .willReturn(Map.of(
                "place_nearby001", greenBadge1,
                "place_nearby002", greenBadge2,
                "place_nearby003", greenBadge3
            ));

        // ScheduleServiceClient — 교체 성공 반환
        com.travelplanner.alternative.dto.internal.ReplaceResult replaceResult =
            new com.travelplanner.alternative.dto.internal.ReplaceResult();
        replaceResult.setSuccess(true);
        replaceResult.setTravelTimeDiffMinutes(-3);
        replaceResult.setNewPlaceName("Mock 대안 장소");
        given(scheduleServiceClient.replaceScheduleItem(anyString(), anyString(), anyString()))
            .willReturn(replaceResult);
    }

    // ===== IT-ALTN-001 ~ 005: 대안 검색 =====

    @Nested
    @DisplayName("POST /api/v1/alternatives/search")
    class SearchAlternativesIntegrationTest {

        @Test
        @DisplayName("IT-ALTN-001: FREE 티어 사용자 대안 검색 시 402 Paywall 반환")
        void givenFreeUser_whenSearchAlternatives_thenReturns402Paywall() {
            // given — FREE 티어 토큰
            String token = generateTestToken(TEST_USER_ID, SubscriptionTier.FREE);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "place_id", "place_abc123",
                "category", "라멘",
                "location", Map.of("lat", 35.6595, "lng", 139.7004)
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/alternatives/search",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
            assertThat(response.getBody()).containsKey("upgrade_url");
        }

        @Test
        @DisplayName("IT-ALTN-002: TRIP_PASS 티어 사용자 대안 검색 성공 시 200과 카드 3장 반환")
        void givenTripPassUser_whenSearchAlternatives_thenReturns200WithCards() {
            // given — TRIP_PASS 티어 토큰
            String token = generateTestToken(PASS_USER_ID, SubscriptionTier.TRIP_PASS);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "place_id", "place_abc123",
                "category", "라멘",
                "location", Map.of("lat", 35.6595, "lng", 139.7004)
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/alternatives/search",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("alternatives");
            assertThat(data).containsKey("original_place_id");
            assertThat(data.get("original_place_id")).isEqualTo("place_abc123");

            java.util.List<?> alternatives = (java.util.List<?>) data.get("alternatives");
            assertThat(alternatives).hasSize(3);
        }

        @Test
        @DisplayName("IT-ALTN-003: PRO 티어 사용자 대안 검색 성공 시 200과 카드 3장 반환")
        void givenProUser_whenSearchAlternatives_thenReturns200WithCards() {
            // given — PRO 티어 토큰
            String token = generateTestToken(PASS_USER_ID, SubscriptionTier.PRO);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "place_id", "place_abc123",
                "category", "라멘",
                "location", Map.of("lat", 35.6595, "lng", 139.7004)
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/alternatives/search",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            java.util.List<?> alternatives = (java.util.List<?>) data.get("alternatives");
            assertThat(alternatives).isNotEmpty();

            // 각 카드에 rank, reason, place 정보 포함 확인
            Map<String, Object> firstCard = (Map<String, Object>) alternatives.get(0);
            assertThat(firstCard).containsKey("alt_id");
            assertThat(firstCard).containsKey("rank");
            assertThat(firstCard).containsKey("reason");
        }

        @Test
        @DisplayName("IT-ALTN-004: place_id 누락 시 400 반환")
        void givenMissingPlaceId_whenSearchAlternatives_thenReturns400() {
            // given — place_id 없이 요청
            String token = generateTestToken(PASS_USER_ID, SubscriptionTier.TRIP_PASS);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "category", "라멘",
                "location", Map.of("lat", 35.6595, "lng", 139.7004)
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/alternatives/search",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-ALTN-005: 인증 없이 대안 검색 시 401 반환")
        void givenNoToken_whenSearchAlternatives_thenReturns401() {
            // given — 토큰 없음
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "place_id", "place_abc123",
                "category", "라멘",
                "location", Map.of("lat", 35.6595, "lng", 139.7004)
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/alternatives/search",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== IT-ALTN-006 ~ 010: 대안 카드 선택 =====

    @Nested
    @DisplayName("POST /api/v1/alternatives/{altId}/select")
    class SelectAlternativeIntegrationTest {

        private String savedAltId;

        @BeforeEach
        void saveAlternative() {
            // 선택할 대안 카드 DB 사전 저장
            com.travelplanner.alternative.domain.PlaceCandidate candidate =
                buildCandidate("place_nearby001", "Mock 라멘 1호점", 300, 4.2f, "라멘", "낮음");
            Alternative alt = Alternative.create(
                "alt_test001", PASS_USER_ID,
                "place_abc123", candidate,
                "근거리 영업 중 동일 카테고리", 0.91
            );
            alternativeRepository.save(alt);
            savedAltId = "alt_test001";
        }

        @Test
        @DisplayName("IT-ALTN-006: TRIP_PASS 사용자가 유효한 대안 선택 시 200과 교체 결과 반환")
        void givenValidAlt_whenSelectAlternative_thenReturns200WithResult() {
            // given
            String token = generateTestToken(PASS_USER_ID, SubscriptionTier.TRIP_PASS);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "original_place_id", "place_abc123",
                "schedule_item_id", "si_test001",
                "trip_id", "trip_test001",
                "selected_rank", 1,
                "elapsed_seconds", 12
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/alternatives/" + savedAltId + "/select",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("schedule_item_id");
            assertThat(data).containsKey("original_place");
            assertThat(data).containsKey("new_place");
            assertThat(data.get("schedule_item_id")).isEqualTo("si_test001");
        }

        @Test
        @DisplayName("IT-ALTN-007: 존재하지 않는 대안 카드 선택 시 404 반환")
        void givenNonExistentAltId_whenSelectAlternative_thenReturns404() {
            // given
            String token = generateTestToken(PASS_USER_ID, SubscriptionTier.TRIP_PASS);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "original_place_id", "place_abc123",
                "schedule_item_id", "si_test001",
                "trip_id", "trip_test001",
                "selected_rank", 1,
                "elapsed_seconds", 12
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/alternatives/alt_nonexistent/select",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("IT-ALTN-008: selected_rank 범위 초과(4) 시 400 반환")
        void givenInvalidRank_whenSelectAlternative_thenReturns400() {
            // given
            String token = generateTestToken(PASS_USER_ID, SubscriptionTier.TRIP_PASS);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "original_place_id", "place_abc123",
                "schedule_item_id", "si_test001",
                "trip_id", "trip_test001",
                "selected_rank", 4,   // 유효 범위 1~3 초과
                "elapsed_seconds", 12
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/alternatives/" + savedAltId + "/select",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-ALTN-009: schedule_item_id 누락 시 400 반환")
        void givenMissingScheduleItemId_whenSelectAlternative_thenReturns400() {
            // given
            String token = generateTestToken(PASS_USER_ID, SubscriptionTier.TRIP_PASS);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "original_place_id", "place_abc123",
                "trip_id", "trip_test001",
                "selected_rank", 1,
                "elapsed_seconds", 12
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/alternatives/" + savedAltId + "/select",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-ALTN-010: 인증 없이 대안 선택 시 401 반환")
        void givenNoToken_whenSelectAlternative_thenReturns401() {
            // given — 토큰 없음
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "original_place_id", "place_abc123",
                "schedule_item_id", "si_test001",
                "trip_id", "trip_test001",
                "selected_rank", 1,
                "elapsed_seconds", 12
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/alternatives/" + savedAltId + "/select",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== 헬퍼 메서드 =====

    private com.travelplanner.alternative.domain.PlaceCandidate buildCandidate(
            String placeId, String name, int distanceM, float rating,
            String category, String congestion) {
        com.travelplanner.alternative.domain.PlaceCandidate candidate =
            new com.travelplanner.alternative.domain.PlaceCandidate();
        candidate.setPlaceId(placeId);
        candidate.setName(name);
        candidate.setDistanceM(distanceM);
        candidate.setRating(rating);
        candidate.setCategory(category);
        candidate.setCongestion(congestion);
        candidate.setOpen(true);
        candidate.setLat(35.6595 + distanceM * 0.000001);
        candidate.setLng(139.7004 + distanceM * 0.000001);
        candidate.setWalkingMinutes(distanceM / 80);
        return candidate;
    }

    private com.travelplanner.alternative.domain.StatusBadge buildBadge(String placeId, String status) {
        com.travelplanner.alternative.domain.StatusBadge badge =
            new com.travelplanner.alternative.domain.StatusBadge();
        badge.setPlaceId(placeId);
        badge.setStatus(status);
        badge.setIcon("CHECK");
        return badge;
    }
}
