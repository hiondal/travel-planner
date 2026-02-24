package com.travelplanner.payment.service;

import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.common.exception.BusinessException;
import com.travelplanner.payment.client.AuthServiceClient;
import com.travelplanner.payment.client.IapVerificationClient;
import com.travelplanner.payment.domain.*;
import com.travelplanner.payment.dto.internal.PurchaseResult;
import com.travelplanner.payment.dto.internal.SubscriptionStatus;
import com.travelplanner.payment.repository.PaymentRecordRepository;
import com.travelplanner.payment.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

/**
 * 구독 서비스 구현체.
 *
 * <p>구독 플랜 조회, IAP 검증 및 구독 관리를 담당한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final IapVerificationClient iapVerificationClient;
    private final AuthServiceClient authServiceClient;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_SUBSCRIPTION_PREFIX = "pay:subscription:";

    // 플랜 정보 (코드에서 관리)
    private static final List<SubscriptionPlan> PLANS = List.of(
            new SubscriptionPlan("plan_trip_pass", "Trip Pass", SubscriptionTier.TRIP_PASS,
                    4900, "KRW", "1회",
                    List.of("대안 카드 기능 이용", "무제한 브리핑"),
                    "com.travel-planner.trippass", "travel_planner_trippass"),
            new SubscriptionPlan("plan_pro", "Pro", SubscriptionTier.PRO,
                    9900, "KRW", "월",
                    List.of("대안 카드 기능 이용", "무제한 브리핑", "AI 컨시어지 가이드", "우선 지원"),
                    "com.travel-planner.pro.monthly", "travel_planner_pro_monthly")
    );

    @Override
    public List<SubscriptionPlan> getSubscriptionPlans() {
        return PLANS;
    }

    @Override
    @Transactional
    public PurchaseResult purchaseSubscription(String userId, String planId, String receipt, String provider) {
        log.info("구독 구매 요청: userId={}, planId={}, provider={}", userId, planId, provider);

        SubscriptionPlan plan = findPlan(planId);
        String productId = "apple".equals(provider) ? plan.getAppleProductId() : plan.getGoogleProductId();

        // IAP 검증
        VerificationResult verificationResult = iapVerificationClient.verify(provider, receipt, productId);
        if (!verificationResult.isValid()) {
            throw new BusinessException("PAYMENT_VERIFICATION_FAILED",
                    "결제에 실패했습니다. 다시 시도해주세요.", 402);
        }

        // 중복 결제 방지
        if (paymentRecordRepository.findByTransactionId(verificationResult.getTransactionId()).isPresent()) {
            throw new BusinessException("DUPLICATE_TRANSACTION", "이미 처리된 결제입니다.", 409);
        }

        // 구독 upsert
        SubscriptionTier newTier = resolveNewTier(planId);
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> Subscription.create(UUID.randomUUID().toString(), userId, planId,
                        newTier, provider.toUpperCase(), verificationResult.getTransactionId()));

        subscription.updateTierAndTransaction(newTier, verificationResult.getTransactionId());
        subscriptionRepository.save(subscription);

        // 결제 이력 저장
        PaymentRecord record = PaymentRecord.create(UUID.randomUUID().toString(),
                subscription.getId(), userId, verificationResult.getTransactionId(),
                plan.getAmount(), plan.getCurrency(), provider.toUpperCase());
        paymentRecordRepository.save(record);

        // Redis 캐시 갱신
        invalidateSubscriptionCache(userId);

        // AUTH 서비스 토큰 재발급
        String newAccessToken = authServiceClient.invalidateAndReissueToken(userId, newTier.name());

        List<String> activatedFeatures = getActivatedFeatures(newTier);

        log.info("구독 구매 완료: userId={}, tier={}, subscriptionId={}", userId, newTier, subscription.getId());
        return new PurchaseResult(subscription, newAccessToken, activatedFeatures);
    }

    @Override
    public SubscriptionStatus getSubscriptionStatus(String userId) {
        String cacheKey = CACHE_SUBSCRIPTION_PREFIX + userId;

        // DB 조회 (정확성 보장)
        Optional<Subscription> subOptional = subscriptionRepository.findByUserId(userId);
        if (subOptional.isEmpty()) {
            cacheSubscriptionStatus(cacheKey, SubscriptionTier.FREE, SubscriptionStatusEnum.ACTIVE, null);
            return SubscriptionStatus.free();
        }

        Subscription sub = subOptional.get();
        cacheSubscriptionStatus(cacheKey, sub.getTier(), sub.getStatus(), sub.getId());

        return new SubscriptionStatus(sub.getTier(), sub.getStatus(), sub.getId(),
                sub.getStartedAt(), sub.getExpiresAt());
    }

    private SubscriptionPlan findPlan(String planId) {
        return PLANS.stream()
                .filter(p -> p.getPlanId().equals(planId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("INVALID_PLAN_ID",
                        "유효하지 않은 플랜 ID입니다: " + planId, 400));
    }

    private SubscriptionTier resolveNewTier(String planId) {
        return findPlan(planId).getTier();
    }

    private List<String> getActivatedFeatures(SubscriptionTier tier) {
        return switch (tier) {
            case TRIP_PASS -> List.of("대안 카드 기능 이용", "무제한 브리핑");
            case PRO -> List.of("대안 카드 기능 이용", "무제한 브리핑", "AI 컨시어지 가이드", "우선 지원");
            default -> List.of();
        };
    }

    private void invalidateSubscriptionCache(String userId) {
        String cacheKey = CACHE_SUBSCRIPTION_PREFIX + userId;
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.warn("구독 캐시 삭제 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

    private void cacheSubscriptionStatus(String cacheKey, SubscriptionTier tier,
                                          SubscriptionStatusEnum status, String subscriptionId) {
        try {
            Map<String, String> hash = new HashMap<>();
            hash.put("tier", tier.name());
            hash.put("status", status.name());
            if (subscriptionId != null) {
                hash.put("subscriptionId", subscriptionId);
            }
            redisTemplate.opsForHash().putAll(cacheKey, hash);
            redisTemplate.expire(cacheKey, Duration.ofMinutes(5));
        } catch (Exception e) {
            log.warn("구독 캐시 저장 실패: userId 캐시 키={}, error={}", cacheKey, e.getMessage());
        }
    }
}
