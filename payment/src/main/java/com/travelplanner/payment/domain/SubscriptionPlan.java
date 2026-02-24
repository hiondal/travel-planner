package com.travelplanner.payment.domain;

import com.travelplanner.common.enums.SubscriptionTier;
import lombok.Getter;

import java.util.List;

/**
 * 구독 플랜 (코드에서 관리, DB 테이블 없음).
 */
@Getter
public class SubscriptionPlan {

    private final String planId;
    private final String name;
    private final SubscriptionTier tier;
    private final int amount;
    private final String currency;
    private final String period;
    private final List<String> features;
    private final String appleProductId;
    private final String googleProductId;

    public SubscriptionPlan(String planId, String name, SubscriptionTier tier,
                             int amount, String currency, String period,
                             List<String> features, String appleProductId, String googleProductId) {
        this.planId = planId;
        this.name = name;
        this.tier = tier;
        this.amount = amount;
        this.currency = currency;
        this.period = period;
        this.features = features;
        this.appleProductId = appleProductId;
        this.googleProductId = googleProductId;
    }
}
