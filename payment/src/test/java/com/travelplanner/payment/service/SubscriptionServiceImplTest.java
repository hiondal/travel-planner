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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * SubscriptionServiceImpl 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private IapVerificationClient iapVerificationClient;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private static final String USER_ID = "user-001";
    private static final String PLAN_PRO = "plan_pro";
    private static final String PLAN_TRIP_PASS = "plan_trip_pass";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(hashOperations.get(anyString(), anyString())).thenReturn(null);
    }

    // ========================== getSubscriptionPlans ==========================

    @Test
    @DisplayName("구독 플랜 목록 조회 - 2개 플랜 반환")
    void getSubscriptionPlans_returnsTwoPlans() {
        List<SubscriptionPlan> plans = subscriptionService.getSubscriptionPlans();

        assertThat(plans).hasSize(2);
        assertThat(plans).extracting(SubscriptionPlan::getPlanId)
                .containsExactly(PLAN_TRIP_PASS, PLAN_PRO);
    }

    // ========================== purchaseSubscription ==========================

    @Test
    @DisplayName("구독 구매 성공 - PRO 플랜 신규 구독")
    void purchaseSubscription_success_newSubscription() {
        // Given
        String receipt = "receipt-data-test";
        String provider = "apple";
        VerificationResult verificationResult = VerificationResult.success("txn_abc123", "com.travel-planner.pro.monthly");

        given(iapVerificationClient.verify(eq(provider), eq(receipt), anyString()))
                .willReturn(verificationResult);
        given(paymentRecordRepository.findByTransactionId("txn_abc123"))
                .willReturn(Optional.empty());
        given(subscriptionRepository.findByUserId(USER_ID))
                .willReturn(Optional.empty());
        given(subscriptionRepository.save(any(Subscription.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(paymentRecordRepository.save(any(PaymentRecord.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(authServiceClient.invalidateAndReissueToken(eq(USER_ID), eq("PRO")))
                .willReturn("new-access-token-xyz");
        given(redisTemplate.delete(anyString())).willReturn(true);

        // When
        PurchaseResult result = subscriptionService.purchaseSubscription(
                USER_ID, PLAN_PRO, receipt, provider);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSubscription().getTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(result.getNewAccessToken()).isEqualTo("new-access-token-xyz");
        assertThat(result.getActivatedFeatures()).contains("AI 컨시어지 가이드");

        then(subscriptionRepository).should().save(any(Subscription.class));
        then(paymentRecordRepository).should().save(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("구독 구매 성공 - 기존 구독 업그레이드")
    void purchaseSubscription_success_upgradeExisting() {
        // Given
        String receipt = "receipt-pro";
        String provider = "google";
        VerificationResult verificationResult = VerificationResult.success("txn_upgrade_001", "travel_planner_pro_monthly");

        Subscription existingSub = Subscription.create("sub-001", USER_ID, PLAN_TRIP_PASS,
                SubscriptionTier.TRIP_PASS, "APPLE", "txn_old");

        given(iapVerificationClient.verify(eq(provider), eq(receipt), anyString()))
                .willReturn(verificationResult);
        given(paymentRecordRepository.findByTransactionId("txn_upgrade_001"))
                .willReturn(Optional.empty());
        given(subscriptionRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(existingSub));
        given(subscriptionRepository.save(any(Subscription.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(paymentRecordRepository.save(any(PaymentRecord.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(authServiceClient.invalidateAndReissueToken(eq(USER_ID), eq("PRO")))
                .willReturn("upgraded-token");
        given(redisTemplate.delete(anyString())).willReturn(true);

        // When
        PurchaseResult result = subscriptionService.purchaseSubscription(
                USER_ID, PLAN_PRO, receipt, provider);

        // Then
        assertThat(result.getSubscription().getTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(result.getSubscription().getId()).isEqualTo("sub-001");
    }

    @Test
    @DisplayName("구독 구매 실패 - IAP 검증 실패")
    void purchaseSubscription_fail_verificationFailed() {
        // Given
        VerificationResult failResult = VerificationResult.failure("INVALID_RECEIPT");

        given(iapVerificationClient.verify(anyString(), anyString(), anyString()))
                .willReturn(failResult);

        // When & Then
        assertThatThrownBy(() -> subscriptionService.purchaseSubscription(
                USER_ID, PLAN_PRO, "bad-receipt", "apple"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("결제에 실패했습니다");
    }

    @Test
    @DisplayName("구독 구매 실패 - 중복 트랜잭션")
    void purchaseSubscription_fail_duplicateTransaction() {
        // Given
        VerificationResult verificationResult = VerificationResult.success("txn_dup_001", "com.travel-planner.pro.monthly");
        PaymentRecord existingRecord = PaymentRecord.create("rec-001", "sub-001",
                USER_ID, "txn_dup_001", 9900, "KRW", "APPLE");

        given(iapVerificationClient.verify(anyString(), anyString(), anyString()))
                .willReturn(verificationResult);
        given(paymentRecordRepository.findByTransactionId("txn_dup_001"))
                .willReturn(Optional.of(existingRecord));

        // When & Then
        assertThatThrownBy(() -> subscriptionService.purchaseSubscription(
                USER_ID, PLAN_PRO, "receipt-dup", "apple"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 처리된 결제");
    }

    @Test
    @DisplayName("구독 구매 실패 - 유효하지 않은 플랜 ID")
    void purchaseSubscription_fail_invalidPlanId() {
        // Given: findPlan이 가장 먼저 실행되므로 외부 클라이언트 스텁 불필요

        // When & Then
        assertThatThrownBy(() -> subscriptionService.purchaseSubscription(
                USER_ID, "plan_invalid_xyz", "receipt", "apple"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 플랜 ID");
    }

    // ========================== getSubscriptionStatus ==========================

    @Test
    @DisplayName("구독 상태 조회 - 캐시 히트")
    void getSubscriptionStatus_cacheHit() {
        // Given
        given(hashOperations.get(anyString(), eq("tier"))).willReturn("PRO");
        given(hashOperations.get(anyString(), eq("status"))).willReturn("ACTIVE");
        given(hashOperations.get(anyString(), eq("subscriptionId"))).willReturn("sub-cached-001");

        // When
        SubscriptionStatus status = subscriptionService.getSubscriptionStatus(USER_ID);

        // Then
        assertThat(status.getTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(status.getStatus()).isEqualTo(SubscriptionStatusEnum.ACTIVE);
        assertThat(status.getSubscriptionId()).isEqualTo("sub-cached-001");

        then(subscriptionRepository).should(never()).findByUserId(any());
    }

    @Test
    @DisplayName("구독 상태 조회 - DB 조회 (캐시 미스, 구독 존재)")
    void getSubscriptionStatus_dbHit_subscriptionExists() {
        // Given
        given(hashOperations.get(anyString(), anyString())).willReturn(null);

        Subscription sub = Subscription.create("sub-db-001", USER_ID, PLAN_PRO,
                SubscriptionTier.PRO, "APPLE", "txn_db_001");
        given(subscriptionRepository.findByUserId(USER_ID)).willReturn(Optional.of(sub));
        given(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).willReturn(true);
        doNothing().when(hashOperations).putAll(anyString(), anyMap());

        // When
        SubscriptionStatus status = subscriptionService.getSubscriptionStatus(USER_ID);

        // Then
        assertThat(status.getTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(status.getSubscriptionId()).isEqualTo("sub-db-001");
    }

    @Test
    @DisplayName("구독 상태 조회 - FREE 반환 (구독 없음)")
    void getSubscriptionStatus_free_noSubscription() {
        // Given
        given(hashOperations.get(anyString(), anyString())).willReturn(null);
        given(subscriptionRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        doNothing().when(hashOperations).putAll(anyString(), anyMap());
        given(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).willReturn(true);

        // When
        SubscriptionStatus status = subscriptionService.getSubscriptionStatus(USER_ID);

        // Then
        assertThat(status.getTier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(status.getSubscriptionId()).isNull();
    }
}
