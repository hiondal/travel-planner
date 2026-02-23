# MONITOR 서비스 - 데이터 설계서

## 데이터설계 요약

| 항목 | 내용 |
|------|------|
| 서비스 | MONITOR (실시간 상태 모니터링) |
| DBMS | PostgreSQL |
| 테이블 수 | 3개 (monitoring_targets, status_history, collected_data) |
| Redis DB | DB 4 (MNTR 전용: 배지 캐시, 수집 데이터 캐시) |
| 서비스 간 FK | 없음 (데이터 독립성 원칙) |
| 특이사항 | status_history: append-only, 6개월+ 보존 [T4] |

---

## 1. 개요

일정 항목의 장소 상태(영업, 날씨, 혼잡도, 이동시간)를 주기적으로 수집·판정하고,
상태 뱃지를 Redis에 캐싱하여 빠른 조회를 제공한다.
`status_history`는 append-only 이력 테이블로 6개월 이상 보존한다.

---

## 2. 테이블 정의

### 2.1 monitoring_targets

모니터링 대상 장소. schedule 서비스에서 ScheduleItemAddedEvent 수신 시 등록.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| place_id | VARCHAR(200) | NOT NULL | 장소 ID (place 서비스 참조, FK 없음) |
| trip_id | VARCHAR(36) | NOT NULL | 여행 ID (schedule 서비스 참조, FK 없음) |
| schedule_item_id | VARCHAR(36) | NOT NULL, UNIQUE | 일정 아이템 ID |
| user_id | VARCHAR(36) | NOT NULL | 사용자 ID (auth 서비스 참조, FK 없음) |
| visit_datetime | TIMESTAMPTZ | NOT NULL | 방문 예정 일시 |
| lat | NUMERIC(10,7) | NOT NULL | 장소 위도 |
| lng | NUMERIC(10,7) | NOT NULL | 장소 경도 |
| category | VARCHAR(50) | | 장소 카테고리 |
| current_status | VARCHAR(10) | NOT NULL, DEFAULT 'GREY' | 현재 상태 (GREEN/YELLOW/RED/GREY) |
| current_status_updated_at | TIMESTAMPTZ | | 현재 상태 마지막 업데이트 |
| consecutive_failure_count | INTEGER | NOT NULL, DEFAULT 0 | 연속 수집 실패 횟수 |
| created_at | TIMESTAMPTZ | NOT NULL | 등록 일시 |

인덱스:
- `idx_monitoring_targets_place_id` (place_id)
- `idx_monitoring_targets_visit_datetime` (visit_datetime) — 수집 윈도우 필터링
- `idx_monitoring_targets_user_id` (user_id)
- `idx_monitoring_targets_schedule_item_id` (schedule_item_id) UNIQUE

체크 제약:
- `chk_monitoring_targets_status`: current_status IN ('GREEN','YELLOW','RED','GREY')

### 2.2 status_history

상태 판정 이력. append-only, 6개월+ 보존. 분석 및 ML 학습 데이터 소스.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| place_id | VARCHAR(200) | NOT NULL | 장소 ID |
| schedule_item_id | VARCHAR(36) | NOT NULL | 일정 아이템 ID |
| status | VARCHAR(10) | NOT NULL | 판정 상태 (GREEN/YELLOW/RED/GREY) |
| reason | TEXT | | 판정 사유 |
| judgment_at | TIMESTAMPTZ | NOT NULL | 판정 일시 |
| confidence_score | NUMERIC(4,3) | | 신뢰도 점수 (0.000 ~ 1.000) |

인덱스:
- `idx_status_history_place_id_judgment_at` (place_id, judgment_at DESC)
- `idx_status_history_schedule_item_id` (schedule_item_id)
- `idx_status_history_judgment_at` (judgment_at) — 파티셔닝/보존 정책용

체크 제약:
- `chk_status_history_status`: status IN ('GREEN','YELLOW','RED','GREY')
- `chk_status_history_confidence`: confidence_score BETWEEN 0.000 AND 1.000

보존 정책:
- 파티셔닝: RANGE 파티션 (월별) 적용 권고
- 6개월 경과 파티션은 콜드 스토리지(S3) 아카이빙 후 삭제

### 2.3 collected_data

외부 API 수집 원본 데이터. 판정 근거 보존 및 폴백 캐시 역할.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| place_id | VARCHAR(200) | NOT NULL | 장소 ID |
| business_status | VARCHAR(20) | | 영업 상태 (OPEN/CLOSED/UNKNOWN) |
| precipitation_prob | INTEGER | | 강수 확률 (0~100) |
| weather_condition | VARCHAR(50) | | 날씨 상태 설명 |
| walking_minutes | INTEGER | | 도보 이동 시간 (분) |
| transit_minutes | INTEGER | | 대중교통 이동 시간 (분) |
| distance_m | INTEGER | | 거리 (미터) |
| congestion_level | VARCHAR(20) | | 혼잡도 수준 |
| has_fallback | BOOLEAN | NOT NULL, DEFAULT FALSE | 폴백 데이터 포함 여부 |
| collected_at | TIMESTAMPTZ | NOT NULL | 수집 일시 |

인덱스:
- `idx_collected_data_place_id_collected_at` (place_id, collected_at DESC)

---

## 3. Redis 캐시 설계

### DB 4 — MNTR 전용

| 키 패턴 | 설명 | TTL | 데이터 타입 |
|---------|------|-----|-------------|
| `mntr:badge:{placeId}` | 상태 뱃지 (상태, 아이콘, 색상) | 5분 | Hash |
| `mntr:collected:{placeId}` | 최신 수집 데이터 (폴백용) | 30분 | String (JSON) |
| `mntr:status:{placeId}` | 현재 상태 요약 | 5분 | Hash |

Hash 필드 (`mntr:badge:{placeId}`):
- `status`: GREEN/YELLOW/RED/GREY
- `icon`: CHECK/EXCLAMATION/X/QUESTION
- `label`: 표시 텍스트
- `colorHex`: HEX 색상 코드
- `updatedAt`: ISO 8601

캐시 무효화:
- 상태 판정 완료 시 `mntr:badge:{placeId}`, `mntr:status:{placeId}` 갱신
- `PlaceStatusChangedEvent` 발행 시 briefing/alternative 서비스에 알림

---

## 4. 데이터 흐름

```
모니터링 대상 등록:
  ScheduleItemAddedEvent 수신
  → monitoring_targets insert

상태 수집 (스케줄러 주기 실행):
  monitoring_targets에서 수집 윈도우 내 대상 조회
  → 외부 API 병렬 호출 (Google Places, OpenWeatherMap, GoogleDirections)
  → collected_data insert
  → StatusJudgmentService.judge() → 상태 판정
  → status_history append
  → monitoring_targets.current_status 업데이트
  → mntr:badge:{placeId} 캐시 갱신
  → 상태 변경 시 PlaceStatusChangedEvent 발행

배지 조회 (API):
  GET /badges?placeIds=...
  → Redis DB4 Cache-Aside (mntr:badge:{placeId})
  → miss 시 monitoring_targets 조회 → 배지 생성 → 캐시 등록
```

---

## 5. 설계 결정 사항

- `current_status`를 monitoring_targets 컬럼으로 비정규화하여 배지 조회 성능 최적화
- `status_history`는 insert-only. UPDATE/DELETE 금지. 월별 파티셔닝 필수
- `collected_data`는 폴백 캐시 겸 판정 근거 보존용 (연속 실패 시 마지막 성공 데이터 재사용)
- `confidence_score`는 Phase 2 ML 모델 도입 시 학습 레이블로 활용
