# PAYMENT 서비스 - 데이터 설계서

## 데이터설계 요약

| 항목 | 내용 |
|------|------|
| 서비스 | PAYMENT (구독 결제) |
| DBMS | PostgreSQL |
| 테이블 수 | 2개 (subscriptions, payment_records) |
| Redis DB | DB 7 (PAY 전용: 구독 상태 캐시) |
| 서비스 간 FK | 없음 (데이터 독립성 원칙) |

---

## 1. 개요

인앱 결제(Apple/Google IAP)를 통한 구독 관리와 결제 이력을 담당한다.
구독 완료 후 auth 서비스의 사용자 tier 업데이트를 트리거한다.
구독 상태는 Redis에 캐시하여 briefing/alternative 서비스의 paywall 확인에 활용한다.

---

## 2. 테이블 정의

### 2.1 subscriptions

사용자 구독 현황. 사용자당 활성 구독은 1개.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| user_id | VARCHAR(36) | NOT NULL, UNIQUE | 사용자 ID (auth 서비스 참조, FK 없음) |
| plan_id | VARCHAR(50) | NOT NULL | 구독 플랜 ID |
| tier | VARCHAR(20) | NOT NULL | 구독 등급 (FREE, TRIP_PASS, PRO) |
| status | VARCHAR(20) | NOT NULL | 구독 상태 (ACTIVE, CANCELLED, CANCELLING) |
| provider | VARCHAR(20) | NOT NULL | 결제 프로바이더 (APPLE, GOOGLE) |
| transaction_id | VARCHAR(200) | | IAP 트랜잭션 ID |
| started_at | TIMESTAMPTZ | NOT NULL | 구독 시작 일시 |
| expires_at | TIMESTAMPTZ | | 구독 만료 일시 |
| created_at | TIMESTAMPTZ | NOT NULL | 레코드 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL | 레코드 수정 일시 |

인덱스:
- `idx_subscriptions_user_id` (user_id) UNIQUE
- `idx_subscriptions_status` (status)
- `idx_subscriptions_expires_at` (expires_at) — 만료 배치 처리용

체크 제약:
- `chk_subscriptions_tier`: tier IN ('FREE', 'TRIP_PASS', 'PRO')
- `chk_subscriptions_status`: status IN ('ACTIVE', 'CANCELLED', 'CANCELLING')
- `chk_subscriptions_provider`: provider IN ('APPLE', 'GOOGLE')

### 2.2 payment_records

결제 트랜잭션 이력. 불변 레코드 (insert-only).

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| subscription_id | VARCHAR(36) | NOT NULL, FK → subscriptions(id) | 구독 ID |
| user_id | VARCHAR(36) | NOT NULL | 사용자 ID |
| transaction_id | VARCHAR(200) | NOT NULL, UNIQUE | IAP 트랜잭션 ID (중복 결제 방지) |
| amount | INTEGER | NOT NULL | 결제 금액 (최소 단위, 예: 센트) |
| currency | VARCHAR(10) | NOT NULL | 통화 코드 (USD, KRW 등) |
| provider | VARCHAR(20) | NOT NULL | 결제 프로바이더 |
| created_at | TIMESTAMPTZ | NOT NULL | 결제 일시 |

인덱스:
- `idx_payment_records_user_id` (user_id)
- `idx_payment_records_subscription_id` (subscription_id)
- `idx_payment_records_transaction_id` (transaction_id) UNIQUE
- `idx_payment_records_created_at` (created_at)

외래키:
- `fk_payment_records_subscription_id` → subscriptions(id)

체크 제약:
- `chk_payment_records_amount`: amount > 0

---

## 3. Redis 캐시 설계

### DB 7 — PAY 전용

| 키 패턴 | 설명 | TTL | 데이터 타입 |
|---------|------|-----|-------------|
| `pay:subscription:{userId}` | 구독 상태 캐시 (tier, status, expiresAt) | 5분 | Hash |

Hash 필드 (`pay:subscription:{userId}`):
- `tier`: FREE/TRIP_PASS/PRO
- `status`: ACTIVE/CANCELLED/CANCELLING
- `subscriptionId`: 구독 ID
- `expiresAt`: ISO 8601 만료 일시

캐시 무효화:
- 구독 구매/취소/상태 변경 시 `pay:subscription:{userId}` 삭제 (즉시 갱신)
- TTL 5분으로 설정하여 만료 후 자동 갱신

---

## 4. 데이터 흐름

```
구독 구매:
  PurchaseRequest (planId, receipt, provider)
  → IapVerificationClient.verify() → VerificationResult
  → 영수증 유효성 확인 (valid=true)
  → transaction_id 중복 확인 (payment_records)
  → subscriptions upsert (user당 1개 활성 구독)
  → payment_records insert
  → pay:subscription:{userId} 캐시 갱신
  → AuthServiceClient.invalidateAndReissueToken() 호출 (tier 업데이트)

구독 상태 조회:
  GET /subscription/status
  → Redis DB7 Cache-Aside (pay:subscription:{userId})
  → miss 시 subscriptions 테이블 조회
  → 캐시 등록 (5분 TTL)
```

---

## 5. 설계 결정 사항

- `subscriptions`는 user_id UNIQUE로 사용자당 1개 활성 구독만 허용
- `payment_records`는 insert-only (불변). transaction_id UNIQUE로 중복 결제 방지
- 구독 완료 후 auth 서비스 API를 직접 호출하여 tier 업데이트 (이벤트 대신 동기 호출 — 결제 원자성 보장)
- `SubscriptionPlan`은 DB 테이블이 아닌 코드/설정 파일로 관리 (클래스 설계서 기반, 자주 변경되지 않음)
