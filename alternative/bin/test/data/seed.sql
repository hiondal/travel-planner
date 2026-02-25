-- ALTERNATIVE 서비스 테스트 초기 데이터
-- 멱등성 보장: TRUNCATE → INSERT 순서
-- 실제 테이블: alternatives, alternative_card_snapshots, selection_logs
-- alternative_card_snapshots 실제 컬럼: id, candidate_place_id, place_id, score_weights, scores, user_id, exposed_at

TRUNCATE TABLE selection_logs;
TRUNCATE TABLE alternative_card_snapshots;
TRUNCATE TABLE alternatives;

-- 테스트 대안 (alternatives 테이블)
INSERT INTO alternatives (id, user_id, original_place_id, place_id, name, lat, lng, distance_m, score, rating, congestion, reason, status_label, walking_minutes, transit_minutes, created_at)
VALUES
    ('alt_test001', 'usr_test001', 'place_abc123', 'place_xyz789', '후쿠로쿠 라멘', 35.6621000, 139.6982000, 320, 0.85, 4.0, '낮음', '근거리 영업 중 동일 카테고리', NULL, 5, NULL, NOW()),
    ('alt_test002', 'usr_test001', 'place_abc123', 'place_ghi012', '멘야 무사시', 35.6571000, 139.7031000, 650, 0.78, 4.3, '보통', '높은 평점', NULL, 9, 4, NOW()),
    ('alt_test003', 'usr_test001', 'place_abc123', 'place_jkl345', '라멘 산쿠로', 35.6610000, 139.7051000, 820, 0.65, 3.8, '낮음', '혼잡도 낮음', '주의 필요', 12, 5, NOW());

-- 테스트 대안 카드 스냅샷 (노출 기록)
INSERT INTO alternative_card_snapshots (id, user_id, place_id, candidate_place_id, score_weights, scores, exposed_at)
VALUES
    ('acs_test001', 'usr_test001', 'place_abc123', 'place_xyz789', '{"distance":0.4,"rating":0.3,"congestion":0.3}', '{"distance":0.9,"rating":0.8,"congestion":0.85}', NOW()),
    ('acs_test002', 'usr_test001', 'place_abc123', 'place_ghi012', '{"distance":0.4,"rating":0.3,"congestion":0.3}', '{"distance":0.7,"rating":0.95,"congestion":0.7}', NOW()),
    ('acs_test003', 'usr_test001', 'place_abc123', 'place_jkl345', '{"distance":0.4,"rating":0.3,"congestion":0.3}', '{"distance":0.6,"rating":0.7,"congestion":0.6}', NOW());
