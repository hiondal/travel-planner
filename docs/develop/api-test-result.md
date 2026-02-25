# API 테스트 결과

- 테스트 일시: 2026-02-26 (Step 4-3 재수행)
- 환경: 로컬 (localhost), Spring Boot 멀티모듈 직접 실행
- Seed 데이터: 투입 완료 (users 3명, places 4개, trips 2개, schedule_items 3개, monitoring_targets 2개, briefings 4개, subscriptions 2개)
- 인증 방식: TestAuthController `/api/v1/test/login` 으로 토큰 발급
- 이전 대비 변경: MntrController.java 수정 (rate limit 전 대상 존재 여부 확인), seed.sql 전면 수정 (실제 JPA 스키마 반영)

## 공통 이슈 및 해결

| 이슈 | 원인 | 해결 |
|------|------|------|
| Windows MINGW64 환경에서 한글 포함 curl `-d` 파싱 실패 | UTF-8 BOM 포함 (`Invalid UTF-8 start byte 0xbf`) | `--data-binary @/tmp/file.json` 파일 방식으로 우회 |
| schedule 서비스 `LocalDate` 파싱 실패 | `spring.jackson` 설정 누락 | `application.yml`에 `write-dates-as-timestamps: false` 추가 후 재빌드 |
| monitor 서비스 API-020 `UNAUTHORIZED` | `INTERNAL_SERVICE_KEY` 환경변수 미설정 (빈 값) | `INTERNAL_SERVICE_KEY=test-internal-key` 설정 후 재시작 |
| seed.sql 스키마 불일치 (전 서비스) | seed.sql이 실제 JPA 생성 테이블과 다름 | 실제 DB 스키마 조회 후 전면 수정 (PostgreSQL 문법, 컬럼명 일치) |
| PLACE API-014/016 한글 파라미터 400 | MINGW64 환경에서 URL 쿼리 한글 파싱 오류 | URL 인코딩 필요 (`%EB%9D%BC%EB%A9%98` 등) — 서비스 자체는 정상 |

## AUTH 서비스 (8081)

| ID | 엔드포인트 | 메서드 | 설명 | HTTP 상태 | 결과 | 비고 |
|----|-----------|--------|------|-----------|------|------|
| API-001 | /api/v1/auth/social-login | POST | Google OAuth 소셜 로그인 | - | SKIP | 외부 Google OAuth 의존 |
| API-002 | /api/v1/auth/token/refresh | POST | Refresh Token으로 갱신 | 200 | PASS | seed DB에 refresh_token 저장됨 → 정상 갱신. 이전과 달리 200 성공 |
| API-003 | /api/v1/auth/logout | POST | 로그아웃 | 204 | PASS | `{"refresh_token": "..."}` body 필요 |
| API-004 | /api/v1/auth/token/invalidate | POST | 토큰 무효화 및 재발급 | 200 | PASS | `{"user_id":"usr_test002","new_tier":"PRO"}` → 새 access_token 반환 |
| API-005 | /api/v1/users/consent | POST | 사용자 동의 저장 | 201 | PASS | `{"push":true,"location":true,"timestamp":"2026-02-26T09:00:00"}` |

> **TestAuthController 응답 구조 확인**: `data` 래핑 없이 최상위에 `accessToken` 필드로 반환됨. jq 파싱 시 `.accessToken` 사용 필요.

## PLACE 서비스 (8083)

| ID | 엔드포인트 | 메서드 | 설명 | HTTP 상태 | 결과 | 비고 |
|----|-----------|--------|------|-----------|------|------|
| API-014 | /api/v1/places/search | GET | 장소 검색 | 200 | PASS | 파라미터: `keyword` (query 아님), `city`. URL 인코딩 필요. DB 데이터 반환 |
| API-015 | /api/v1/places/{place_id} | GET | 장소 상세 조회 | 200 | PASS | DB 데이터 정상 반환. 영업시간 포함 |
| API-016 | /api/v1/places/nearby | GET | 주변 장소 검색 | 200 | PASS | 파라미터: `lat`, `lng`, `category`, `radius` (1000/2000/3000만 허용). URL 인코딩 필요 |

## SCHEDULE 서비스 (8082)

