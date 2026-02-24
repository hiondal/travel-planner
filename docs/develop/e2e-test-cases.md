# E2E 테스트 케이스

> 작성자: 조현아/가디언 (QA 엔지니어)
> 작성일: 2026-02-24
> 기반: dev-plan.md Phase 4 테스트 시나리오 (TC-01 ~ TC-10)
> 범위: Phase 1 MVP — 일정 등록, 배지 표시, 브리핑 Push, 대안 카드 3장

---

## 1. 테스트 범위 및 전략

### 1-1. 테스트 레벨 정의

| 레벨 | 도구 | 범위 | 비고 |
|------|------|------|------|
| 단위 테스트 | JUnit 5 + Mockito | 서비스 레이어 비즈니스 로직 | 7개 모듈 16개 파일 |
| 통합 테스트 | Spring Boot Test (TestRestTemplate) | 서비스 내 HTTP API + H2 DB | 7개 모듈, 서비스별 엔드포인트 검증 |
| E2E 테스트 | curl 기반 API 체인 스크립트 | 서비스 간 API 호출 체인 | 이번 작성 |

### 1-2. E2E 테스트 전략

- **접근 방식**: Flutter 앱 UI 자동화가 아닌 API 레벨 E2E (curl HTTP 요청 체인)
- **이유**: Flutter 앱 UI 자동화는 실기기/에뮬레이터가 필요하며, Playwright는 브라우저 기반이므로 Flutter 모바일 앱에는 적용 불가. Phase 1에서는 백엔드 서비스 간 연동을 curl 스크립트로 검증
- **실행 전제**: 7개 백엔드 서비스가 docker-compose 또는 IntelliJ로 모두 기동된 상태
- **미실행 항목**: FCM Push 수신, IAP 실결제는 외부 서비스 의존으로 별도 검증 필요

### 1-3. 서비스 URL 매핑

| 서비스 ID | URL | 포트 |
|----------|-----|------|
| AUTH | http://localhost:8081/api/v1 | 8081 |
| SCHD | http://localhost:8082/api/v1 | 8082 |
| PLCE | http://localhost:8083/api/v1 | 8083 |
| MNTR | http://localhost:8084/api/v1 | 8084 |
| BRIF | http://localhost:8085/api/v1 | 8085 |
| ALTN | http://localhost:8086/api/v1 | 8086 |
| PAY  | http://localhost:8087/api/v1 | 8087 |

---

## 2. E2E 테스트 시나리오

### E2E-001: 신규 사용자 온보딩 플로우

**유형**: E2E
**관련 서비스**: AUTH
**유저스토리**: 앱을 처음 실행한 사용자가 Google 계정으로 로그인하고 JWT 토큰을 발급받은 후, 위치/푸시 동의를 저장하고, 첫 여행을 생성한다.

#### 사전 조건
- AUTH 서비스 기동 (port 8081), SCHD 서비스 기동 (port 8082)
- Google OAuth2는 테스트 환경에서 Mock 응답 (실제 Google API 키 없음 -- 401 INVALID_OAUTH_CODE 정상 처리 확인)

#### 시나리오 흐름

```
1. POST /api/v1/auth/social-login (AUTH:8081)
   입력: { "provider": "google", "oauth_code": "test_google_code" }
   기대: 401 INVALID_OAUTH_CODE (Google API 키 없는 테스트 환경)
   --> 실제 환경: 200 OK, access_token + refresh_token + user_profile 반환, is_new_user=true

2. POST /api/v1/auth/social-login (동일 계정 재로그인)
   기대: 200 OK, is_new_user=false (실제 환경)

3. POST /api/v1/users/consent (AUTH:8081)
   헤더: Authorization: Bearer <access_token>
   입력: { "location": true, "push": true, "consented_at": "2026-02-24T10:00:00" }
   기대: 201 Created, consent_id 반환

4. POST /api/v1/trips (SCHD:8082)
   헤더: Authorization: Bearer <access_token>
   입력: { "name": "도쿄 3박4일", "start_date": "2026-03-15", "end_date": "2026-03-18", "city": "도쿄" }
   기대: 201 Created, trip_id 반환
```

#### 테스트 케이스 목록

| TC-ID | 단계 | 입력 | 기대 결과 | 검증 포인트 |
|-------|------|------|----------|-----------|
| E2E-001-01 | Google 로그인 | provider=google, oauth_code=valid | 200, access_token 존재 | JWT 구조 (3-part) 확인 |
| E2E-001-02 | Apple 로그인 | provider=apple, oauth_code=valid | 200, access_token 존재 | provider 필드 apple |
| E2E-001-03 | 지원 외 provider | provider=kakao | 400, VALIDATION_ERROR | error_code 확인 |
| E2E-001-04 | provider 누락 | oauth_code만 전달 | 400, VALIDATION_ERROR | 필드 오류 메시지 |
| E2E-001-05 | 동의 저장 | Bearer 토큰 + location/push | 201, consent_id | consent_id 존재 확인 |
| E2E-001-06 | 동의 없이 여행 생성 | 토큰만 사용 | 201, trip_id | 동의 무관 생성 가능 |
| E2E-001-07 | 인증 없이 동의 저장 | 토큰 없음 | 401 UNAUTHORIZED | 표준 오류 포맷 |

