# 통합 테스트 케이스

## 1. 테스트 범위

| 서비스 | 엔드포인트 수 | 테스트 케이스 수 |
|--------|-------------|----------------|
| AUTH   | 5           | 16             |
| SCHD   | 5           | 18             |
| PLCE   | 3           | 12             |
| MNTR   | 3           | 10             |
| BRIF   | 3           | 11             |
| ALTN   | 2           | 9              |
| PAY    | 3           | 10             |
| **합계** | **24**    | **86**         |

---

## 2. 테스트 케이스 목록

### AUTH Service

| ID | 엔드포인트 | 메서드 | 시나리오 | 입력 | 기대 결과 | 유형 |
|----|-----------|--------|---------|------|----------|------|
| IT-AUTH-001 | /api/v1/auth/social-login | POST | Google 소셜 로그인 성공 | provider=google, oauth_code=valid_mock_code | 200, access_token + refresh_token + user_profile 반환 | Happy |
| IT-AUTH-002 | /api/v1/auth/social-login | POST | Apple 소셜 로그인 성공 | provider=apple, oauth_code=valid_mock_code | 200, access_token + refresh_token + user_profile 반환 | Happy |
| IT-AUTH-003 | /api/v1/auth/social-login | POST | provider 필드 누락 | oauth_code만 전달 | 400 BAD_REQUEST | Validation |
| IT-AUTH-004 | /api/v1/auth/social-login | POST | oauth_code 필드 누락 | provider만 전달 | 400 BAD_REQUEST | Validation |
| IT-AUTH-005 | /api/v1/auth/social-login | POST | 지원하지 않는 provider | provider=kakao | 400 BAD_REQUEST | Validation |
| IT-AUTH-006 | /api/v1/auth/token/refresh | POST | 유효한 Refresh Token으로 갱신 성공 | 유효한 refresh_token | 200, 새 access_token + expires_in | Happy |
| IT-AUTH-007 | /api/v1/auth/token/refresh | POST | 만료된 Refresh Token | 만료된 refresh_token | 401 UNAUTHORIZED | Validation |
| IT-AUTH-008 | /api/v1/auth/token/refresh | POST | refresh_token 필드 누락 | 빈 바디 | 400 BAD_REQUEST | Validation |
| IT-AUTH-009 | /api/v1/auth/logout | POST | 인증된 사용자 로그아웃 | Bearer 토큰 + refresh_token | 204 No Content | Happy |
| IT-AUTH-010 | /api/v1/auth/logout | POST | 인증 토큰 없이 로그아웃 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-AUTH-011 | /api/v1/auth/token/invalidate | POST | 구독 티어 변경 후 토큰 재발급 | user_id + new_tier=TRIP_PASS | 200, 새 access_token + tier | Happy |
| IT-AUTH-012 | /api/v1/auth/token/invalidate | POST | 인증 없이 토큰 무효화 요청 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-AUTH-013 | /api/v1/users/consent | POST | 위치/푸시 동의 저장 성공 | Bearer 토큰 + location=true + push=true | 201, consent_id 반환 | Happy |
| IT-AUTH-014 | /api/v1/users/consent | POST | 인증 없이 동의 저장 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-AUTH-015 | /api/v1/users/consent | POST | timestamp 필드 누락 | location + push만 전달 | 400 BAD_REQUEST | Validation |
| IT-AUTH-016 | /api/v1/users/consent | POST | location 필드 누락 | push + timestamp만 전달 | 400 BAD_REQUEST | Validation |

### SCHD Service

