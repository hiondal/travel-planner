package com.travelplanner.briefing.integration;

import com.travelplanner.briefing.domain.Briefing;
import com.travelplanner.briefing.domain.BriefingContent;
import com.travelplanner.briefing.domain.StatusLevel;
import com.travelplanner.briefing.repository.BriefingRepository;
import com.travelplanner.common.enums.BriefingType;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BRIEFING 서비스 통합 테스트.
 *
 * <p>브리핑 상세 조회, 목록 조회, 생성 API를 검증한다.</p>
 *
 * <p>외부 의존성 처리:</p>
 * <ul>
 *   <li>RedisTemplate — @MockBean으로 대체</li>
 * </ul>
 *
 * @author 조현아/가디언
 * @since 1.0.0
 */
@DisplayName("BRIEFING API 통합 테스트")
class BriefingApiIntegrationTest extends IntegrationTestBase {

    private static final String TEST_USER_ID   = "usr_test001";
    private static final String OTHER_USER_ID  = "usr_test002";

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private BriefingRepository briefingRepository;

    /** 공유 테스트 픽스처 */
    private Briefing safeBriefing;
    private Briefing warningBriefing;
    private Briefing expiredBriefing;
    private Briefing otherUserBriefing;

    @BeforeEach
    void setUp() {
        // 테스트 간 데이터 격리: 기존 데이터 정리
        briefingRepository.deleteAll();

        BriefingContent safeContent = new BriefingContent("영업 중", "보통", "맑음", 15, null, 420);
        safeBriefing = Briefing.create(
            "brif_it001", TEST_USER_ID, "si_test001",
            "place_abc123", "이치란 라멘 시부야", BriefingType.SAFE,
            LocalDateTime.now().plusHours(2),
            "idem_key_001",
            "현재까지 모든 항목 정상입니다. 예정대로 출발하세요.",
            StatusLevel.SAFE, safeContent, Collections.emptyList()
        );
        briefingRepository.save(safeBriefing);

        BriefingContent warningContent = new BriefingContent("영업 중", "혼잡", "맑음", 20, 8, 620);
        warningBriefing = Briefing.create(
            "brif_it002", TEST_USER_ID, "si_test002",
            "place_def456", "시부야 스크램블 교차로", BriefingType.WARNING,
            LocalDateTime.now().plusHours(3),
            "idem_key_002",
            "혼잡도이(가) 감지되었습니다. 대안을 확인해보세요.",
            StatusLevel.CAUTION, warningContent, Collections.emptyList()
        );
        briefingRepository.save(warningBriefing);

        // 만료된 브리핑 (departure_time = 과거)
        BriefingContent expiredContent = new BriefingContent("영업 중", "보통", "맑음", 15, null, 420);
        expiredBriefing = Briefing.create(
            "brif_it003", TEST_USER_ID, "si_test001",
            "place_abc123", "이치란 라멘 시부야", BriefingType.SAFE,
            LocalDateTime.now().minusHours(1),
            "idem_key_003",
            "현재까지 모든 항목 정상입니다.",
            StatusLevel.SAFE, expiredContent, Collections.emptyList()
        );
        briefingRepository.save(expiredBriefing);

        // 다른 사용자 소유 브리핑
        BriefingContent otherContent = new BriefingContent("영업 중", "낮음", "맑음", 5, null, 320);
        otherUserBriefing = Briefing.create(
            "brif_it004", OTHER_USER_ID, "si_other001",
            "place_xyz789", "후쿠로쿠 라멘", BriefingType.SAFE,
            LocalDateTime.now().plusHours(2),
            "idem_key_004",
            "현재까지 모든 항목 정상입니다.",
            StatusLevel.SAFE, otherContent, Collections.emptyList()
        );
        briefingRepository.save(otherUserBriefing);
    }

    // ===== IT-BRIF-001 ~ 006: 브리핑 상세 조회 =====

    @Nested
    @DisplayName("GET /api/v1/briefings/{briefing_id}")
    class GetBriefingIntegrationTest {

