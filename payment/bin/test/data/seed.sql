-- PAYMENT 서비스 테스트 초기 데이터
-- 멱등성 보장: TRUNCATE → INSERT 순서
-- 실제 subscriptions 컬럼: id, user_id, plan_id, tier, status, provider, started_at, expires_at, transaction_id, created_at, updated_at
-- 실제 payment_records 컬럼: id, user_id, subscription_id, amount, currency, provider, transaction_id, created_at

TRUNCATE TABLE payment_records;
TRUNCATE TABLE subscriptions;

-- 테스트 구독 정보
INSERT INTO subscriptions (id, user_id, plan_id, tier, status, provider, transaction_id, started_at, expires_at, created_at, updated_at)
VALUES
    ('sub_test001', 'usr_test001', 'plan_trip_pass', 'TRIP_PASS', 'ACTIVE', 'apple', 'txn_apple_001', NOW(), NULL, NOW(), NOW()),
    ('sub_test002', 'usr_test003', 'plan_pro', 'PRO', 'CANCELLING', 'google', 'txn_google_002', NOW() - INTERVAL '1 month', NOW() + INTERVAL '1 month', NOW(), NOW());

-- 테스트 결제 이력
INSERT INTO payment_records (id, user_id, subscription_id, amount, currency, provider, transaction_id, created_at)
VALUES
    ('pr_test001', 'usr_test001', 'sub_test001', 4900, 'KRW', 'apple', 'txn_apple_001', NOW()),
    ('pr_test002', 'usr_test003', 'sub_test002', 9900, 'KRW', 'google', 'txn_google_002', NOW());
