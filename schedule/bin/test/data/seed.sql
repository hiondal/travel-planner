-- SCHEDULE 서비스 테스트 초기 데이터
-- 멱등성 보장: TRUNCATE → INSERT 순서

TRUNCATE TABLE schedule_items;
TRUNCATE TABLE trips;

-- 테스트 여행 일정
INSERT INTO trips (id, user_id, name, start_date, end_date, city, status, created_at, updated_at)
VALUES
    ('trip_test001', 'usr_test001', '도쿄 3박4일', '2026-03-15', '2026-03-18', '도쿄', 'ACTIVE', NOW(), NOW()),
    ('trip_test002', 'usr_test001', '오사카 2박3일', '2026-04-01', '2026-04-03', '오사카', 'ACTIVE', NOW(), NOW()),
    ('trip_test003', 'usr_test002', '파리 5박6일', '2026-05-01', '2026-05-06', '파리', 'COMPLETED', NOW(), NOW());

-- 테스트 일정 아이템
INSERT INTO schedule_items (id, trip_id, place_id, place_name, visit_datetime, timezone, sort_order, outside_business_hours, status, created_at)
VALUES
    ('si_test001', 'trip_test001', 'place_abc123', '이치란 라멘 시부야', '2026-03-16T12:00:00', 'Asia/Tokyo', 1, FALSE, 'ACTIVE', NOW()),
    ('si_test002', 'trip_test001', 'place_def456', '시부야 스크램블 교차로', '2026-03-16T15:00:00', 'Asia/Tokyo', 2, FALSE, 'ACTIVE', NOW()),
    ('si_test003', 'trip_test002', 'place_ghi012', '도톤보리', '2026-04-02T14:00:00', 'Asia/Tokyo', 1, FALSE, 'ACTIVE', NOW());
