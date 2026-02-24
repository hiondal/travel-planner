package com.travelplanner.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.payment.dto.internal.PurchaseResult;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 구독 구매 응답 DTO.
 */
@Getter
public class PurchaseResponse {

    @JsonProperty("subscription_id")
    private final String subscriptionId;

    @JsonProperty("tier")
    private final String tier;

    @JsonProperty("status")
    private final String status;

    @JsonProperty("started_at")
    private final LocalDateTime startedAt;

    @JsonProperty("expires_at")
    private final LocalDateTime expiresAt;

    @JsonProperty("new_access_token")
    private final String newAccessToken;

    @JsonProperty("activated_features")
    private final List<String> activatedFeatures;

    private PurchaseResponse(PurchaseResult result) {
        this.subscriptionId = result.getSubscription().getId();
        this.tier = result.getSubscription().getTier().name();
        this.status = result.getSubscription().getStatus().name();
        this.startedAt = result.getSubscription().getStartedAt();
        this.expiresAt = result.getSubscription().getExpiresAt();
        this.newAccessToken = result.getNewAccessToken();
        this.activatedFeatures = result.getActivatedFeatures();
    }

    public static PurchaseResponse of(PurchaseResult result) {
        return new PurchaseResponse(result);
    }
}
