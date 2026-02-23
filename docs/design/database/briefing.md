# BRIEFING 서비스 - 데이터 설계서

## 데이터설계 요약

| 항목 | 내용 |
|------|------|
| 서비스 | BRIEFING (출발 전 브리핑) |
| DBMS | PostgreSQL |
| 테이블 수 | 2개 (briefings, briefing_logs) |
| Redis DB | DB 5 (BRIF 전용: 브리핑 캐시) |
| 서비스 간 FK | 없음 (데이터 독립성 원칙) |
| 특이사항 | idempotency_key 포함 (멱등성 보장) |

---

## 1. 개요

출발 전 AI 브리핑(안전/경고) 생성 및 조회를 담당한다.
동일 장소/출발시간에 대한 중복 브리핑 생성을 방지하기 위해 `idempotency_key`를 사용한다.
생성 이력은 `briefing_logs`로 별도 관리한다.

---

## 2. 테이블 정의

### 2.1 briefings

브리핑 본문 및 메타데이터. 멱등성 키로 중복 생성 방지.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| user_id | VARCHAR(36) | NOT NULL | 사용자 ID (auth 서비스 참조, FK 없음) |
| schedule_item_id | VARCHAR(36) | NOT NULL | 일정 아이템 ID (schedule 서비스 참조, FK 없음) |
| place_id | VARCHAR(200) | NOT NULL | 장소 ID |
| place_name | VARCHAR(200) | NOT NULL | 장소명 스냅샷 |
| type | VARCHAR(10) | NOT NULL | 브리핑 타입 (SAFE, WARNING) |
| departure_time | TIMESTAMPTZ | NOT NULL | 출발 예정 시간 |
| idempotency_key | VARCHAR(200) | NOT NULL, UNIQUE | 멱등성 키 ({placeId}:{departureTime 정규화}) |
| summary_text | TEXT | NOT NULL | 브리핑 요약 텍스트 |
| status_level | VARCHAR(10) | NOT NULL | 상태 수준 (SAFE, CAUTION, DANGER) |
| business_status | VARCHAR(50) | | 영업 상태 내용 |
| congestion | VARCHAR(50) | | 혼잡도 내용 |
| weather | VARCHAR(100) | | 날씨 내용 |
| walking_minutes | INTEGER | | 도보 이동 시간 |
| transit_minutes | INTEGER | | 대중교통 이동 시간 |
| distance_m | INTEGER | | 거리 (미터) |
| risk_items | JSONB | | 위험 항목 목록 [{label, severity}] |
| created_at | TIMESTAMPTZ | NOT NULL | 생성 일시 |

인덱스:
- `idx_briefings_user_id_created_at` (user_id, created_at DESC)
- `idx_briefings_user_id_departure_date` (user_id, DATE(departure_time))
- `idx_briefings_idempotency_key` (idempotency_key) UNIQUE
- `idx_briefings_schedule_item_id` (schedule_item_id)

체크 제약:
- `chk_briefings_type`: type IN ('SAFE', 'WARNING')
- `chk_briefings_status_level`: status_level IN ('SAFE', 'CAUTION', 'DANGER')

### 2.2 briefing_logs

브리핑 생성/조회 이력 로그. 분석 및 무료 티어 제한 카운팅 지원.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| briefing_id | VARCHAR(36) | NOT NULL | 연관 브리핑 ID |
| user_id | VARCHAR(36) | NOT NULL | 사용자 ID |
| status | VARCHAR(20) | NOT NULL | 처리 상태 (CREATED, SKIPPED, FAILED) |
| reason | VARCHAR(200) | | 처리 사유 (예: 멱등성 중복, 무료 제한 초과) |
| read_at | TIMESTAMPTZ | | 조회 일시 |
| read_latency_seconds | INTEGER | | 생성 후 조회까지 지연 시간 (초) |
| created_at | TIMESTAMPTZ | NOT NULL | 로그 생성 일시 |

인덱스:
- `idx_briefing_logs_user_id_created_at` (user_id, created_at DESC)
- `idx_briefing_logs_briefing_id` (briefing_id)
- `idx_briefing_logs_user_id_date` (user_id, DATE(created_at)) — 일별 카운트 조회용

---

## 3. Redis 캐시 설계

### DB 5 — BRIF 전용

| 키 패턴 | 설명 | TTL | 데이터 타입 |
|---------|------|-----|-------------|
| `brif:briefing:{briefingId}` | 브리핑 상세 정보 캐시 | 30분 | String (JSON) |
| `brif:list:{userId}:{date}` | 날짜별 브리핑 목록 캐시 | 10분 | String (JSON) |
| `brif:count:{userId}:{date}` | 일별 브리핑 생성 카운트 (무료 제한) | 자정까지 | String |
| `brif:idem:{idempotencyKey}` | 멱등성 체크 캐시 | 2시간 | String (briefingId) |

캐시 무효화:
- 브리핑 생성 완료 시 `brif:list:{userId}:{date}` 삭제
- `brif:count:{userId}:{date}` TTL을 해당일 자정으로 설정 (일별 자동 리셋)

---

## 4. 데이터 흐름

```
브리핑 생성 (스케줄러 트리거):
  GenerateBriefingRequest (scheduleItemId, placeId, userId, departureTime)
  → idempotency_key 생성 → Redis brif:idem 캐시 확인
  → 중복 시 기존 briefingId 반환 (SKIPPED)
  → 무료 티어 일일 제한 확인 (brif:count:{userId}:{date})
  → monitor 서비스에서 최신 상태 조회
  → BriefingTextGenerator.generate() → 브리핑 텍스트 생성
  → briefings insert
  → briefing_logs insert (status=CREATED)
  → FCM 푸시 발송
  → BriefingCreatedEvent 발행

브리핑 조회:
  GET /briefings/{briefingId}
  → Redis DB5 Cache-Aside (brif:briefing:{briefingId})
  → miss 시 briefings 테이블 조회
  → 만료 여부 판정 (departure_time 기준)
  → briefing_logs.read_at 업데이트
```

---

## 5. 설계 결정 사항

- `idempotency_key`는 `{placeId}:{departureTime을 시간 단위로 반올림}` 형태로 생성하여 출발 시간이 같으면 동일 브리핑 반환
- `risk_items`는 JSONB 타입으로 저장 (구조 변경 유연성)
- 브리핑 본문 필드들(business_status, congestion, weather 등)은 비정규화로 직접 컬럼에 저장하여 조회 성능 최적화
- `briefing_logs`의 일별 카운트(`countByUserIdAndCreatedAtDate`)는 Redis 카운터로 1차 확인 후 DB는 정합성 보장용
