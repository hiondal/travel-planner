# ALTERNATIVE 서비스 - 데이터 설계서

## 데이터설계 요약

| 항목 | 내용 |
|------|------|
| 서비스 | ALTERNATIVE (대안 장소 추천) |
| DBMS | PostgreSQL |
| 테이블 수 | 3개 (alternatives, alternative_card_snapshots, selection_logs) |
| Redis DB | DB 6 (ALTN 전용: 대안 카드 캐시) |
| 서비스 간 FK | 없음 (데이터 독립성 원칙) |
| 특이사항 | alternative_card_snapshots, selection_logs: ML 학습 데이터 [T3] |

---

## 1. 개요

장소 이상 감지 시 대안 장소를 추천하고 선택 이력을 관리한다.
`AlternativeCardSnapshot`과 `SelectionLog`는 ML 학습용 데이터 소스로 장기 보존한다.
대안 카드 검색 결과는 Redis에 캐시하여 반복 조회를 최소화한다.

---

## 2. 테이블 정의

### 2.1 alternatives

추천된 대안 장소 정보 스냅샷. 검색 시점의 장소 상태를 기록.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| user_id | VARCHAR(36) | NOT NULL | 사용자 ID (auth 서비스 참조, FK 없음) |
| original_place_id | VARCHAR(200) | NOT NULL | 원래 장소 ID |
| place_id | VARCHAR(200) | NOT NULL | 대안 장소 ID (place 서비스 참조, FK 없음) |
| name | VARCHAR(200) | NOT NULL | 대안 장소명 스냅샷 |
| distance_m | INTEGER | NOT NULL | 원래 장소로부터 거리 (미터) |
| rating | NUMERIC(3,1) | | 장소 평점 |
| congestion | VARCHAR(20) | | 혼잡도 수준 |
| reason | VARCHAR(200) | | 추천 이유 |
| status_label | VARCHAR(50) | | 상태 레이블 (monitor 서비스 뱃지 스냅샷) |
| lat | NUMERIC(10,7) | NOT NULL | 위도 |
| lng | NUMERIC(10,7) | NOT NULL | 경도 |
| walking_minutes | INTEGER | | 도보 이동 시간 (분) |
| transit_minutes | INTEGER | | 대중교통 이동 시간 (분) |
| score | NUMERIC(5,4) | NOT NULL | 종합 추천 점수 |
| created_at | TIMESTAMPTZ | NOT NULL | 생성 일시 |

인덱스:
- `idx_alternatives_user_id` (user_id)
- `idx_alternatives_original_place_id` (original_place_id)
- `idx_alternatives_place_id` (place_id)
- `idx_alternatives_created_at` (created_at)

### 2.2 alternative_card_snapshots

대안 카드 노출 스냅샷. 어떤 가중치/점수로 노출됐는지 기록 (ML 학습 데이터 [T3]).

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| user_id | VARCHAR(36) | NOT NULL | 사용자 ID |
| place_id | VARCHAR(200) | NOT NULL | 원래 장소 ID |
| candidate_place_id | VARCHAR(200) | NOT NULL | 후보 대안 장소 ID |
| score_weights | JSONB | NOT NULL | 적용된 가중치 {distanceWeight, ratingWeight, congestionWeight} |
| scores | JSONB | NOT NULL | 개별 점수 {distanceScore, ratingScore, congestionScore, totalScore} |
| exposed_at | TIMESTAMPTZ | NOT NULL | 노출 일시 |

인덱스:
- `idx_alt_card_snapshots_user_id` (user_id)
- `idx_alt_card_snapshots_place_id` (place_id)
- `idx_alt_card_snapshots_exposed_at` (exposed_at)

### 2.3 selection_logs

사용자 대안 선택 로그. 어떤 순위의 카드를 선택했는지, 선택 소요 시간 기록 (ML 학습 데이터 [T3]).

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| alt_card_id | VARCHAR(36) | NOT NULL | alternative_card_snapshots.id 참조 (서비스 내부 FK) |
| user_id | VARCHAR(36) | NOT NULL | 사용자 ID |
| selected_rank | INTEGER | NOT NULL | 선택한 카드 순위 (1-based) |
| elapsed_seconds | INTEGER | NOT NULL | 화면 표시 후 선택까지 소요 시간 (초) |
| adopted | BOOLEAN | NOT NULL, DEFAULT FALSE | 일정에 실제 반영 여부 |
| created_at | TIMESTAMPTZ | NOT NULL | 선택 일시 |

인덱스:
- `idx_selection_logs_user_id` (user_id)
- `idx_selection_logs_alt_card_id` (alt_card_id)
- `idx_selection_logs_created_at` (created_at)

외래키:
- `fk_selection_logs_alt_card_id` → alternative_card_snapshots(id)

---

## 3. Redis 캐시 설계

### DB 6 — ALTN 전용

| 키 패턴 | 설명 | TTL | 데이터 타입 |
|---------|------|-----|-------------|
| `altn:cards:{placeId}:{category}:{radius}` | 대안 카드 목록 캐시 | 10분 | String (JSON) |
| `altn:alt:{altId}` | 개별 대안 상세 캐시 | 30분 | String (JSON) |

캐시 무효화:
- 원래 장소의 상태 변경(`PlaceStatusChangedEvent`) 수신 시 해당 placeId 관련 카드 캐시 삭제
- 대안 선택 완료 시 `altn:cards:{placeId}:*` 패턴 삭제

---

## 4. 데이터 흐름

```
대안 검색:
  AlternativeSearchRequest (placeId, category, lat, lng)
  → Redis DB6 Cache-Aside 조회
  → miss 시:
    place 서비스에서 근처 후보 조회 (searchNearby)
    monitor 서비스에서 후보 뱃지 조회 (getBadges)
    → 상태 필터링 (RED 제외)
    → ScoreCalculator.calculateScores() 점수 계산
    → alternatives 목록 생성
    → alternative_card_snapshots insert (ML 데이터 수집)
    → Redis 캐시 등록 (10분 TTL)

대안 선택:
  SelectAlternativeRequest
  → alternatives 조회
  → schedule 서비스 replaceScheduleItem 호출
  → selection_logs insert (adopted=true)
  → 캐시 무효화
```

---

## 5. 설계 결정 사항

- `alternatives`는 검색 시점의 상태 스냅샷을 저장 (place_name, status_label 비정규화)
- `score_weights`, `scores`는 JSONB로 저장하여 가중치 모델 변경에 유연하게 대응
- `selection_logs.adopted`는 schedule 서비스 replaceScheduleItem 성공 여부로 업데이트
- ML 학습 데이터 보존 정책: `alternative_card_snapshots`와 `selection_logs`는 최소 1년 보존
