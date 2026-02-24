-- MONITOR 서비스 테스트 초기 데이터
-- 멱등성 보장: TRUNCATE → INSERT 순서

TRUNCATE TABLE place_statuses;

-- 테스트 장소 상태
INSERT INTO place_statuses (id, place_id, place_name, overall_status, business_status, congestion_status, congestion_value, congestion_is_unknown, weather_status, weather_value, precipitation_prob, walking_minutes, transit_minutes, distance_m, reason, updated_at)
VALUES
    ('ps_test001', 'place_abc123', '이치란 라멘 시부야', 'GREEN', 'NORMAL', 'NORMAL', '보통', FALSE, 'NORMAL', '맑음', 10, 15, NULL, 420, NULL, NOW()),
    ('ps_test002', 'place_def456', '시부야 스크램블 교차로', 'YELLOW', 'NORMAL', 'WARNING', '혼잡', FALSE, 'NORMAL', '맑음', 15, 20, 8, 620, '혼잡도 높음', NOW()),
    ('ps_test003', 'place_xyz789', '후쿠로쿠 라멘', 'GREY', 'NORMAL', 'NORMAL', '보통', TRUE, 'NORMAL', '맑음', 5, 5, NULL, 320, '데이터 미확인', NOW());
