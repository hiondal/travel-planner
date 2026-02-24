package com.travelplanner.payment.client;

import com.travelplanner.payment.domain.VerificationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * IAP 영수증 검증 클라이언트.
 *
 * <p>Phase 1: Mock 검증 (항상 성공 반환).</p>
 * <p>Phase 2: 실제 Apple/Google IAP 검증 연동 예정.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Slf4j
@Component
public class IapVerificationClient {

    /**
     * IAP 영수증을 검증한다.
     *
     * <p>Phase 1: 항상 성공 결과 반환.</p>
     *
     * @param provider  결제 제공자 (apple/google)
     * @param receipt   영수증 데이터
     * @param productId 제품 ID
     * @return 검증 결과 (Mock: 항상 성공)
     */
    public VerificationResult verify(String provider, String receipt, String productId) {
        log.info("[IAP-PHASE1] Mock 영수증 검증 (항상 성공): provider={}, productId={}", provider, productId);
        // Phase 2: 실제 IAP 검증 구현
        // if ("apple".equals(provider)) return verifyApple(receipt, productId);
        // if ("google".equals(provider)) return verifyGoogle(receipt, productId);
        String mockTransactionId = "txn_" + UUID.randomUUID().toString().substring(0, 8);
        return VerificationResult.success(mockTransactionId, productId);
    }

    public VerificationResult verifyApple(String receipt, String productId) {
        log.info("[IAP] Apple 영수증 검증: productId={}", productId);
        String mockTransactionId = "apple_txn_" + UUID.randomUUID().toString().substring(0, 8);
        return VerificationResult.success(mockTransactionId, productId);
    }

    public VerificationResult verifyGoogle(String receipt, String productId) {
        log.info("[IAP] Google 영수증 검증: productId={}", productId);
        String mockTransactionId = "google_txn_" + UUID.randomUUID().toString().substring(0, 8);
        return VerificationResult.success(mockTransactionId, productId);
    }
}
