# SCHEDULE 서비스 - 데이터 설계서

## 데이터설계 요약

| 항목 | 내용 |
|------|------|
| 서비스 | SCHEDULE (여행 일정 관리) |
| DBMS | PostgreSQL |
| 테이블 수 | 2개 (trips, schedule_items) |
| Redis DB | DB 2 (SCHD 전용: 일정 캐시) |
| 서비스 간 FK | 없음 (데이터 독립성 원칙) |

---

## 1. 개요

여행(Trip) 생성 및 일정 아이템(ScheduleItem) 관리를 담당한다.
place 서비스의 장소 데이터는 캐시로만 참조하며, DB 조인 금지.
consent 확인은 서비스 클라이언트 호출로 처리한다.

---

## 2. 테이블 정의

### 2.1 trips

여행 계획 헤더 정보.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| user_id | VARCHAR(36) | NOT NULL | 소유 사용자 ID (auth 서비스 참조, FK 없음) |
| name | VARCHAR(200) | NOT NULL | 여행 이름 |
| start_date | DATE | NOT NULL | 여행 시작일 |
| end_date | DATE | NOT NULL | 여행 종료일 |
| city | VARCHAR(100) | NOT NULL | 여행 도시 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | 상태 (ACTIVE, COMPLETED, CANCELLED) |
| created_at | TIMESTAMPTZ | NOT NULL | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL | 수정 일시 |

인덱스:
- `idx_trips_user_id` (user_id)
- `idx_trips_user_id_status` (user_id, status)
- `idx_trips_start_date_end_date` (start_date, end_date)

체크 제약:
- `chk_trips_dates`: end_date >= start_date
- `chk_trips_status`: status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')

### 2.2 schedule_items

일정 항목. trip에 종속되는 장소 방문 계획.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| trip_id | VARCHAR(36) | NOT NULL, FK → trips(id) | 소속 여행 ID |
| place_id | VARCHAR(200) | NOT NULL | 장소 ID (place 서비스 참조, FK 없음) |
| place_name | VARCHAR(200) | NOT NULL | 장소명 스냅샷 (place 서비스 변경에 독립) |
| visit_datetime | TIMESTAMPTZ | NOT NULL | 방문 예정 일시 |
| timezone | VARCHAR(50) | NOT NULL | 방문 장소 타임존 |
| sort_order | INTEGER | NOT NULL, DEFAULT 0 | 일정 내 순서 |
| outside_business_hours | BOOLEAN | NOT NULL, DEFAULT FALSE | 영업시간 외 방문 여부 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | 상태 |
| created_at | TIMESTAMPTZ | NOT NULL | 생성 일시 |

인덱스:
- `idx_schedule_items_trip_id` (trip_id)
- `idx_schedule_items_trip_id_visit_datetime` (trip_id, visit_datetime)
- `idx_schedule_items_trip_id_sort_order` (trip_id, sort_order)
- `idx_schedule_items_place_id` (place_id) — 모니터링 대상 조회 시 사용

외래키:
- `fk_schedule_items_trip_id` → trips(id) ON DELETE CASCADE

---

## 3. Redis 캐시 설계

### DB 2 — SCHD 전용

| 키 패턴 | 설명 | TTL | 데이터 타입 |
|---------|------|-----|-------------|
| `schd:trip:{tripId}` | 여행 기본 정보 캐시 | 10분 | Hash |
| `schd:schedule:{tripId}` | 일정 아이템 목록 캐시 | 5분 | String (JSON) |

Hash 필드 (`schd:trip:{tripId}`):
- `userId`, `name`, `startDate`, `endDate`, `city`, `status`

캐시 무효화:
- 일정 아이템 추가/삭제/교체 시 `schd:schedule:{tripId}` 삭제
- 여행 상태 변경 시 `schd:trip:{tripId}` 삭제

---

## 4. 데이터 흐름

```
여행 생성:
  CreateTripRequest → consent 확인 (auth 서비스 호출)
  → trips 테이블 insert
  → schd:trip:{tripId} 캐시 등록

일정 추가:
  AddScheduleItemRequest → place 서비스에서 장소 상세 조회
  → 영업시간 체크 (place_name 스냅샷으로 저장)
  → schedule_items insert
  → schd:schedule:{tripId} 캐시 무효화
  → ScheduleItemAddedEvent 발행 (monitor 서비스 구독)

일정 교체:
  ReplaceScheduleItemRequest → place 서비스 신규 장소 조회
  → schedule_items update (place_id, place_name)
  → 이동시간 재계산
  → ScheduleItemReplacedEvent 발행
```

---

## 5. 설계 결정 사항

- `place_name`은 생성 시점 스냅샷으로 저장. place 서비스의 장소명 변경에 영향받지 않음
- `user_id`는 auth 서비스 ID 참조이지만 DB FK 없음. 서비스 간 데이터 독립성 원칙 적용
- 모니터링 대상 등록을 위해 `ScheduleItemAddedEvent`를 발행하며, monitor 서비스가 이를 소비
