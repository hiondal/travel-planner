package com.travelplanner.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.payment.domain.SubscriptionPlan;
import lombok.Getter;

import java.util.List;

/**
 * 구독 플랜 응답 DTO.
 */
@Getter
public class SubscriptionPlanDto {

    @JsonProperty("plan_id")
    private final String planId;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("tier")
    private final String tier;

    @JsonProperty("price")
    private final PriceDto price;

    @JsonProperty("features")
    private final List<String> features;

    @JsonProperty("apple_product_id")
    private final String appleProductId;

    @JsonProperty("google_product_id")
    private final String googleProductId;

    private SubscriptionPlanDto(SubscriptionPlan plan) {
        this.planId = plan.getPlanId();
        this.name = plan.getName();
        this.tier = plan.getTier().name();
        this.price = new PriceDto(plan.getAmount(), plan.getCurrency(), plan.getPeriod());
        this.features = plan.getFeatures();
        this.appleProductId = plan.getAppleProductId();
        this.googleProductId = plan.getGoogleProductId();
    }

    public static SubscriptionPlanDto from(SubscriptionPlan plan) {
        return new SubscriptionPlanDto(plan);
    }
}
