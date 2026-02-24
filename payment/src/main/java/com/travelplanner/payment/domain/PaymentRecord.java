package com.travelplanner.payment.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 이력 엔티티 (insert-only).
 */
@Entity
@Table(name = "payment_records",
        indexes = {
                @Index(name = "idx_payment_records_user_id", columnList = "userId"),
                @Index(name = "idx_payment_records_subscription_id", columnList = "subscriptionId"),
                @Index(name = "idx_payment_records_transaction_id", columnList = "transactionId", unique = true)
        })
@Getter
@NoArgsConstructor
public class PaymentRecord {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "subscription_id", nullable = false, length = 36)
    private String subscriptionId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 200)
    private String transactionId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PaymentRecord(String subscriptionId, String userId, String transactionId,
                         int amount, String currency, String provider) {
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
        this.provider = provider;
    }

    public static PaymentRecord create(String id, String subscriptionId, String userId,
                                        String transactionId, int amount, String currency, String provider) {
        PaymentRecord record = new PaymentRecord(subscriptionId, userId, transactionId, amount, currency, provider);
        record.id = id;
        record.createdAt = LocalDateTime.now();
        return record;
    }
}
