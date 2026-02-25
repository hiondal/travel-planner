-- AUTH 서비스 테스트 초기 데이터
-- 멱등성 보장: TRUNCATE → INSERT 순서

TRUNCATE TABLE consents;
TRUNCATE TABLE refresh_tokens;
TRUNCATE TABLE users;

-- 테스트 사용자
INSERT INTO users (id, provider, provider_id, email, nickname, avatar_url, tier, created_at, updated_at)
VALUES
    ('usr_test001', 'GOOGLE', 'google_sub_001', 'testuser@gmail.com', '테스트유저', 'https://avatar.test/001.jpg', 'FREE', NOW(), NOW()),
    ('usr_test002', 'APPLE', 'apple_sub_002', 'appleuser@icloud.com', '애플유저', 'https://avatar.test/002.jpg', 'TRIP_PASS', NOW(), NOW()),
    ('usr_test003', 'GOOGLE', 'google_sub_003', 'prouser@gmail.com', '프로유저', 'https://avatar.test/003.jpg', 'PRO', NOW(), NOW());

-- 테스트 Refresh Token (컬럼명: refresh_token)
INSERT INTO refresh_tokens (id, user_id, refresh_token, expires_at, created_at)
VALUES
    ('rt_test001', 'usr_test001', 'valid.refresh.token.001', NOW() + INTERVAL '30 days', NOW()),
    ('rt_test002', 'usr_test002', 'valid.refresh.token.002', NOW() + INTERVAL '30 days', NOW()),
    ('rt_test003', 'usr_test003', 'expired.refresh.token.003', NOW() - INTERVAL '1 day', NOW());

-- 테스트 동의 이력 (updated_at 컬럼 없음)
INSERT INTO consents (id, user_id, location, push, consented_at, created_at)
VALUES
    ('cns_test001', 'usr_test001', TRUE, TRUE, NOW(), NOW());
