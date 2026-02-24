-- ALTERNATIVE 서비스 테스트 초기 데이터
-- 멱등성 보장: TRUNCATE → INSERT 순서

TRUNCATE TABLE alternative_selections;
TRUNCATE TABLE alternative_card_snapshots;

-- 테스트 대안 카드 스냅샷
INSERT INTO alternative_card_snapshots (id, original_place_id, place_id, place_name, distance_m, rating, congestion, reason, status_label, lat, lng, walking_minutes, transit_minutes, created_at)
VALUES
    ('alt_test001', 'place_abc123', 'place_xyz789', '후쿠로쿠 라멘', 320, 4.0, '낮음', '근거리 영업 중 동일 카테고리', NULL, 35.6621000, 139.6982000, 5, NULL, NOW()),
    ('alt_test002', 'place_abc123', 'place_ghi012', '멘야 무사시', 650, 4.3, '보통', '높은 평점', NULL, 35.6571000, 139.7031000, 9, 4, NOW()),
    ('alt_test003', 'place_abc123', 'place_jkl345', '라멘 산쿠로', 820, 3.8, '낮음', '혼잡도 낮음', '주의 필요', 35.6610000, 139.7051000, 12, 5, NOW());