| ID | 엔드포인트 | 메서드 | 시나리오 | 입력 | 기대 결과 | 유형 |
|----|-----------|--------|---------|------|----------|------|
| IT-SCHD-001 | /api/v1/trips | POST | 여행 일정 생성 성공 | Bearer 토큰 + name/start_date/end_date/city | 201, trip_id + ACTIVE 상태 반환 | Happy |
| IT-SCHD-002 | /api/v1/trips | POST | 여행명 누락 | name 없음 | 400 BAD_REQUEST | Validation |
| IT-SCHD-003 | /api/v1/trips | POST | 여행명 50자 초과 | name 51자 | 400 BAD_REQUEST | Validation |
| IT-SCHD-004 | /api/v1/trips | POST | 종료일이 시작일보다 이전 | end_date < start_date | 400 BAD_REQUEST | Validation |
| IT-SCHD-005 | /api/v1/trips | POST | 인증 없이 여행 생성 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-SCHD-006 | /api/v1/trips/{trip_id} | GET | 여행 상세 조회 성공 | 유효한 trip_id | 200, 여행 정보 반환 | Happy |
| IT-SCHD-007 | /api/v1/trips/{trip_id} | GET | 존재하지 않는 여행 조회 | 없는 trip_id | 404 NOT_FOUND | Validation |
| IT-SCHD-008 | /api/v1/trips/{trip_id} | GET | 인증 없이 조회 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-SCHD-009 | /api/v1/trips/{trip_id}/schedule | GET | 일정표 조회 성공 | 유효한 trip_id | 200, schedule_items 목록 반환 | Happy |
| IT-SCHD-010 | /api/v1/trips/{trip_id}/schedule-items | POST | 장소 추가 성공 (영업시간 내) | place_id + visit_datetime + timezone | 201, schedule_item_id 반환 | Happy |
| IT-SCHD-011 | /api/v1/trips/{trip_id}/schedule-items | POST | 장소 추가 - place_id 누락 | visit_datetime만 전달 | 400 BAD_REQUEST | Validation |
| IT-SCHD-012 | /api/v1/trips/{trip_id}/schedule-items | POST | 인증 없이 장소 추가 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-SCHD-013 | /api/v1/trips/{trip_id}/schedule-items | POST | 영업시간 외 추가 시도 (force=false) | 영업시간 외 visit_datetime, force=false | 200, OUTSIDE_BUSINESS_HOURS 경고 | Happy |
| IT-SCHD-014 | /api/v1/trips/{trip_id}/schedule-items | POST | 영업시간 외 강제 추가 (force=true) | 영업시간 외 visit_datetime, force=true | 201, outside_business_hours=true | Happy |
| IT-SCHD-015 | /api/v1/trips/{trip_id}/schedule-items/{item_id} | DELETE | 장소 삭제 성공 | 유효한 trip_id + item_id | 204 No Content | Happy |
| IT-SCHD-016 | /api/v1/trips/{trip_id}/schedule-items/{item_id} | DELETE | 존재하지 않는 아이템 삭제 | 없는 item_id | 404 NOT_FOUND | Validation |
| IT-SCHD-017 | /api/v1/trips/{trip_id}/schedule-items/{item_id}/replace | PUT | 장소 교체 성공 | new_place_id | 200, 교체 결과 반환 | Happy |
| IT-SCHD-018 | /api/v1/trips/{trip_id}/schedule-items/{item_id}/replace | PUT | PLCE 서비스 연동 - 장소 유효성 검증 | 유효하지 않은 place_id | 400 BAD_REQUEST | Cross-Service |

### PLCE Service

| ID | 엔드포인트 | 메서드 | 시나리오 | 입력 | 기대 결과 | 유형 |
|----|-----------|--------|---------|------|----------|------|
| IT-PLCE-001 | /api/v1/places/search | GET | 키워드 검색 성공 | keyword=시부야 라멘 + city=도쿄 | 200, places 목록 반환 (최대 10개) | Happy |
| IT-PLCE-002 | /api/v1/places/search | GET | 검색어 1자 (최소 길이 미달) | keyword=라 + city=도쿄 | 400 BAD_REQUEST | Validation |
| IT-PLCE-003 | /api/v1/places/search | GET | keyword 파라미터 누락 | city만 전달 | 400 BAD_REQUEST | Validation |
| IT-PLCE-004 | /api/v1/places/search | GET | city 파라미터 누락 | keyword만 전달 | 400 BAD_REQUEST | Validation |
| IT-PLCE-005 | /api/v1/places/search | GET | 인증 없이 검색 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-PLCE-006 | /api/v1/places/{place_id} | GET | 장소 상세 조회 성공 | 유효한 place_id | 200, 상세 정보 반환 | Happy |
| IT-PLCE-007 | /api/v1/places/{place_id} | GET | 존재하지 않는 장소 조회 | 없는 place_id | 404 NOT_FOUND | Validation |
| IT-PLCE-008 | /api/v1/places/{place_id} | GET | 인증 없이 상세 조회 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-PLCE-009 | /api/v1/places/nearby | GET | 주변 장소 검색 성공 | lat + lng + category + radius=1000 | 200, 주변 장소 목록 반환 | Happy |
| IT-PLCE-010 | /api/v1/places/nearby | GET | 지원하지 않는 radius 값 | radius=500 | 400 BAD_REQUEST | Validation |
| IT-PLCE-011 | /api/v1/places/nearby | GET | lat 파라미터 누락 | lng + category + radius만 전달 | 400 BAD_REQUEST | Validation |
| IT-PLCE-012 | /api/v1/places/nearby | GET | 인증 없이 주변 검색 | 토큰 없음 | 401 UNAUTHORIZED | Auth |

### MNTR Service