        @Test
        @DisplayName("IT-BRIF-001: SAFE 브리핑 상세 조회 성공 시 200과 내용 반환")
        void givenSafeBriefing_whenGetBriefing_thenReturns200WithContent() {
            // given
            HttpHeaders headers = authHeadersFor(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/briefings/brif_it001",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("briefing_id")).isEqualTo("brif_it001");
            assertThat(data.get("type")).isEqualTo("SAFE");
            assertThat(data.get("place_id")).isEqualTo("place_abc123");
            assertThat(data.get("expired")).isEqualTo(false);
            assertThat(data.get("alternative_link")).isNull();

            Map<String, Object> content = (Map<String, Object>) data.get("content");
            assertThat(content).isNotNull();
            assertThat(content).containsKey("summary");
        }

        @Test
        @DisplayName("IT-BRIF-002: 만료된 브리핑 조회 시 200과 expired=true 반환")
        void givenExpiredBriefing_whenGetBriefing_thenReturns200WithExpiredTrue() {
            // given
            HttpHeaders headers = authHeadersFor(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/briefings/brif_it003",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("expired")).isEqualTo(true);
            assertThat(data.get("expire_message")).isNotNull();
        }

        @Test
        @DisplayName("IT-BRIF-003: WARNING 브리핑 조회 시 alternative_link 포함 반환")
        void givenWarningBriefing_whenGetBriefing_thenReturnsAlternativeLink() {
            // given
            HttpHeaders headers = authHeadersFor(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/briefings/brif_it002",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("type")).isEqualTo("WARNING");
            assertThat(data.get("alternative_link")).isNotNull();
            assertThat(data.get("alternative_link").toString())
                .contains("place_def456");
        }

        @Test
        @DisplayName("IT-BRIF-004: 존재하지 않는 브리핑 조회 시 404 반환")
        void givenNonExistentBriefingId_whenGetBriefing_thenReturns404() {
            // given
            HttpHeaders headers = authHeadersFor(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/briefings/brif_nonexistent",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("IT-BRIF-005: 타인의 브리핑 접근 시 403 반환")
        void givenOtherUserBriefing_whenGetBriefing_thenReturns403() {
            // given - TEST_USER_ID로 OTHER_USER_ID 소유 브리핑 접근
            HttpHeaders headers = authHeadersFor(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/briefings/brif_it004",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("IT-BRIF-006: 인증 없이 브리핑 조회 시 401 반환")
        void givenNoToken_whenGetBriefing_thenReturns401() {
            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/briefings/brif_it001",
                HttpMethod.GET,
                new HttpEntity<>(null),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== IT-BRIF-007 ~ 009: 브리핑 목록 조회 =====

    @Nested
    @DisplayName("GET /api/v1/briefings")
    class GetBriefingListIntegrationTest {

        @Test
        @DisplayName("IT-BRIF-007: 오늘 브리핑 목록 조회 성공 시 200과 briefings 반환")
        void givenAuthenticatedUser_whenGetBriefingList_thenReturns200WithList() {
            // given
            HttpHeaders headers = authHeadersFor(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/briefings",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("briefings");
            List<?> briefings = (List<?>) data.get("briefings");
            // 오늘 생성된 TEST_USER_ID 소유 브리핑 3개 반환
            assertThat(briefings).hasSize(3);
        }

        @Test
        @DisplayName("IT-BRIF-008: 특정 날짜 브리핑 목록 조회 시 해당 날짜 목록 반환")
        void givenSpecificDate_whenGetBriefingList_thenReturnsDateFilteredList() {
            // given
            HttpHeaders headers = authHeadersFor(TEST_USER_ID);
            String today = java.time.LocalDate.now().toString();

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/briefings?date=" + today,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("date")).isEqualTo(today);
            assertThat(data).containsKey("briefings");
        }

        @Test
        @DisplayName("IT-BRIF-009: 인증 없이 목록 조회 시 401 반환")
        void givenNoToken_whenGetBriefingList_thenReturns401() {
            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/briefings",
                HttpMethod.GET,
                new HttpEntity<>(null),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== IT-BRIF-010 ~ 011: 브리핑 생성 (내부 호출) =====

    @Nested
    @DisplayName("POST /api/v1/briefings/generate")
    class GenerateBriefingIntegrationTest {

        @Test
        @DisplayName("IT-BRIF-010: 내부 키로 브리핑 생성 성공 시 201과 briefing_id 반환")
        void givenInternalKey_whenGenerateBriefing_thenReturns201() {
            // given
            HttpHeaders headers = internalServiceHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                "schedule_item_id", "si_gen001",
                "place_id", "place_abc123",
                "user_id", TEST_USER_ID,
                "departure_time", LocalDateTime.now().plusHours(2).toString(),
                "triggered_at", LocalDateTime.now().toString()
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/briefings/generate",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).containsKey("briefing_id");
            assertThat(response.getBody().get("status")).isEqualTo("CREATED");
        }

        @Test
        @DisplayName("IT-BRIF-011: 동일 요청 재전송 시 200과 EXISTING 상태 반환 (멱등성)")
        void givenDuplicateRequest_whenGenerateBriefing_thenReturns200WithExisting() {
            // given - 먼저 브리핑 생성
            HttpHeaders headers = internalServiceHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            LocalDateTime departure = LocalDateTime.now().plusHours(2);
            Map<String, Object> request = Map.of(
                "schedule_item_id", "si_idem001",
                "place_id", "place_abc123",
                "user_id", TEST_USER_ID,
                "departure_time", departure.toString(),
                "triggered_at", LocalDateTime.now().toString()
            );

            // 첫 번째 요청 → 201 CREATED
            restTemplate.exchange(
                "/api/v1/briefings/generate",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // when - 동일 요청 재전송
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/briefings/generate",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then - 200 EXISTING 반환 (멱등성)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("status")).isEqualTo("EXISTING");
        }
    }
}
