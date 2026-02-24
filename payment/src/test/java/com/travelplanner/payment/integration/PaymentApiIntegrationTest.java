package com.travelplanner.payment.integration;

import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.payment.client.AuthServiceClient;
import com.travelplanner.payment.domain.Subscription;
import com.travelplanner.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * PAYMENT 서비스 통합 테스트.
 *
 * <p>구독 플랜 조회, IAP 결제 구매, 구독 상태 조회 API를 검증한다.</p>
 *
 * <p>외부 의존성 처리:</p>
 * <ul>
 *   <li>AuthServiceClient — @MockBean (AUTH 서비스 HTTP 호출 차단)</li>
 *   <li>RedisTemplate — @MockBean (Redis 미기동 환경 대응)</li>
 *   <li>IapVerificationClient — Phase 1 Mock 구현 (항상 성공 반환), MockBean 불필요</li>
 * </ul>
 *
 * @author 조현아/가디언
 * @since 1.0.0
 */
@DisplayName("PAYMENT API 통합 테스트")
class PaymentApiIntegrationTest extends IntegrationTestBase {

    private static final String TEST_USER_ID      = "usr_test001";
    private static final String SUBSCRIBED_USER_ID = "usr_sub001";

    @MockBean
    private AuthServiceClient authServiceClient;

    @MockBean(name = "redisTemplate")
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void setUp() {
        // RedisTemplate 기본 동작 설정 — 캐시 미스 유도
        HashOperations<String, Object, Object> hashOps = org.mockito.Mockito.mock(HashOperations.class);
        given(redisTemplate.opsForHash()).willReturn(hashOps);
        given(hashOps.get(anyString(), anyString())).willReturn(null);

        // AuthServiceClient — Mock 토큰 반환
        given(authServiceClient.invalidateAndReissueToken(anyString(), anyString()))
            .willReturn("mock_new_access_token_trip_pass");

        // 테스트 간 데이터 격리: 기존 데이터 정리
        subscriptionRepository.deleteAll();

        // SUBSCRIBED_USER_ID에 대한 기존 구독 데이터 저장
        Subscription existingSub = Subscription.create(
            "sub_test001", SUBSCRIBED_USER_ID,
            "plan_trip_pass", SubscriptionTier.TRIP_PASS,
            "APPLE", "txn_existing001"
        );
        subscriptionRepository.save(existingSub);
    }

    // ===== IT-PAY-001 ~ 003: 구독 플랜 목록 조회 =====

    @Nested
    @DisplayName("GET /api/v1/subscriptions/plans")
    class GetSubscriptionPlansIntegrationTest {

        @Test
        @DisplayName("IT-PAY-001: 구독 플랜 목록 조회 성공 시 200과 plans 목록 반환")
        void givenAuthenticatedUser_whenGetPlans_thenReturns200WithPlans() {
            // given
            String token = generateTestToken(TEST_USER_ID);
            HttpHeaders headers = authHeaders(token);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/subscriptions/plans",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("plans");
            List<?> plans = (List<?>) data.get("plans");
            assertThat(plans).isNotEmpty();

            // 첫 번째 플랜 필드 검증
            Map<String, Object> firstPlan = (Map<String, Object>) plans.get(0);
            assertThat(firstPlan).containsKey("plan_id");
            assertThat(firstPlan).containsKey("name");
            assertThat(firstPlan).containsKey("price");
            assertThat(firstPlan).containsKey("features");
        }

        @Test
        @DisplayName("IT-PAY-002: 플랜 목록에 Trip Pass와 Pro 플랜 포함 확인")
        void givenAuthenticatedUser_whenGetPlans_thenBothPlansIncluded() {
            // given
            String token = generateTestToken(TEST_USER_ID);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/subscriptions/plans",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            List<Map<String, Object>> plans = (List<Map<String, Object>>) data.get("plans");

            List<String> planIds = plans.stream()
                .map(p -> (String) p.get("plan_id"))
                .toList();
            assertThat(planIds).contains("plan_trip_pass", "plan_pro");
        }

        @Test
        @DisplayName("IT-PAY-003: 인증 없이 플랜 조회 시 플랜 목록 반환 (공개 엔드포인트)")
        void givenNoToken_whenGetPlans_thenReturns200() {
            // given — 인증 헤더 없음 (공개 엔드포인트)

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/subscriptions/plans",
                HttpMethod.GET,
                new HttpEntity<>(null),
                Map.class
            );

            // then — plans는 공개 정보이므로 200 반환
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // ===== IT-PAY-004 ~ 008: 구독 구매 =====

    @Nested
    @DisplayName("POST /api/v1/subscriptions/purchase")
    class PurchaseSubscriptionIntegrationTest {

