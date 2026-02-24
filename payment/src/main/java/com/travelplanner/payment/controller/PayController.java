package com.travelplanner.payment.controller;

import com.travelplanner.common.response.ApiResponse;
import com.travelplanner.common.security.UserPrincipal;
import com.travelplanner.payment.dto.internal.PurchaseResult;
import com.travelplanner.payment.dto.internal.SubscriptionStatus;
import com.travelplanner.payment.dto.request.PurchaseRequest;
import com.travelplanner.payment.dto.response.PurchaseResponse;
import com.travelplanner.payment.dto.response.SubscriptionPlansResponse;
import com.travelplanner.payment.dto.response.SubscriptionStatusResponse;
import com.travelplanner.payment.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 구독 결제 컨트롤러.
 *
 * <p>구독 플랜 조회, IAP 결제 처리, 구독 상태 조회를 담당한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class PayController {

    private final SubscriptionService subscriptionService;

    /**
     * 구독 플랜 목록을 조회한다.
     *
     * @return 구독 플랜 목록
     */
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<SubscriptionPlansResponse>> getSubscriptionPlans() {
        log.debug("구독 플랜 목록 조회");
        SubscriptionPlansResponse response = SubscriptionPlansResponse.of(
                subscriptionService.getSubscriptionPlans());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * IAP 영수증 검증 후 구독을 구매 처리한다.
     *
     * @param principal 인증된 사용자
     * @param request   구매 요청 (planId, receipt, provider)
     * @return 구매 결과 및 새 액세스 토큰 (201 Created)
     */
    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<PurchaseResponse>> purchaseSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PurchaseRequest request) {

        // Phase 1: 인증 미적용 — userId 하드코딩
        String userId = (principal != null) ? principal.getUserId() : "user-test-001";

        PurchaseResult result = subscriptionService.purchaseSubscription(
                userId, request.getPlanId(), request.getReceipt(), request.getProvider());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(PurchaseResponse.of(result)));
    }

    /**
     * 현재 사용자의 구독 상태를 조회한다.
     *
     * @param principal 인증된 사용자
     * @return 구독 상태 (tier, status, 만료일 등)
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> getSubscriptionStatus(
            @AuthenticationPrincipal UserPrincipal principal) {

        // Phase 1: 인증 미적용 — userId 하드코딩
        String userId = (principal != null) ? principal.getUserId() : "user-test-001";

        SubscriptionStatus status = subscriptionService.getSubscriptionStatus(userId);
        return ResponseEntity.ok(ApiResponse.ok(SubscriptionStatusResponse.from(status)));
    }
}