| ID | 엔드포인트 | 메서드 | 설명 | HTTP 상태 | 결과 | 비고 |
|----|-----------|--------|------|-----------|------|------|
| API-006 | /api/v1/trips | GET | 여행 목록 조회 | 200 | PASS | trips 2건 반환 (usr_test001 소유). place_name 정상 반환 |
| API-007 | /api/v1/trips | POST | 여행 생성 | 201 | PASS | 파일 방식으로 전송. trip_id 자동 생성 (UUID) |
| API-008 | /api/v1/trips/{tripId} | GET | 여행 조회 | 200 | PASS | trip_test001 조회 성공 (seed ID 사용) |
| API-009 | /api/v1/trips/{tripId}/schedule | GET | 일정표 조회 | 200 | PASS | schedule_items 목록 반환 |
| API-010 | /api/v1/trips/{tripId}/schedule-items | POST | 장소 추가 | 201 | PASS | place_name 정상 반환 ("이치란 라멘 시부야") |
| API-011 | /api/v1/trips/{tripId}/schedule-items/{itemId} | DELETE | 장소 삭제 | 204 | PASS | si_test002 삭제 성공 |
| API-012 | /api/v1/trips/{tripId}/schedule-items/{itemId}/replace | PUT | 장소 교체 | 200 | PASS | `{"new_place_id":"place_ghi012"}` → 교체 성공 |
| API-013 | /api/v1/trips/{tripId} | DELETE | 여행 삭제 | 204 | PASS | 신규 생성 여행 삭제 성공 |

## MONITOR 서비스 (8084)

| ID | 엔드포인트 | 메서드 | 설명 | HTTP 상태 | 결과 | 비고 |
|----|-----------|--------|------|-----------|------|------|
| API-017 | /api/v1/badges | GET | 상태 배지 목록 조회 | 200 | PASS | `place_ids` 쿼리 파라미터 필수 (콤마 구분). `?place_ids=place_abc123,place_def456` |
| API-018 | /api/v1/badges/{placeId}/detail | GET | 배지 상세 조회 | 200 | PASS | 영업상태/혼잡도/날씨/이동시간 상세 반환 |
| API-019 | /api/v1/badges/{placeId}/refresh | POST | 상태 수동 새로고침 | 200 | PASS | 외부 API 호출 후 상태 갱신. rate limit 전 대상 존재 여부 확인 로직 추가됨 |
| API-020 | /api/v1/monitor/collect | POST | 내부 데이터 수집 트리거 | 202 | PASS | `X-Internal-Service-Key: test-internal-key` 헤더 필요 |

## BRIEFING 서비스 (8085)

| ID | 엔드포인트 | 메서드 | 설명 | HTTP 상태 | 결과 | 비고 |
|----|-----------|--------|------|-----------|------|------|
| API-021 | /api/v1/briefings | GET | 브리핑 목록 조회 | 200 | PASS | 당일 브리핑만 반환. seed 데이터는 과거 날짜라 빈 결과 (정상) |
| API-022 | /api/v1/briefings/{briefingId} | GET | 브리핑 상세 조회 | 200 | PASS | expired:true (departure_time 과거). 내용 정상 반환 |
| API-023 | /api/v1/briefings/generate | POST | 브리핑 생성 | 201 | PASS | 필수: `schedule_item_id`, `place_id`, `user_id`, `departure_time`, `triggered_at` |

## ALTERNATIVE 서비스 (8086)

| ID | 엔드포인트 | 메서드 | 설명 | HTTP 상태 | 결과 | 비고 |
|----|-----------|--------|------|-----------|------|------|
| API-024 | /api/v1/alternatives/search | POST | 대안 검색 | 200 | PASS | TRIP_PASS/PRO 토큰 필요. FREE 티어는 402 반환. 필드: `place_id`, `category`, `location{lat,lng}` |
| API-025 | /api/v1/alternatives/{altId}/select | POST | 대안 선택 | 200 | PASS | 검색 응답의 `alt_id` 사용. 필드: `original_place_id`, `schedule_item_id`, `trip_id`, `selected_rank`, `elapsed_seconds` |

## PAYMENT 서비스 (8087)

