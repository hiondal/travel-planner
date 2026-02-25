-- PLACE 서비스 테스트 초기 데이터
-- 멱등성 보장: TRUNCATE → INSERT 순서
-- place_business_hours → places 순서 (FK 제약 때문에 역순 삭제)

TRUNCATE TABLE place_business_hours CASCADE;
TRUNCATE TABLE places CASCADE;

-- 테스트 장소
INSERT INTO places (id, name, address, category, rating, lat, lng, timezone, photo_url, city, updated_at)
VALUES
    ('place_abc123', '이치란 라멘 시부야', '도쿄 시부야구 도겐자카 1-22-7', '라멘', 4.2, 35.6595000, 139.7004000, 'Asia/Tokyo', 'https://maps.test/photo1.jpg', '도쿄', NOW()),
    ('place_def456', '시부야 스크램블 교차로', '도쿄 시부야구 도겐자카 2', '관광지', 4.5, 35.6594000, 139.7005000, 'Asia/Tokyo', 'https://maps.test/photo2.jpg', '도쿄', NOW()),
    ('place_xyz789', '후쿠로쿠 라멘', '도쿄 시부야구 우다가와초 13-11', '라멘', 4.0, 35.6621000, 139.6982000, 'Asia/Tokyo', 'https://maps.test/photo3.jpg', '도쿄', NOW()),
    ('place_ghi012', '멘야 무사시', '도쿄 시부야구 사쿠라가오카초 1-9', '라멘', 4.3, 35.6571000, 139.7031000, 'Asia/Tokyo', 'https://maps.test/photo4.jpg', '도쿄', NOW());

-- 테스트 영업시간 (id는 auto-generated bigint, 컬럼명: day_of_week)
INSERT INTO place_business_hours (place_id, day_of_week, open_time, close_time)
VALUES
    ('place_abc123', 'MONDAY', '11:00', '22:00'),
    ('place_abc123', 'TUESDAY', '11:00', '22:00'),
    ('place_abc123', 'WEDNESDAY', '11:00', '22:00'),
    ('place_abc123', 'THURSDAY', '11:00', '22:00'),
    ('place_abc123', 'FRIDAY', '11:00', '22:00'),
    ('place_abc123', 'SATURDAY', '11:00', '23:00'),
    ('place_abc123', 'SUNDAY', '11:00', '23:00'),
    ('place_xyz789', 'MONDAY', '11:30', '21:00'),
    ('place_xyz789', 'TUESDAY', '11:30', '21:00'),
    ('place_xyz789', 'WEDNESDAY', '11:30', '21:00'),
    ('place_xyz789', 'THURSDAY', '11:30', '21:00'),
    ('place_xyz789', 'FRIDAY', '11:30', '21:00'),
    ('place_ghi012', 'MONDAY', '10:00', '21:00'),
    ('place_ghi012', 'TUESDAY', '10:00', '21:00'),
    ('place_ghi012', 'WEDNESDAY', '10:00', '21:00'),
    ('place_ghi012', 'THURSDAY', '10:00', '21:00'),
    ('place_ghi012', 'FRIDAY', '10:00', '21:00');