---

### E2E-002: 일정 관리 플로우

**유형**: E2E
**관련 서비스**: AUTH -> SCHD -> PLCE
**유저스토리**: 로그인한 사용자가 도쿄 여행을 생성하고 라멘 맛집을 검색하여 일정에 추가한다.

#### 사전 조건
- AUTH, SCHD, PLCE 서비스 모두 기동
- 유효한 JWT access_token 보유

#### 시나리오 흐름

```
1. POST /api/v1/trips (SCHD:8082)
   입력: { "name": "도쿄 3박4일", "start_date": "2026-03-15", "end_date": "2026-03-18", "city": "도쿄" }
   기대: 201 Created, trip_id 반환

2. GET /api/v1/trips (SCHD:8082)
   기대: 200 OK, 생성한 여행이 목록에 포함

3. GET /api/v1/places/search?keyword=이치란&city=도쿄 (PLCE:8083)
   기대: 200 OK, 장소 목록 반환 (Google API 키 없는 환경에서는 빈 목록)

4. POST /api/v1/trips/{trip_id}/schedule-items (SCHD:8082)
   입력: { "place_id": "place_abc123", "visit_datetime": "2026-03-16T12:00:00", "timezone": "Asia/Tokyo" }
   기대: 201 Created, schedule_item_id 반환

5. GET /api/v1/trips/{trip_id}/schedule (SCHD:8082)
   기대: 200 OK, schedule_items에 추가한 장소 포함

6. DELETE /api/v1/trips/{trip_id}/schedule-items/{item_id} (SCHD:8082)
   기대: 204 No Content
```

#### 테스트 케이스 목록

| TC-ID | 단계 | 입력 | 기대 결과 | 검증 포인트 |
|-------|------|------|----------|-----------|
| E2E-002-01 | 여행 생성 성공 | name/start_date/end_date/city | 201, trip_id 존재 | status=ACTIVE |
| E2E-002-02 | 여행명 50자 초과 | name 51자 | 400, VALIDATION_ERROR | 필드 오류 메시지 |
| E2E-002-03 | 종료일 < 시작일 | end_date < start_date | 400, VALIDATION_ERROR | 날짜 검증 오류 |
| E2E-002-04 | 인증 없이 생성 | 토큰 없음 | 401 UNAUTHORIZED | 표준 오류 포맷 |
| E2E-002-05 | 장소 검색 | keyword=이치란&city=도쿄 | 200, places 배열 | 검색 결과 구조 확인 |
| E2E-002-06 | 장소 추가 성공 | place_id + visit_datetime | 201, schedule_item_id | place_name 스냅샷 확인 |
| E2E-002-07 | place_id 누락 | visit_datetime만 전달 | 400, VALIDATION_ERROR | 필드 오류 메시지 |
| E2E-002-08 | 존재하지 않는 여행에 장소 추가 | 없는 trip_id | 404, RESOURCE_NOT_FOUND | error_code 확인 |
| E2E-002-09 | 일정표 조회 | 생성된 trip_id | 200, schedule_items 배열 | 추가한 장소 포함 확인 |
| E2E-002-10 | 장소 삭제 | schedule_item_id | 204 No Content | 삭제 후 목록에서 제거 |

---

### E2E-003: 실시간 모니터링 플로우

**유형**: E2E
**관련 서비스**: MNTR -> BRIF
**유저스토리**: 일정표 화면에서 각 장소의 실시간 상태를 배지로 확인하고, 상태 변화 시 브리핑을 수신한다.

#### 사전 조건
- MNTR 서비스 기동 (port 8084), BRIF 서비스 기동 (port 8085)
- 유효한 JWT access_token 보유

#### 시나리오 흐름

```
1. GET /api/v1/badges?place_ids=place_abc123,place_def456 (MNTR:8084)
   기대: 200 OK, badges 배열 (각 배지의 status_label 포함)

2. GET /api/v1/badges/{place_id}/detail (MNTR:8084)
   기대: 200 OK, 상태 상세 정보 (날씨/혼잡도/영업시간/교통 4개 지표)

3. POST /api/v1/briefings/generate (BRIF:8085 내부 API)
   헤더: X-Internal-Service-Key
   입력: { "schedule_item_id": "si_001", "place_id": "place_abc123",
           "user_id": "usr_001", "departure_time": "2026-03-16T09:00:00" }
   기대: 201 Created, briefing_id + status=CREATED

4. GET /api/v1/briefings (BRIF:8085)
   헤더: Authorization Bearer 토큰
   기대: 200 OK, briefings 목록
```

