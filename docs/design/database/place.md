# PLACE 서비스 - 데이터 설계서

## 데이터설계 요약

| 항목 | 내용 |
|------|------|
| 서비스 | PLACE (장소 정보) |
| DBMS | PostgreSQL |
| 테이블 수 | 2개 (places, place_business_hours) |
| Redis DB | DB 3 (PLCE 전용: 장소 데이터·검색 결과 캐시) |
| 서비스 간 FK | 없음 (데이터 독립성 원칙) |

---

## 1. 개요

Google Places API 응답을 로컬 DB에 캐싱하여 외부 API 호출을 최소화한다.
장소 검색 결과는 Redis에 캐시하고, 상세 정보는 PostgreSQL + Redis 이중 구조로 관리한다.
`BusinessHour`는 별도 테이블(place_business_hours)로 정규화한다.

---

## 2. 테이블 정의

### 2.1 places

Google Places API에서 수집한 장소 정보. upsert 방식으로 갱신.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(200) | PK | Google Place ID |
| name | VARCHAR(200) | NOT NULL | 장소명 |
| address | VARCHAR(500) | | 주소 |
| category | VARCHAR(50) | | 카테고리 |
| rating | NUMERIC(3,1) | | 평점 (0.0 ~ 5.0) |
| lat | NUMERIC(10,7) | NOT NULL | 위도 |
| lng | NUMERIC(10,7) | NOT NULL | 경도 |
| timezone | VARCHAR(50) | | 타임존 |
| photo_url | VARCHAR(500) | | 대표 사진 URL |
| city | VARCHAR(100) | | 도시명 |
| updated_at | TIMESTAMPTZ | NOT NULL | 마지막 갱신 일시 |

인덱스:
- `idx_places_city` (city)
- `idx_places_category` (category)
- `idx_places_city_category` (city, category)
- `idx_places_coordinates` (lat, lng) — 근처 장소 검색용
- `idx_places_updated_at` (updated_at) — 캐시 신선도 관리용

체크 제약:
- `chk_places_rating`: rating BETWEEN 0.0 AND 5.0

### 2.2 place_business_hours

장소별 영업시간. places 테이블과 1:N 관계.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | SERIAL | PK | 자동 증가 |
| place_id | VARCHAR(200) | NOT NULL, FK → places(id) | 장소 ID |
| day_of_week | VARCHAR(10) | NOT NULL | 요일 (MONDAY ~ SUNDAY) |
| open_time | VARCHAR(5) | | 오픈 시간 (HH:mm) |
| close_time | VARCHAR(5) | | 마감 시간 (HH:mm) |

인덱스:
- `idx_place_business_hours_place_id` (place_id)

외래키:
- `fk_place_business_hours_place_id` → places(id) ON DELETE CASCADE

체크 제약:
- `chk_business_hours_day`: day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')

---

## 3. Redis 캐시 설계

### DB 3 — PLCE 전용

| 키 패턴 | 설명 | TTL | 데이터 타입 |
|---------|------|-----|-------------|
| `plce:detail:{placeId}` | 장소 상세 정보 (영업시간 포함) | 1시간 | String (JSON) |
| `plce:search:{keyword}:{city}` | 텍스트 검색 결과 목록 | 30분 | String (JSON) |
| `plce:nearby:{latLng}:{category}:{radius}` | 근처 장소 검색 결과 | 15분 | String (JSON) |

키 생성 규칙:
- `{latLng}`: 위경도를 소수점 3자리로 반올림하여 키 충돌 방지 (예: `35.681_139.767`)
- `{keyword}:{city}`: 소문자 정규화 후 URL 인코딩

캐시 무효화:
- Google Places API로 신규 조회 시 해당 placeId 캐시 갱신 (write-through)
- 장소 updated_at이 24시간 초과 시 강제 refresh

---

## 4. 데이터 흐름

```
장소 텍스트 검색:
  keyword + city → Redis DB3 Cache-Aside 조회
  → miss 시 PostgreSQL places 조회 (city + keyword LIKE)
  → miss 시 Google Places API textSearch 호출
  → 결과를 places + place_business_hours upsert
  → Redis 캐시 등록 (30분 TTL)

장소 상세 조회:
  placeId → Redis DB3 Cache-Aside 조회
  → miss 시 PostgreSQL places + place_business_hours JOIN 조회
  → miss 시 Google Places API placeDetail 호출
  → DB upsert → Redis 캐시 등록 (1시간 TTL)

근처 장소 검색:
  lat, lng, category, radius → Redis DB3 Cache-Aside 조회
  → miss 시 PostgreSQL 좌표 반경 쿼리
  → miss/stale 시 Google Places API nearbySearch 호출
  → DB upsert → Redis 캐시 등록 (15분 TTL)
```

---

## 5. 설계 결정 사항

- `places.id`는 Google Place ID를 그대로 사용 (외부 시스템 키 일관성)
- 영업시간은 별도 테이블 정규화로 요일별 조회 효율화
- 캐시가 주 경로이며 PostgreSQL은 Google API 호출 횟수 절감을 위한 영속 캐시 역할
- 좌표 기반 근처 검색은 PostGIS 없이 NUMERIC 컬럼 범위 쿼리로 MVP 구현 (Phase 2에서 PostGIS 전환 고려)