| ID | 엔드포인트 | 메서드 | 시나리오 | 입력 | 기대 결과 | 유형 |
|----|-----------|--------|---------|------|----------|------|
| IT-MNTR-001 | /api/v1/badges | GET | 배지 목록 조회 성공 | place_ids=place_abc123,place_def456 | 200, badges 목록 반환 | Happy |
| IT-MNTR-002 | /api/v1/badges | GET | 단일 장소 배지 조회 | place_ids=place_abc123 | 200, 배지 1개 반환 | Happy |
| IT-MNTR-003 | /api/v1/badges | GET | place_ids 파라미터 누락 | 파라미터 없음 | 400 BAD_REQUEST | Validation |
| IT-MNTR-004 | /api/v1/badges | GET | 인증 없이 배지 조회 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-MNTR-005 | /api/v1/badges/{place_id}/detail | GET | 상태 상세 조회 성공 | 유효한 place_id | 200, 상세 상태 정보 반환 | Happy |
| IT-MNTR-006 | /api/v1/badges/{place_id}/detail | GET | YELLOW 상태 장소 - 대안 버튼 포함 | YELLOW 상태 place_id | 200, show_alternative_button=true | Happy |
| IT-MNTR-007 | /api/v1/badges/{place_id}/detail | GET | 존재하지 않는 장소 상태 조회 | 없는 place_id | 404 NOT_FOUND | Validation |
| IT-MNTR-008 | /api/v1/badges/{place_id}/detail | GET | 인증 없이 상태 상세 조회 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-MNTR-009 | /api/v1/monitor/collect | POST | 데이터 수집 트리거 성공 | X-Internal-Service-Key + triggered_by | 202 ACCEPTED, job_id 반환 | Happy |
| IT-MNTR-010 | /api/v1/monitor/collect | POST | 내부 서비스 키 없이 수집 트리거 | 키 없음 | 401 UNAUTHORIZED | Auth |

### BRIF Service

| ID | 엔드포인트 | 메서드 | 시나리오 | 입력 | 기대 결과 | 유형 |
|----|-----------|--------|---------|------|----------|------|
| IT-BRIF-001 | /api/v1/briefings/{briefing_id} | GET | 브리핑 상세 조회 성공 (SAFE) | 유효한 briefing_id | 200, SAFE 브리핑 내용 반환 | Happy |
| IT-BRIF-002 | /api/v1/briefings/{briefing_id} | GET | 만료된 브리핑 조회 | 만료된 briefing_id | 200, expired=true | Happy |
| IT-BRIF-003 | /api/v1/briefings/{briefing_id} | GET | WARNING 브리핑 - 대안 링크 포함 | WARNING 상태 briefing_id | 200, alternative_link != null | Happy |
| IT-BRIF-004 | /api/v1/briefings/{briefing_id} | GET | 존재하지 않는 브리핑 조회 | 없는 briefing_id | 404 NOT_FOUND | Validation |
| IT-BRIF-005 | /api/v1/briefings/{briefing_id} | GET | 타인의 브리핑 접근 | 소유권 없는 briefing_id | 403 FORBIDDEN | Auth |
| IT-BRIF-006 | /api/v1/briefings/{briefing_id} | GET | 인증 없이 브리핑 조회 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-BRIF-007 | /api/v1/briefings | GET | 브리핑 목록 조회 성공 | Bearer 토큰 | 200, briefings 목록 반환 | Happy |
| IT-BRIF-008 | /api/v1/briefings | GET | 특정 날짜 브리핑 목록 조회 | date=2026-03-16 | 200, 해당 날짜 briefings 반환 | Happy |
| IT-BRIF-009 | /api/v1/briefings | GET | 인증 없이 목록 조회 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-BRIF-010 | /api/v1/briefings/generate | POST | 브리핑 생성 성공 | X-Internal-Service-Key + schedule_item_id + place_id + user_id + departure_time | 201, briefing_id + CREATED | Happy |
| IT-BRIF-011 | /api/v1/briefings/generate | POST | 중복 요청 - 멱등성 처리 | 동일한 schedule_item_id + departure_time | 200, EXISTING 상태 반환 | Happy |

### ALTN Service

