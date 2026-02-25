package com.travelplanner.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.payment.config.SecurityConfig;
import com.travelplanner.payment.domain.Subscription;
import com.travelplanner.payment.domain.SubscriptionStatusEnum;
import com.travelplanner.payment.dto.internal.PurchaseResult;
import com.travelplanner.payment.dto.internal.SubscriptionStatus;
import com.travelplanner.payment.dto.request.PurchaseRequest;
import com.travelplanner.payment.service.SubscriptionService;
import com.travelplanner.common.security.JwtProvider;
import com.travelplanner.common.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PayController 단위 테스트.
 */
@WebMvcTest(PayController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "internal.service-key=test-internal-key")
@WithMockUser
class PayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SubscriptionService subscriptionService;

    // ========================== GET /api/v1/subscriptions/plans ==========================

    @Test
    @DisplayName("구독 플랜 목록 조회 성공 - 200 OK")
    void getSubscriptionPlans_success() throws Exception {
        // Given — 서비스가 빈 리스트 반환 (플랜 구조는 인메모리에서 관리)
        given(subscriptionService.getSubscriptionPlans()).willReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.plans").isArray());
    }

    // ========================== POST /api/v1/subscriptions/purchase ==========================

    @Test
    @DisplayName("구독 구매 성공 - 201 Created")
    void purchaseSubscription_success_201() throws Exception {
        // Given
        UserPrincipal userPrincipal = new UserPrincipal("user-test-001", "test@test.com", SubscriptionTier.FREE);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, List.of());

        Subscription sub = Subscription.create("sub-001", "user-test-001", "plan_pro",
                SubscriptionTier.PRO, "APPLE", "txn_test_001");
        PurchaseResult purchaseResult = new PurchaseResult(sub, "new-access-token-test",
                List.of("대안 카드 기능 이용", "무제한 브리핑", "AI 컨시어지 가이드", "우선 지원"));

        given(subscriptionService.purchaseSubscription(eq("user-test-001"), eq("plan_pro"),
                anyString(), eq("apple")))
                .willReturn(purchaseResult);

        String requestBody = """
                {
                    "plan_id": "plan_pro",
                    "receipt": "receipt-test-data",
                    "provider": "apple"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions/purchase")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tier").value("PRO"))
                .andExpect(jsonPath("$.data.subscription_id").value("sub-001"))
                .andExpect(jsonPath("$.data.new_access_token").value("new-access-token-test"))
                .andExpect(jsonPath("$.data.activated_features").isArray());
    }

    @Test
    @DisplayName("구독 구매 실패 - provider 유효성 검사 400")
    void purchaseSubscription_fail_invalidProvider() throws Exception {
        // Given
        String requestBody = """
                {
                    "plan_id": "plan_pro",
                    "receipt": "receipt-test-data",
                    "provider": "kakao"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구독 구매 실패 - 필수 필드 누락 400")
    void purchaseSubscription_fail_missingFields() throws Exception {
        // Given
        String requestBody = """
                {
                    "plan_id": "plan_pro"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========================== GET /api/v1/subscriptions/status ==========================

    @Test
    @DisplayName("구독 상태 조회 성공 - PRO 구독 중 (내부 서비스 호출)")
    void getSubscriptionStatus_success_pro() throws Exception {
        // Given
        SubscriptionStatus subscriptionStatus = new SubscriptionStatus(
                SubscriptionTier.PRO, SubscriptionStatusEnum.ACTIVE,
                "sub-status-001", null, null);
        given(subscriptionService.getSubscriptionStatus("user-test-001"))
                .willReturn(subscriptionStatus);

        // When & Then
        mockMvc.perform(get("/api/v1/subscriptions/status")
                        .param("userId", "user-test-001")
                        .header("X-Internal-Service-Key", "test-internal-key")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tier").value("PRO"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.subscription_id").value("sub-status-001"));
    }

    @Test
    @DisplayName("구독 상태 조회 성공 - FREE 미구독 (내부 서비스 호출)")
    void getSubscriptionStatus_success_free() throws Exception {
        // Given
        given(subscriptionService.getSubscriptionStatus("user-test-001"))
                .willReturn(SubscriptionStatus.free());

        // When & Then
        mockMvc.perform(get("/api/v1/subscriptions/status")
                        .param("userId", "user-test-001")
                        .header("X-Internal-Service-Key", "test-internal-key")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tier").value("FREE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
