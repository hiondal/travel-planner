package com.travelplanner.payment.service;

import com.travelplanner.payment.domain.SubscriptionPlan;
import com.travelplanner.payment.dto.internal.PurchaseResult;
import com.travelplanner.payment.dto.internal.SubscriptionStatus;

import java.util.List;

/**
 * 구독 서비스 인터페이스.
 */
public interface SubscriptionService {

    List<SubscriptionPlan> getSubscriptionPlans();

    PurchaseResult purchaseSubscription(String userId, String planId, String receipt, String provider);

    SubscriptionStatus getSubscriptionStatus(String userId);
}
