-- PAYMENT 서비스 테스트 초기 데이터
-- 멱등성 보장: TRUNCATE → INSERT 순서

TRUNCATE TABLE payment_records;
TRUNCATE TABLE subscriptions;

-- 테스트 구독 정보
INSERT INTO subscriptions (id, user_id, plan_id, tier, status, provider, started_at, expires_at, created_at, updated_at)
VALUES
    ('sub_test001', 'usr_test001', 'plan_trip_pass', 'TRIP_PASS', 'ACTIVE', 'apple', NOW(), NULL, NOW(), NOW()),
    ('sub_test002', 'usr_test003', 'plan_pro', 'PRO', 'CANCELLING', 'google', DATEADD('MONTH', -1, NOW()), DATEADD('MONTH', 1, NOW()), NOW(), NOW());

-- 테스트 결제 이력
INSERT INTO payment_records (id, user_id, subscription_id, plan_id, amount, currency, provider, receipt_hash, status, created_at)
VALUES
    ('pr_test001', 'usr_test001', 'sub_test001', 'plan_trip_pass', 4900, 'KRW', 'apple', 'hash_apple_receipt_001', 'SUCCESS', NOW()),
    ('pr_test002', 'usr_test003', 'sub_test002', 'plan_pro', 9900, 'KRW', 'google', 'hash_google_receipt_002', 'SUCCESS', NOW());
