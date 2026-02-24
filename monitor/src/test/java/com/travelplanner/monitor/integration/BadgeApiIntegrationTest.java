package com.travelplanner.monitor.integration;

import com.travelplanner.monitor.domain.MonitoringTarget;
import com.travelplanner.monitor.domain.PlaceStatusEnum;
import com.travelplanner.monitor.repository.MonitoringRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MONITOR 서비스 통합 테스트 - 배지 조회 및 상태 상세 조회.
 *
 * <p>외부 의존성 처리:</p>
 * <ul>
 *   <li>RedisTemplate — @MockBean으로 대체 (캐시 계층 우회)</li>
 * </ul>
 *
 * @author 조현아/가디언
 * @since 1.0.0
 */
@DisplayName("MONITOR API 통합 테스트")
class BadgeApiIntegrationTest extends IntegrationTestBase {

    private static final String TEST_USER_ID = "usr_test001";

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MonitoringRepository monitoringRepository;

    @BeforeEach
    void setUp() {
        // 테스트용 모니터링 대상 등록
        MonitoringTarget greenTarget = new MonitoringTarget(
            "mt_test001", "place_abc123", "trip_test001",
            "si_test001", TEST_USER_ID,
            LocalDateTime.now().plusHours(3), 35.6595, 139.7004
        );
        greenTarget.updateStatus(PlaceStatusEnum.GREEN);
        monitoringRepository.save(greenTarget);

        MonitoringTarget yellowTarget = new MonitoringTarget(
            "mt_test002", "place_def456", "trip_test001",
            "si_test002", TEST_USER_ID,
            LocalDateTime.now().plusHours(4), 35.6594, 139.7005
        );
        yellowTarget.updateStatus(PlaceStatusEnum.YELLOW);
        monitoringRepository.save(yellowTarget);

        MonitoringTarget greyTarget = new MonitoringTarget(
            "mt_test003", "place_xyz789", "trip_test002",
            "si_test003", TEST_USER_ID,
            LocalDateTime.now().plusHours(5), 35.6621, 139.6982
        );
        // GREY는 기본 상태이므로 updateStatus 불필요
        monitoringRepository.save(greyTarget);
    }

    // ===== IT-MNTR-001 ~ 004: 배지 목록 조회 =====

    @Nested
    @DisplayName("GET /api/v1/badges")
    class GetBadgesIntegrationTest {

        @Test
        @DisplayName("IT-MNTR-001: 여러 장소 배지 목록 조회 성공 시 200과 badges 반환")
        void givenMultiplePlaceIds_whenGetBadges_thenReturns200WithBadges() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/badges?place_ids=place_abc123,place_def456,place_xyz789",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            List<?> badges = (List<?>) data.get("badges");
            assertThat(badges).hasSize(3);
        }

        @Test
        @DisplayName("IT-MNTR-002: 단일 장소 배지 조회 시 200과 배지 1개 반환")
        void givenSinglePlaceId_whenGetBadges_thenReturns200WithOneBadge() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/badges?place_ids=place_abc123",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            List<?> badges = (List<?>) data.get("badges");
            assertThat(badges).hasSize(1);

            Map<String, Object> badge = (Map<String, Object>) badges.get(0);
            assertThat(badge.get("place_id")).isEqualTo("place_abc123");
            assertThat(badge.get("status")).isEqualTo("GREEN");
            assertThat(badge.get("icon")).isEqualTo("CHECK");
            assertThat(badge.get("color_hex")).isEqualTo("#4CAF50");
        }

        @Test
        @DisplayName("IT-MNTR-003: place_ids 파라미터 누락 시 400 반환")
        void givenMissingPlaceIds_whenGetBadges_thenReturns400() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/badges",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-MNTR-004: 인증 없이 배지 조회 시 401 반환")
        void givenNoToken_whenGetBadges_thenReturns401() {
            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/badges?place_ids=place_abc123",
                HttpMethod.GET,
                new HttpEntity<>(null),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== IT-MNTR-005 ~ 008: 상태 상세 조회 =====

    @Nested
    @DisplayName("GET /api/v1/badges/{placeId}/detail")
    class GetStatusDetailIntegrationTest {

        @Test
        @DisplayName("IT-MNTR-005: GREEN 상태 장소 상세 조회 성공 시 200 반환")
        void givenGreenStatusPlace_whenGetStatusDetail_thenReturns200() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/badges/place_abc123/detail",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("place_id")).isEqualTo("place_abc123");
            assertThat(data.get("overall_status")).isEqualTo("GREEN");
            assertThat(data.get("show_alternative_button")).isEqualTo(false);
        }

        @Test
        @DisplayName("IT-MNTR-006: YELLOW 상태 장소 조회 시 show_alternative_button=true 반환")
        void givenYellowStatusPlace_whenGetStatusDetail_thenShowAlternativeButtonTrue() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/badges/place_def456/detail",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("overall_status")).isEqualTo("YELLOW");
            assertThat(data.get("show_alternative_button")).isEqualTo(true);
        }

        @Test
        @DisplayName("IT-MNTR-007: 존재하지 않는 장소 상태 조회 시 404 반환")
        void givenNonExistentPlaceId_whenGetStatusDetail_thenReturns404() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/badges/place_nonexistent/detail",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("IT-MNTR-008: 인증 없이 상태 상세 조회 시 401 반환")
        void givenNoToken_whenGetStatusDetail_thenReturns401() {
            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/badges/place_abc123/detail",
                HttpMethod.GET,
                new HttpEntity<>(null),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== IT-MNTR-009 ~ 010: 데이터 수집 트리거 =====

    @Nested
    @DisplayName("POST /api/v1/monitor/collect")
    class DataCollectionTriggerIntegrationTest {

        @Test
        @DisplayName("IT-MNTR-009: 데이터 수집 트리거 성공 시 202와 job_id 반환")
        void givenInternalServiceKey_whenTriggerCollect_thenReturns202() {
            // given
            HttpHeaders headers = internalServiceHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Map.of(
                "triggered_by", "scheduler",
                "triggered_at", "2026-03-16T10:00:00"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/monitor/collect",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).containsKey("job_id");
            assertThat(response.getBody().get("status")).isEqualTo("ACCEPTED");
        }

        @Test
        @DisplayName("IT-MNTR-010: 인증 없이 수집 트리거 시 401 반환")
        void givenNoServiceKey_whenTriggerCollect_thenReturns401() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Map.of("triggered_by", "scheduler");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/monitor/collect",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
