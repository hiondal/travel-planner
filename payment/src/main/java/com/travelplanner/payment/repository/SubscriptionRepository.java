package com.travelplanner.payment.repository;

import com.travelplanner.payment.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 구독 JPA 리포지토리.
 */
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    Optional<Subscription> findByUserId(String userId);

    Optional<Subscription> findByUserIdAndStatus(String userId, String status);
}
