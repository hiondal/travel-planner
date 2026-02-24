package com.travelplanner.payment.domain;

import com.travelplanner.common.enums.SubscriptionTier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 구독 엔티티.
 *
 * <p>사용자 구독 현황 (사용자당 활성 구독 1개).</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(name = "subscriptions",
        indexes = {
                @Index(name = "idx_subscriptions_user_id", columnList = "userId", unique = true),
                @Index(name = "idx_subscriptions_status", columnList = "status"),
                @Index(name = "idx_subscriptions_expires_at", columnList = "expiresAt")
        })
@Getter
@NoArgsConstructor
public class Subscription {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    private String userId;

    @Column(name = "plan_id", nullable = false, length = 50)
    private String planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private SubscriptionTier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatusEnum status;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "transaction_id", length = 200)
    private String transactionId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Subscription(String userId, String planId, SubscriptionTier tier, String provider) {
        this.userId = userId;
        this.planId = planId;
        this.tier = tier;
        this.provider = provider;
        this.status = SubscriptionStatusEnum.ACTIVE;
    }

    public static Subscription create(String id, String userId, String planId,
                                       SubscriptionTier tier, String provider, String transactionId) {
        Subscription sub = new Subscription(userId, planId, tier, provider);
        sub.id = id;
        sub.transactionId = transactionId;
        sub.startedAt = LocalDateTime.now();
        sub.createdAt = LocalDateTime.now();
        sub.updatedAt = LocalDateTime.now();
        return sub;
    }

    public void activate() {
        this.status = SubscriptionStatusEnum.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = SubscriptionStatusEnum.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == SubscriptionStatusEnum.ACTIVE;
    }

    public boolean isCancelling() {
        return status == SubscriptionStatusEnum.CANCELLING;
    }

    public void updateTierAndTransaction(SubscriptionTier tier, String transactionId) {
        this.tier = tier;
        this.transactionId = transactionId;
        this.startedAt = LocalDateTime.now();
        this.status = SubscriptionStatusEnum.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
}