| ID | 엔드포인트 | 메서드 | 설명 | HTTP 상태 | 결과 | 비고 |
|----|-----------|--------|------|-----------|------|------|
| API-026 | /api/v1/subscriptions/plans | GET | 구독 플랜 조회 | 200 | PASS | TRIP_PASS(4900원), PRO(9900원) 플랜 반환 |
| API-027 | /api/v1/subscriptions/purchase | POST | 구독 구매 | 201 | PASS | `provider`는 `apple` 또는 `google` (소문자). `receipt` 필드 필수 |
| API-028 | /api/v1/subscriptions/status | GET | 구독 상태 조회 | 200 | PASS | TRIP_PASS ACTIVE 상태 정상 반환 |

## 테스트 요약

| 서비스 | 전체 | PASS | SKIP | FAIL | 비고 |
|--------|------|------|------|------|------|
| AUTH | 5 | 4 | 1 | 0 | API-001 Google OAuth SKIP |
| PLACE | 3 | 3 | 0 | 0 | DB 데이터 실제 반환. 한글 파라미터 URL 인코딩 필요 |
| SCHEDULE | 8 | 8 | 0 | 0 | place_name 정상 반환 확인 |
| MONITOR | 4 | 4 | 0 | 0 | place_ids 파라미터 필수 확인 |
| BRIEFING | 3 | 3 | 0 | 0 | generate 필수 필드 5개 확인 |
| ALTERNATIVE | 2 | 2 | 0 | 0 | FREE 티어 402 처리 정상 |
| PAYMENT | 3 | 3 | 0 | 0 | |
| **합계** | **28** | **27** | **1** | **0** | |

## 이전 테스트 대비 변경사항

### 이전 이슈 해결 여부

| 이전 이슈 | 해결 여부 | 결과 |
|----------|----------|------|
| jackson 설정 누락 (schedule) | 해결됨 | schedule 서비스 정상 동작 |
| INTERNAL_SERVICE_KEY 미정의 | 해결됨 | API-020 202 PASS |
| place_name "알 수 없는 장소" 문제 | **해결됨** | PLACE 서비스 DB 데이터 seed 투입으로 "이치란 라멘 시부야" 정상 반환 |

### 신규 발견 사항

1. **TestAuthController 응답 구조**: `data` 래핑 없이 최상위에 `accessToken` 필드. 이전 테스트에서 `.data.access_token`으로 파싱했으나 실제로는 `.accessToken`
2. **API-002 Refresh Token**: seed DB에 저장된 refresh_token으로 갱신 성공 (이전 테스트에서는 401이었으나 이번에는 200 성공 — seed 데이터 정상 투입 덕분)
3. **API-017 place_ids 파라미터 필수**: 이전 테스트에서는 파라미터 없이 호출했으나 실제로 필수 쿼리 파라미터 존재
4. **API-024 대안 검색 요청 필드**: 이전 `original_place_id`, `location`, `schedule_item_id`, `trip_id` → 실제는 `place_id`, `category`, `location{lat,lng}`
5. **API-023 브리핑 생성 필드**: `schedule_item_id`, `place_id`, `user_id`, `departure_time`, `triggered_at` 5개 필드 모두 필수
6. **ALTERNATIVE 서비스 FREE 티어 접근 차단**: FREE 사용자 402 Payment Required 응답 정상 동작 확인
7. **MntrController rate limit 개선**: `0188767` 커밋 — rate limit 전에 대상 존재 여부 확인하여 불필요한 Redis 연산 방지

### seed.sql 수정 내역 (스키마 불일치 전면 수정)

| 서비스 | 수정 내용 |
|--------|----------|
| auth | `refresh_tokens.token` → `refresh_token`, `consents.updated_at` 제거, `DATEADD` → PostgreSQL `INTERVAL` 문법 |
| place | `business_hours` → `place_business_hours`, `id` 제거(auto-generated), `day` → `day_of_week`, TRUNCATE 순서 수정 |
| monitor | `place_statuses` (없는 테이블) → `monitoring_targets`, `collected_data`, `status_history` 실제 테이블로 전면 재작성 |
| briefing | `summary` → `summary_text`, `alternative_link` 제거, `idempotency_key`/`status_level` 추가, `DATEADD` → `INTERVAL` |
| alternative | `alternative_selections` → `selection_logs`, `alternative_card_snapshots` 실제 스키마로 전면 재작성, `alternatives` 테이블 추가 |
| payment | `DATEADD` → `INTERVAL`, `plan_id`/`receipt_hash`/`status` 제거, `transaction_id` 추가 |