        @Test
        @DisplayName("IT-PAY-004: 유효한 Apple IAP 영수증으로 Trip Pass 구매 시 201과 new_access_token 반환")
        void givenValidAppleReceipt_whenPurchaseTripPass_thenReturns201WithToken() {
            // given
            String token = generateTestToken(TEST_USER_ID);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Map.of(
                "plan_id", "plan_trip_pass",
                "receipt", "apple_receipt_base64_test_data",
                "provider", "apple"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/subscriptions/purchase",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("subscription_id");
            assertThat(data).containsKey("new_access_token");
            assertThat(data).containsKey("activated_features");
            assertThat(data.get("tier")).isEqualTo("TRIP_PASS");
            assertThat(data.get("new_access_token")).isNotNull();
        }

        @Test
        @DisplayName("IT-PAY-005: 유효한 Google IAP 영수증으로 Pro 구매 시 201과 activated_features 반환")
        void givenValidGoogleReceipt_whenPurchasePro_thenReturns201WithFeatures() {
            // given
            String token = generateTestToken(TEST_USER_ID);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Map.of(
                "plan_id", "plan_pro",
                "receipt", "google_receipt_test_data",
                "provider", "google"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/subscriptions/purchase",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("tier")).isEqualTo("PRO");
            List<?> features = (List<?>) data.get("activated_features");
            assertThat(features).isNotEmpty();
        }

        @Test
        @DisplayName("IT-PAY-006: 유효하지 않은 plan_id로 구매 시 400 반환")
        void givenInvalidPlanId_whenPurchase_thenReturns400() {
            // given
            String token = generateTestToken(TEST_USER_ID);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Map.of(
                "plan_id", "plan_nonexistent",
                "receipt", "some_receipt_data",
                "provider", "apple"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/subscriptions/purchase",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then — BusinessException → 400
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-PAY-007: provider가 'ios'로 잘못된 값 입력 시 400 반환")
        void givenInvalidProvider_whenPurchase_thenReturns400() {
            // given
            String token = generateTestToken(TEST_USER_ID);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Map.of(
                "plan_id", "plan_trip_pass",
                "receipt", "some_receipt_data",
                "provider", "ios"  // apple|google 만 허용
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/subscriptions/purchase",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IT-PAY-008: receipt 누락 시 400 반환")
        void givenMissingReceipt_whenPurchase_thenReturns400() {
            // given
            String token = generateTestToken(TEST_USER_ID);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Map.of(
                "plan_id", "plan_trip_pass",
                "provider", "apple"
                // receipt 누락
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/subscriptions/purchase",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ===== IT-PAY-009 ~ 012: 구독 상태 조회 =====

    @Nested
    @DisplayName("GET /api/v1/subscriptions/status")
    class GetSubscriptionStatusIntegrationTest {

        @Test
        @DisplayName("IT-PAY-009: 활성 구독 보유 사용자 상태 조회 시 200과 TRIP_PASS 반환")
        void givenActiveSubscription_whenGetStatus_thenReturns200WithTier() {
            // given — SUBSCRIBED_USER_ID는 setUp에서 TRIP_PASS 구독 저장됨
            String token = generateTestToken(SUBSCRIBED_USER_ID, SubscriptionTier.TRIP_PASS);
            HttpHeaders headers = authHeaders(token);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/subscriptions/status",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data).containsKey("tier");
            assertThat(data).containsKey("status");
            assertThat(data.get("tier")).isEqualTo("TRIP_PASS");
            assertThat(data.get("status")).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("IT-PAY-010: 구독 없는 사용자 상태 조회 시 200과 FREE 반환")
        void givenNoSubscription_whenGetStatus_thenReturns200WithFreeTier() {
            // given — TEST_USER_ID는 구독 없음
            String token = generateTestToken(TEST_USER_ID, SubscriptionTier.FREE);
            HttpHeaders headers = authHeaders(token);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/subscriptions/status",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("tier")).isEqualTo("FREE");
        }

        @Test
        @DisplayName("IT-PAY-011: 구매 후 상태 조회 시 새 티어 반영 확인")
        void givenPurchaseCompleted_whenGetStatus_thenNewTierReflected() {
            // given — 먼저 구매 처리
            String token = generateTestToken(TEST_USER_ID, SubscriptionTier.FREE);
            HttpHeaders headers = authHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> purchaseRequest = Map.of(
                "plan_id", "plan_trip_pass",
                "receipt", "apple_receipt_for_status_check",
                "provider", "apple"
            );

            restTemplate.exchange(
                "/api/v1/subscriptions/purchase",
                HttpMethod.POST,
                new HttpEntity<>(purchaseRequest, headers),
                Map.class
            );

            // when — 상태 조회
            HttpHeaders statusHeaders = authHeaders(generateTestToken(TEST_USER_ID, SubscriptionTier.FREE));
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/subscriptions/status",
                HttpMethod.GET,
                new HttpEntity<>(statusHeaders),
                Map.class
            );

            // then — 구매 후 TRIP_PASS 티어 반영
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("tier")).isEqualTo("TRIP_PASS");
            assertThat(data.get("status")).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("IT-PAY-012: 인증 없이 상태 조회 시 401 반환")
        void givenNoToken_whenGetStatus_thenReturns401() {
            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/subscriptions/status",
                HttpMethod.GET,
                new HttpEntity<>(null),
                Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