| ID | 엔드포인트 | 메서드 | 시나리오 | 입력 | 기대 결과 | 유형 |
|----|-----------|--------|---------|------|----------|------|
| IT-ALTN-001 | /api/v1/alternatives/search | POST | 대안 카드 검색 성공 (TRIP_PASS 사용자) | TRIP_PASS 토큰 + place_id + category + location | 200, cards 3장 반환 | Happy |
| IT-ALTN-002 | /api/v1/alternatives/search | POST | FREE 티어 Paywall | FREE 토큰 + place_id + category + location | 402 Payment Required | Happy |
| IT-ALTN-003 | /api/v1/alternatives/search | POST | place_id 누락 | category + location만 전달 | 400 BAD_REQUEST | Validation |
| IT-ALTN-004 | /api/v1/alternatives/search | POST | location 누락 | place_id + category만 전달 | 400 BAD_REQUEST | Validation |
| IT-ALTN-005 | /api/v1/alternatives/search | POST | 인증 없이 대안 검색 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-ALTN-006 | /api/v1/alternatives/search | POST | PLCE + MNTR 연동 - 대안 후보 검색 | TRIP_PASS 토큰 + 유효한 place_id | 200, PLCE/MNTR 모킹 응답 기반 cards 반환 | Cross-Service |
| IT-ALTN-007 | /api/v1/alternatives/{alt_id}/select | POST | 대안 카드 선택 및 일정 반영 성공 | alt_id + original_place_id + schedule_item_id + trip_id | 200, 교체 결과 반환 | Happy |
| IT-ALTN-008 | /api/v1/alternatives/{alt_id}/select | POST | 존재하지 않는 대안 카드 선택 | 없는 alt_id | 404 NOT_FOUND | Validation |
| IT-ALTN-009 | /api/v1/alternatives/{alt_id}/select | POST | 인증 없이 대안 선택 | 토큰 없음 | 401 UNAUTHORIZED | Auth |

### PAY Service

| ID | 엔드포인트 | 메서드 | 시나리오 | 입력 | 기대 결과 | 유형 |
|----|-----------|--------|---------|------|----------|------|
| IT-PAY-001 | /api/v1/subscriptions/plans | GET | 구독 플랜 목록 조회 성공 | Bearer 토큰 | 200, plans 목록 (Trip Pass + Pro) 반환 | Happy |
| IT-PAY-002 | /api/v1/subscriptions/plans | GET | 인증 없이 플랜 조회 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-PAY-003 | /api/v1/subscriptions/purchase | POST | Apple IAP 구독 구매 성공 | plan_id + receipt + provider=apple | 201, subscription_id + new_access_token | Happy |
| IT-PAY-004 | /api/v1/subscriptions/purchase | POST | Google Play 구독 구매 성공 | plan_id + receipt + provider=google | 201, subscription_id + new_access_token | Happy |
| IT-PAY-005 | /api/v1/subscriptions/purchase | POST | plan_id 누락 | receipt + provider만 전달 | 400 BAD_REQUEST | Validation |
| IT-PAY-006 | /api/v1/subscriptions/purchase | POST | 지원하지 않는 provider | provider=samsung_pay | 400 BAD_REQUEST | Validation |
| IT-PAY-007 | /api/v1/subscriptions/purchase | POST | 인증 없이 구매 요청 | 토큰 없음 | 401 UNAUTHORIZED | Auth |
| IT-PAY-008 | /api/v1/subscriptions/purchase | POST | AUTH 연동 - 구독 후 토큰 재발급 | 유효한 purchase 요청 | 201, new_access_token에 tier 반영 | Cross-Service |
| IT-PAY-009 | /api/v1/subscriptions/status | GET | FREE 티어 구독 상태 조회 | FREE 토큰 | 200, tier=FREE, subscription_id=null | Happy |
| IT-PAY-010 | /api/v1/subscriptions/status | GET | 인증 없이 상태 조회 | 토큰 없음 | 401 UNAUTHORIZED | Auth |

---

## 3. 서비스 간 의존 관계 테스트

| ID | 호출 서비스 | 피호출 서비스 | 시나리오 | 검증 포인트 |
|----|------------|-------------|---------|-----------|
| IT-CROSS-001 | SCHD | PLCE | 일정에 장소 추가 시 PLCE 장소 유효성 조회 | PLCE Mock 응답으로 place_name 스냅샷 저장 확인 |
| IT-CROSS-002 | ALTN | PLCE | 대안 후보 검색 시 PLCE 주변 장소 조회 | PLCE Mock 응답으로 cards 구성 확인 |
| IT-CROSS-003 | ALTN | MNTR | 대안 후보 상태 조회 | MNTR Mock 응답으로 status_label 설정 확인 |
| IT-CROSS-004 | BRIF | PAY | 구독 등급 확인 (FREE 티어 한도 검증) | PAY Mock 응답으로 422 SKIPPED 반환 확인 |
| IT-CROSS-005 | PAY | AUTH | 구독 구매 후 토큰 재발급 | AUTH Mock 응답으로 new_access_token 발급 확인 |
