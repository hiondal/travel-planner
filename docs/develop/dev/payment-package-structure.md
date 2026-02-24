# PAY 서비스 — 패키지 구조도

## 개요

| 항목 | 값 |
|------|---|
| 서비스 ID | PAY |
| 모듈 경로 | `payment/` |
| 루트 패키지 | `com.travelplanner.payment` |
| 포트 | 8087 |
| Spring Boot 진입점 | `PaymentApplication` |
| DB | PostgreSQL `payment` 데이터베이스 |
| Redis | DB7 (구독 상태 캐시) |

---

## 패키지 트리

```
com.travelplanner.payment
│
├── PaymentApplication.java                        ← Spring Boot 진입점
│
├── config/
│   ├── SecurityConfig.java                        ← Phase 1: 전체 permitAll (Phase 3 JWT 적용 예정)
│   ├── RedisConfig.java                           ← Redis DB7 설정
│   └── AppConfig.java                             ← RestTemplate Bean
│
├── controller/
│   └── PayController.java                         ← 구독 결제 API
│       - GET  /api/v1/subscriptions/plans
│       - POST /api/v1/subscriptions/purchase
│       - GET  /api/v1/subscriptions/status
│
├── service/
│   ├── SubscriptionService.java                   ← 서비스 인터페이스
│   └── SubscriptionServiceImpl.java               ← 서비스 구현체
│       · PLANS 인메모리 관리 (plan_trip_pass: 4900 KRW, plan_pro: 9900 KRW)
│       · IAP 검증 → 중복 결제 방지 → 구독 upsert → 결제 이력 저장
│       · Redis 캐시 갱신 → AUTH 서비스 토큰 재발급
│
├── repository/
│   ├── SubscriptionRepository.java                ← Spring Data JPA (subscriptions 테이블)
│   │   · findByUserId, findByUserIdAndStatus
│   └── PaymentRecordRepository.java               ← Spring Data JPA (payment_records 테이블)
│       · findByTransactionId (중복 결제 방지)
│
├── domain/
│   ├── Subscription.java                          ← @Entity: subscriptions 테이블 (사용자당 1개)
│   │   · id(UUID), userId(unique), planId, tier(SubscriptionTier)
│   │   · status(SubscriptionStatusEnum), provider, transactionId
│   │   · startedAt, expiresAt, createdAt, updatedAt
│   ├── PaymentRecord.java                         ← @Entity: payment_records 테이블 (insert-only)
│   │   · id(UUID), subscriptionId, userId, transactionId(unique)
│   │   · amount, currency, provider, createdAt
│   ├── SubscriptionPlan.java                      ← 플랜 정보 (코드 관리, DB 테이블 없음)
│   │   · planId, name, tier, amount, currency, period, features
│   │   · appleProductId, googleProductId
│   ├── SubscriptionStatusEnum.java                ← 구독 상태 Enum: ACTIVE, CANCELLING, CANCELLED
│   └── VerificationResult.java                    ← IAP 검증 결과 (transactionId, productId, valid, failureReason)
│
├── client/
│   ├── IapVerificationClient.java                 ← IAP 영수증 검증 클라이언트
│   │   · Phase 1: Mock 항상 성공 (UUID 트랜잭션 ID 반환)
│   │   · Phase 2: 실제 Apple/Google IAP 검증 연동 예정
│   └── AuthServiceClient.java                     ← AUTH 서비스 토큰 재발급 클라이언트 (Mock 폴백)
│
└── dto/
    ├── request/
    │   └── PurchaseRequest.java                   ← planId, receipt, provider(@Pattern: apple|google)
    ├── response/
    │   ├── SubscriptionPlansResponse.java          ← 플랜 목록 응답
    │   ├── SubscriptionPlanDto.java                ← 개별 플랜 DTO (name, tier, price, features)
    │   ├── PriceDto.java                           ← 가격 DTO (amount, currency, period)
    │   ├── PurchaseResponse.java                   ← 구매 결과 응답 (subscriptionId, tier, newAccessToken, activatedFeatures)
    │   └── SubscriptionStatusResponse.java         ← 구독 상태 응답 (tier, status, startedAt, expiresAt)
    └── internal/
        ├── PurchaseResult.java                     ← 서비스 내부 구매 결과 (subscription, newAccessToken, activatedFeatures)
        └── SubscriptionStatus.java                 ← 서비스 내부 구독 상태 (tier, status, subscriptionId, startedAt, expiresAt)
```

---

## API 매핑

| 메서드 | 경로 | 인증 필요 | 설명 |
|--------|------|----------|------|
| GET | /api/v1/subscriptions/plans | 없음 | 구독 플랜 목록 조회 |
| POST | /api/v1/subscriptions/purchase | Phase 3 예정 | IAP 영수증 검증 및 구독 구매 (201 Created) |
| GET | /api/v1/subscriptions/status | Phase 3 예정 | 현재 구독 상태 조회 |

---

## Redis 키 패턴

| DB | 키 패턴 | TTL | 용도 |
|----|---------|-----|------|
| DB7 | `pay:subscription:{userId}` | 5분 | 구독 상태 캐시 (Hash: tier, status, subscriptionId) |

---

## 구독 플랜 (인메모리 관리)

| planId | 이름 | Tier | 금액 | 주기 | 주요 기능 |
|--------|------|------|------|------|----------|
| plan_trip_pass | Trip Pass | TRIP_PASS | 4,900 KRW | 1회 | 대안 카드 기능 이용, 무제한 브리핑 |
| plan_pro | Pro | PRO | 9,900 KRW | 월 | 대안 카드 기능 이용, 무제한 브리핑, AI 컨시어지 가이드, 우선 지원 |

---

## Phase 1 / Phase 2 전환 지점

| 컴포넌트 | Phase 1 | Phase 2 |
|---------|---------|---------|
| IapVerificationClient | Mock 항상 성공 | 실제 Apple Store Connect / Google Play API |
| AuthServiceClient | Mock 폴백 | AUTH 서비스 실제 HTTP 호출 |
| SecurityConfig | 전체 permitAll | JWT Bearer 인증 적용 |

---

## 의존 관계

- common 모듈: `ApiResponse`, `UserPrincipal`, `SubscriptionTier`, `BusinessException`, `GlobalExceptionHandler`
- 외부 서비스: AUTH 서비스 (http://localhost:8081)
- DB: PostgreSQL `payment` 데이터베이스 (subscriptions, payment_records)
- Cache: Redis DB7 (구독 상태 캐시, 5분 TTL)
