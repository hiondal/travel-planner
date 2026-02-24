package com.travelplanner.payment.domain;

import lombok.Getter;

/**
 * IAP 검증 결과.
 */
@Getter
public class VerificationResult {

    private final String transactionId;
    private final String productId;
    private final boolean valid;
    private final String failureReason;

    private VerificationResult(String transactionId, String productId, boolean valid, String failureReason) {
        this.transactionId = transactionId;
        this.productId = productId;
        this.valid = valid;
        this.failureReason = failureReason;
    }

    public static VerificationResult success(String transactionId, String productId) {
        return new VerificationResult(transactionId, productId, true, null);
    }

    public static VerificationResult failure(String reason) {
        return new VerificationResult(null, null, false, reason);
    }
}
