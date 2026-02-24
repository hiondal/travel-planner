package com.travelplanner.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.payment.dto.internal.SubscriptionStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 구독 상태 응답 DTO.
 */
@Getter
public class SubscriptionStatusResponse {

    @JsonProperty("tier")
    private final String tier;

    @JsonProperty("status")
    private final String status;

    @JsonProperty("subscription_id")
    private final String subscriptionId;

    @JsonProperty("started_at")
    private final LocalDateTime startedAt;

    @JsonProperty("expires_at")
    private final LocalDateTime expiresAt;

    private SubscriptionStatusResponse(SubscriptionStatus subscriptionStatus) {
        this.tier = subscriptionStatus.getTier().name();
        this.status = subscriptionStatus.getStatus().name();
        this.subscriptionId = subscriptionStatus.getSubscriptionId();
        this.startedAt = subscriptionStatus.getStartedAt();
        this.expiresAt = subscriptionStatus.getExpiresAt();
    }

    public static SubscriptionStatusResponse from(SubscriptionStatus subscriptionStatus) {
        return new SubscriptionStatusResponse(subscriptionStatus);
    }
}
