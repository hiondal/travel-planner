-- BRIEFING 서비스 테스트 초기 데이터
-- 멱등성 보장: TRUNCATE → INSERT 순서

TRUNCATE TABLE briefings;

-- 테스트 브리핑 (SAFE - 미만료)
INSERT INTO briefings (id, user_id, schedule_item_id, place_id, place_name, type, departure_time, business_status, congestion, weather, walking_minutes, transit_minutes, distance_m, summary, alternative_link, created_at)
VALUES
    ('brif_test001', 'usr_test001', 'si_test001', 'place_abc123', '이치란 라멘 시부야', 'SAFE',
     DATEADD('HOUR', 2, NOW()),
     '영업 중', '보통', '맑음', 15, NULL, 420,
     '현재까지 모든 항목 정상입니다. 예정대로 출발하세요.',
     NULL, NOW()),
    -- WARNING - 미만료
    ('brif_test002', 'usr_test001', 'si_test002', 'place_def456', '시부야 스크램블 교차로', 'WARNING',
     DATEADD('HOUR', 2, NOW()),
     '영업 중', '혼잡', '맑음', 20, 8, 620,
     '혼잡도이(가) 감지되었습니다. 대안을 확인해보세요.',
     '/alternatives?place_id=place_def456', NOW()),
    -- SAFE - 만료됨 (departure_time < NOW)
    ('brif_test003', 'usr_test001', 'si_test001', 'place_abc123', '이치란 라멘 시부야', 'SAFE',
     DATEADD('HOUR', -1, NOW()),
     '영업 중', '보통', '맑음', 15, NULL, 420,
     '현재까지 모든 항목 정상입니다. 예정대로 출발하세요.',
     NULL, DATEADD('HOUR', -2, NOW())),
    -- 다른 사용자 소유 브리핑
    ('brif_test004', 'usr_test002', 'si_test_other', 'place_xyz789', '후쿠로쿠 라멘', 'SAFE',
     DATEADD('HOUR', 2, NOW()),
     '영업 중', '낮음', '맑음', 5, NULL, 320,
     '현재까지 모든 항목 정상입니다. 예정대로 출발하세요.',
     NULL, NOW());
