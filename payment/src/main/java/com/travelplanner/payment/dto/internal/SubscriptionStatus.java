package com.travelplanner.payment.dto.internal;

import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.payment.domain.SubscriptionStatusEnum;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 구독 상태 서비스 내부 DTO.
 */
@Getter
public class SubscriptionStatus {

    private final SubscriptionTier tier;
    private final SubscriptionStatusEnum status;
    private final String subscriptionId;
    private final LocalDateTime startedAt;
    private final LocalDateTime expiresAt;

    public SubscriptionStatus(SubscriptionTier tier, SubscriptionStatusEnum status,
                               String subscriptionId, LocalDateTime startedAt, LocalDateTime expiresAt) {
        this.tier = tier;
        this.status = status;
        this.subscriptionId = subscriptionId;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }

    public static SubscriptionStatus free() {
        return new SubscriptionStatus(SubscriptionTier.FREE, SubscriptionStatusEnum.ACTIVE,
                null, null, null);
    }
}
