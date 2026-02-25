-- MONITOR 서비스 테스트 초기 데이터
-- 멱등성 보장: TRUNCATE → INSERT 순서
-- 실제 테이블: monitoring_targets, collected_data, status_history

TRUNCATE TABLE status_history;
TRUNCATE TABLE collected_data;
TRUNCATE TABLE monitoring_targets;

-- 테스트 모니터링 대상
INSERT INTO monitoring_targets (id, user_id, trip_id, schedule_item_id, place_id, lat, lng, category, visit_datetime, current_status, current_status_updated_at, consecutive_failure_count, created_at)
VALUES
    ('mt_test001', 'usr_test001', 'trip_001', 'si_test001', 'place_abc123', 35.6595000, 139.7004000, '라멘', NOW() + INTERVAL '2 hours', 'GREEN', NOW(), 0, NOW()),
    ('mt_test002', 'usr_test001', 'trip_001', 'si_test002', 'place_def456', 35.6594000, 139.7005000, '관광지', NOW() + INTERVAL '4 hours', 'YELLOW', NOW(), 0, NOW());

-- 테스트 수집 데이터
INSERT INTO collected_data (id, place_id, business_status, congestion_level, weather_condition, precipitation_prob, walking_minutes, transit_minutes, distance_m, has_fallback, collected_at)
VALUES
    ('cd_test001', 'place_abc123', 'OPEN', 'LOW', '맑음', 10, 15, NULL, 420, FALSE, NOW()),
    ('cd_test002', 'place_def456', 'OPEN', 'HIGH', '맑음', 15, 20, 8, 620, FALSE, NOW());

-- 테스트 상태 이력
INSERT INTO status_history (id, place_id, schedule_item_id, status, confidence_score, reason, judgment_at)
VALUES
    ('sh_test001', 'place_abc123', 'si_test001', 'GREEN', 0.95, NULL, NOW()),
    ('sh_test002', 'place_def456', 'si_test002', 'YELLOW', 0.75, '혼잡도 높음', NOW());
