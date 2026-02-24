package com.travelplanner.payment.dto.internal;

import com.travelplanner.payment.domain.Subscription;
import lombok.Getter;

import java.util.List;

/**
 * 구독 구매 서비스 내부 결과.
 */
@Getter
public class PurchaseResult {

    private final Subscription subscription;
    private final String newAccessToken;
    private final List<String> activatedFeatures;

    public PurchaseResult(Subscription subscription, String newAccessToken, List<String> activatedFeatures) {
        this.subscription = subscription;
        this.newAccessToken = newAccessToken;
        this.activatedFeatures = activatedFeatures;
    }
}