#### 테스트 케이스 목록

| TC-ID | 단계 | 입력 | 기대 결과 | 검증 포인트 |
|-------|------|------|----------|-----------|
| E2E-003-01 | 배지 목록 조회 | place_ids=place_abc123 | 200, badges[0].status_label | GREY/GREEN/YELLOW/RED 중 하나 |
| E2E-003-02 | 복수 배지 조회 | place_ids=a,b,c | 200, badges.length=3 | 각 place_id 매핑 확인 |
| E2E-003-03 | place_ids 누락 | 파라미터 없음 | 400, VALIDATION_ERROR | 표준 오류 포맷 |
| E2E-003-04 | 배지 상세 조회 | 유효한 place_id | 200, 4개 지표 포함 | weather/crowding/business_hours/traffic |
| E2E-003-05 | 브리핑 생성 | 내부 서비스 키 + 필수 필드 | 201, briefing_id | status=CREATED |
| E2E-003-06 | 브리핑 생성 멱등성 | 동일 요청 재전송 | 200, status=EXISTING | 중복 생성 방지 |
| E2E-003-07 | 내부 키 없이 브리핑 생성 | 키 헤더 누락 | 401 UNAUTHORIZED | 내부 API 보호 확인 |
| E2E-003-08 | 브리핑 목록 조회 | Bearer 토큰 | 200, briefings 배열 | 날짜별 정렬 확인 |

---

### E2E-004: 대안 검색 플로우

**유형**: E2E
**관련 서비스**: ALTN -> PLCE + MNTR + PAY
**유저스토리**: 브리핑에서 대안 탐색을 진입하면 Paywall 확인 후 구독하고, 점수 순으로 정렬된 대안 장소 카드 3장을 확인하여 하나를 선택하면 일정이 교체된다.

#### 사전 조건
- ALTN(8086), PLCE(8083), MNTR(8084), PAY(8087), SCHD(8082) 서비스 기동
- FREE 티어 및 TRIP_PASS 티어 사용자의 JWT access_token 보유

#### 시나리오 흐름

```
1. POST /api/v1/alternatives/search (ALTN:8086) -- FREE 티어
   기대: 402 Payment Required (Paywall)

2. GET /api/v1/subscriptions/plans (PAY:8087)
   기대: 200 OK, plans 목록 (TRIP_PASS + PRO)

3. POST /api/v1/subscriptions/purchase (PAY:8087)
   입력: { "plan_id": "plan_trip_pass", "receipt": "dummy_receipt", "provider": "apple" }
   기대: 201 Created, subscription_id + new_access_token (tier=TRIP_PASS)

4. POST /api/v1/alternatives/search (ALTN:8086) -- TRIP_PASS 티어
   입력: { "place_id": "place_abc123", "category": "restaurant",
           "location": { "lat": 35.6585, "lng": 139.7454 } }
   기대: 200 OK, cards 3장 (score 내림차순 정렬)

5. POST /api/v1/alternatives/{alt_id}/select (ALTN:8086)
   입력: { "original_place_id": "place_abc123", "schedule_item_id": "si_001",
           "trip_id": "trip_001", "selected_rank": 1, "elapsed_seconds": 12 }
   기대: 200 OK, 교체 결과 반환

6. GET /api/v1/trips/{trip_id}/schedule (SCHD:8082)
   기대: 200 OK, 교체된 장소가 일정에 반영됨 확인
```

#### 테스트 케이스 목록

| TC-ID | 단계 | 입력 | 기대 결과 | 검증 포인트 |
|-------|------|------|----------|-----------|
| E2E-004-01 | 대안 카드 검색 (FREE) | FREE 토큰 | 402, PAYMENT_REQUIRED | Paywall 진입 |
| E2E-004-02 | 구독 플랜 목록 조회 | Bearer 토큰 | 200, plans 배열 | TRIP_PASS + PRO 포함 |
| E2E-004-03 | Apple IAP 구독 구매 | plan_id + receipt + provider=apple | 201, new_access_token | tier=TRIP_PASS 반영 |
| E2E-004-04 | 대안 카드 3장 조회 (TRIP_PASS) | TRIP_PASS 토큰 + 필수 필드 | 200, cards.length=3 | score 내림차순 정렬 |
| E2E-004-05 | 대안 선택 후 일정 교체 | alt_id + schedule_item_id | 200, 교체 결과 | original_place/new_place 포함 |
| E2E-004-06 | 교체 후 일정표 반영 확인 | trip_id 일정표 조회 | 200, 새 장소 포함 | 크로스 서비스 연동 확인 |
| E2E-004-07 | place_id 누락 | location + category만 전달 | 400, VALIDATION_ERROR | 필드 오류 메시지 |
| E2E-004-08 | 인증 없이 대안 검색 | 토큰 없음 | 401 UNAUTHORIZED | 표준 오류 포맷 |

---

### E2E-005: 토큰 관리 플로우

**유형**: E2E
**관련 서비스**: AUTH
**유저스토리**: Access Token이 만료되면 Refresh Token으로 자동 갱신하여 사용자 세션이 유지되고, 로그아웃하면 모든 토큰이 무효화된다.

#### 사전 조건
- AUTH 서비스 기동 (port 8081)
- 유효한 JWT access_token + refresh_token 보유

#### 시나리오 흐름

```
1. POST /api/v1/auth/social-login (AUTH:8081)
   기대: 200 OK, access_token + refresh_token 발급

2. POST /api/v1/auth/token/refresh (AUTH:8081)
   입력: { "refresh_token": "<step1에서 발급된 토큰>" }
   기대: 200 OK, 새 access_token + expires_in

3. POST /api/v1/auth/logout (AUTH:8081)
   헤더: Authorization: Bearer <access_token>
   입력: { "refresh_token": "<refresh_token>" }
   기대: 204 No Content

4. POST /api/v1/auth/token/refresh (로그아웃 후 재시도)
   입력: { "refresh_token": "<step1에서 발급된 토큰>" }
   기대: 401, INVALID_REFRESH_TOKEN (블랙리스트 확인)
```

#### 테스트 케이스 목록

| TC-ID | 단계 | 입력 | 기대 결과 | 검증 포인트 |
|-------|------|------|----------|-----------|
| E2E-005-01 | 유효한 Refresh Token 갱신 | 유효한 refresh_token | 200, 새 access_token | expires_in 존재 |
| E2E-005-02 | 만료된 Refresh Token | 만료된 토큰 | 401, INVALID_REFRESH_TOKEN | error_code 확인 |
| E2E-005-03 | 변조된 Refresh Token | 변조된 JWT | 401, INVALID_REFRESH_TOKEN | 서명 검증 실패 |
| E2E-005-04 | refresh_token 필드 누락 | 빈 바디 | 400, VALIDATION_ERROR | 필드 오류 메시지 |
| E2E-005-05 | 로그아웃 성공 | Bearer 토큰 + refresh_token | 204 No Content | 응답 바디 없음 |
| E2E-005-06 | 인증 없이 로그아웃 | 토큰 없음 | 401 UNAUTHORIZED | 표준 오류 포맷 |
| E2E-005-07 | 로그아웃 후 재갱신 시도 | 로그아웃된 토큰 | 401, INVALID_REFRESH_TOKEN | 블랙리스트 확인 |

---

## 3. E2E 테스트 우선순위

### 3-1. 우선순위 분류

| 우선순위 | 시나리오 | 이유 |
|---------|---------|------|
| P0 (즉시 실행) | E2E-001, E2E-002, E2E-005 | 인증/일정 등록/토큰 관리 -- 핵심 플로우 |
| P1 (Sprint 2) | E2E-003 | 배지/브리핑 -- Must Have 기능 |
| P2 (Sprint 3) | E2E-004 | 대안+결제 -- 외부 의존 크로스 서비스 |

### 3-2. 자동화 대상 vs 수동 검증 대상

| 구분 | 시나리오 | 비고 |
|------|---------|------|
| curl 스크립트 자동화 | E2E-001 ~ E2E-005 (부분) | API 레벨 체인 검증 |
| 수동/실기기 검증 필요 | FCM Push 수신, IAP 실결제, Flutter UI | 외부 서비스 의존 |

### 3-3. 테스트 제약사항

| 제약사항 | 영향 | 대응 |
|---------|------|------|
| Google OAuth API 키 없음 | 소셜 로그인 실제 토큰 발급 불가 | 401 정상 처리 확인, 인증 없는 API는 직접 검증 |
| Google Places API 키 없음 | 장소 검색 시 빈 결과 반환 | 빈 목록 200 OK 확인 |
| FCM 설정 없음 | Push 수신 미확인 | Phase 2에서 실기기 검증 |
| IAP Sandbox 없음 | 실결제 플로우 미확인 | Mock IAP로 구독 활성화 확인 |

### 3-4. 완료 기준 (Definition of Done)

- [x] E2E 테스트 시나리오 5개 문서화 완료
- [x] curl 기반 E2E 스크립트 작성 완료
- [ ] 7개 서비스 docker-compose 기동 후 E2E 스크립트 실행
- [ ] FCM Push 수신 실기기 수동 확인 (Phase 2)
- [ ] Apple Sandbox IAP 수동 확인 (Phase 2)
