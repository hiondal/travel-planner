# API 명세서 현행화 변경 로그

## 변경 일시: 2026-02-26

## 현행화 기준

- 분석 대상: 각 서비스 모듈의 `src/main/java/**/controller/*.java` 및 `dto/response/*.java`
- 공통 발견 사항: 모든 응답이 `ApiResponse<T>` 래핑 구조(`success`, `data`, `errorCode`, `errorMessage`, `timestamp`)로 반환되나 기존 명세서에 미반영되어 있었음
- `LocalDateTime` 직렬화: timezone 정보 없이 ISO 8601 형식(`2026-03-16T12:00:00`)으로 직렬화됨 (기존 명세의 `+09:00` 표기와 차이)

---

## 서비스별 변경 내역

### AUTH (auth-service-api.yaml)

| 항목 | 변경 전 | 변경 후 | 사유 |
|------|--------|--------|------|
| `POST /test/login` | 명세 없음 | 신규 추가 | `TestAuthController` (@Profile("dev")) 구현 발견. dev 환경 전용 토큰 발급 엔드포인트 |
| `POST /auth/social-login` provider enum | `[google, apple]` | `[google]` | 실제 구현이 Google OAuth만 지원, Apple 미구현 |
| `POST /auth/social-login` 응답 스키마 | `SocialLoginResponse` (래핑 없음) | `ApiResponse<SocialLoginResponse>` | 공통 ApiResponse 래핑 구조 반영 |
| `POST /auth/token/refresh` 응답 스키마 | `TokenRefreshResponse` (래핑 없음) | `ApiResponse<TokenRefreshResponse>` | 공통 ApiResponse 래핑 구조 반영 |
| `POST /auth/token/invalidate` 응답 스키마 | `TokenInvalidateResponse` (래핑 없음) | `ApiResponse<TokenInvalidateResponse>` | 공통 ApiResponse 래핑 구조 반영 |
| `POST /users/consent` 응답 스키마 | `ConsentResponse` (래핑 없음) | `ApiResponse<ConsentResponse>` | 공통 ApiResponse 래핑 구조 반영 |
| `ConsentResponse.consented_at` 포맷 | `date-time` with timezone | LocalDateTime (timezone 없음) | Java LocalDateTime 직렬화 결과 반영 |
| 에러 응답 스키마 | `{error, message, timestamp}` | `{success, errorCode, errorMessage, timestamp}` | ApiResponse.error() 실제 구조 반영 |

### SCHEDULE (schedule-service-api.yaml)

| 항목 | 변경 전 | 변경 후 | 사유 |
|------|--------|--------|------|
| `GET /trips` | 명세 없음 | 신규 추가 | `SchdController.getTrips()` 구현 발견 (SCHD-00) |
| `DELETE /trips/{tripId}` | 명세 없음 | 신규 추가 | `SchdController.deleteTrip()` 구현 발견 |
| `GET /trips/{tripId}/schedule` date 파라미터 | 없음 | `?date=YYYY-MM-DD` 쿼리 파라미터 추가 | `@RequestParam(required = false) String date` 구현 반영 |
| `TripListResponse` 스키마 | 없음 (명세 자체 없음) | `{trips: TripResponse[], total: int}` | `TripListResponse.of()` 실제 필드 반영 |
| 경로 파라미터명 | `{trip_id}`, `{item_id}` | `{tripId}`, `{itemId}` | Controller `@PathVariable String tripId` 실제 변수명 반영 |
| `PlaceRef` 스키마 필드 | `place_id`, `place_name` | `place_id`, `place_name` | `PlaceRef.java` 확인, 동일하게 유지 |
| `ScheduleItemSummary.visit_datetime` | `date-time` with timezone | LocalDateTime (timezone 없음) | Java LocalDateTime 직렬화 결과 반영 |
| 모든 응답 스키마 | 래핑 없음 | `ApiResponse<T>` 래핑 구조 | 공통 ApiResponse 래핑 구조 반영 |
| 에러 응답 스키마 | `{error, message, timestamp}` | `{success, errorCode, errorMessage, timestamp}` | ApiResponse.error() 실제 구조 반영 |
| `BusinessHoursWarningResponse` | ApiResponse 래핑 있음으로 가정 | ApiResponse 래핑 없음 (plain 반환) | Controller에서 `ApiResponse.ok()` 미사용, 직접 반환 확인 |

### PLACE (place-service-api.yaml)

| 항목 | 변경 전 | 변경 후 | 사유 |
|------|--------|--------|------|
| `GET /places/search` security | `BearerAuth` 필요 | 인증 불필요 (security 제거) | Controller에 `@AuthenticationPrincipal` 없음, 공개 API |
| `GET /places/{place_id}` security | `BearerAuth` 필요 | 인증 불필요 (security 제거) | Controller에 `@AuthenticationPrincipal` 없음, 공개 API |
| `GET /places/nearby` security | `BearerAuth` 필요 | 인증 불필요 (security 제거) | Controller에 `@AuthenticationPrincipal` 없음, 공개 API |
| `GET /places/search` 캐시 TTL | 5분 | 30분 | Controller description 실제 값 반영 |
| 모든 응답 스키마 | 래핑 없음 | `ApiResponse<T>` 래핑 구조 | 공통 ApiResponse 래핑 구조 반영 |
| `radius` 유효성 검사 | 명세만 존재 | 1000/2000/3000 외 400 반환 명시 | `ALLOWED_RADII` Set 기반 ValidationException 구현 반영 |
| `keyword` 유효성 검사 | 명세만 존재 | 2자 미만 시 400 반환 명시 | `MIN_KEYWORD_LENGTH = 2` 기반 ValidationException 구현 반영 |
| 에러 응답 스키마 | `{error, message, timestamp}` | `{success, errorCode, errorMessage, timestamp}` | ApiResponse.error() 실제 구조 반영 |

### MONITOR (monitor-service-api.yaml)

| 항목 | 변경 전 | 변경 후 | 사유 |
|------|--------|--------|------|
| `POST /badges/{placeId}/refresh` | 명세 없음 | 신규 추가 | `MntrController.refreshPlaceStatus()` 구현 발견 (MNTR-03) |
| `POST /badges/{placeId}/refresh` 429 응답 | 없음 | 429 Too Many Requests 추가 | 60초 rate limit Redis 구현 반영 |
| `POST /badges/{placeId}/refresh` 404 우선순위 | 없음 | rate limit 전에 대상 존재 여부 확인 명시 | 0188767 커밋 로직 반영 |
| 경로 파라미터명 | `{place_id}` | `{placeId}` | Controller `@PathVariable String placeId` 실제 변수명 반영 |
| `POST /monitor/collect` security | `InternalServiceKey` scheme | `X-Internal-Service-Key` 헤더 파라미터로 변경 | Controller에서 `@RequestHeader` 직접 검증, 스키마 방식과 다름 |
| `POST /monitor/collect` 401 응답 | ApiResponse 래핑 | plain Map(`{error, message}`) | Controller에서 `Map.of()` 직접 반환 확인 |
| `BadgeItem.updated_at` 포맷 | `date-time` with timezone | LocalDateTime (timezone 없음) | Java LocalDateTime 직렬화 결과 반영 |
| `CollectTriggerResponse` | ApiResponse 래핑 있음으로 가정 | ApiResponse 래핑 없음 (직접 반환) | Controller에서 `ApiResponse.ok()` 미사용 확인 |
| 모든 배지/상태 응답 스키마 | 래핑 없음 | `ApiResponse<T>` 래핑 구조 | 공통 ApiResponse 래핑 구조 반영 |
| 에러 응답 스키마 | `{error, message, timestamp}` | `{success, errorCode, errorMessage, timestamp}` | ApiResponse.error() 실제 구조 반영 |

### BRIEFING (briefing-service-api.yaml)

| 항목 | 변경 전 | 변경 후 | 사유 |
|------|--------|--------|------|
| `BriefingListItem` 스키마 | `{briefing_id, type, place_name, created_at, expired}` | `place_id` 필드 추가 | `BriefingListItemDto.java`에 `place_id` 필드 구현 발견 |
| `GET /briefings/{briefing_id}` 경로 파라미터 | `{briefing_id}` | `{briefingId}` | Controller `@PathVariable String briefingId` 실제 변수명 반영 |
| `BriefingDetailResponse.departure_time` 포맷 | `date-time` with timezone | LocalDateTime (timezone 없음) | Java LocalDateTime 직렬화 결과 반영 |
| `BriefingDetailResponse.created_at` 포맷 | `date-time` with timezone | LocalDateTime (timezone 없음) | Java LocalDateTime 직렬화 결과 반영 |
| `POST /briefings/generate` security | `InternalServiceKey` 필수 | security 제거 (헤더 검증 없음) | Controller에 서비스 키 검증 로직 없음 확인 |
| `POST /briefings/generate` 422 응답 | `BriefingSkippedResponse` (래핑 미지정) | plain Map(`{status, reason, message}`)으로 명시 | Controller에서 `Map.of()` 직접 반환 확인 |
| `POST /briefings/generate` 201/200 응답 | ApiResponse 래핑 있음으로 가정 | `GenerateBriefingResponse` 직접 반환 | Controller에서 `ApiResponse.ok()` 미사용 확인 |
| 브리핑 조회/목록 응답 스키마 | 래핑 없음 | `ApiResponse<T>` 래핑 구조 | 공통 ApiResponse 래핑 구조 반영 |
| 에러 응답 스키마 | `{error, message, timestamp}` | `{success, errorCode, errorMessage, timestamp}` | ApiResponse.error() 실제 구조 반영 |

### ALTERNATIVE (alternative-service-api.yaml)

| 항목 | 변경 전 | 변경 후 | 사유 |
|------|--------|--------|------|
| `AlternativeSearchResponse.cards` | `cards` 필드명 | `alternatives` 필드명으로 변경 | `AlternativeSearchResponse.java` `@JsonProperty("alternatives")` 확인 |
| `AlternativeCard` 스키마 | `rank` 필드 없음 | `rank` 필드 추가 (int, 1~3) | `AlternativeCardDto.java` `@JsonProperty("rank")` 구현 발견 |
| `AlternativeCard.rating` | not nullable | `nullable: true` | `AlternativeCardDto.java` `Float rating` (래퍼 타입) 반영 |
| 경로 파라미터명 | `{alt_id}` | `{altId}` | Controller `@PathVariable String altId` 실제 변수명 반영 |
| `SelectAlternativeResponse` place 필드 | `{place_id, name}` | `{place_id, name}` | `PlaceRefDto.java` 확인, 동일하게 유지 (구 명세의 `name` 필드명 유지) |
| `POST /alternatives/search` 402 응답 | `PaywallResponse` (래핑 미지정) | ApiResponse 래핑 없이 직접 반환으로 명시 | Controller에서 `new PaywallResponse()` 직접 반환 확인 |
| 성공 응답 스키마 | 래핑 없음 | `ApiResponse<T>` 래핑 구조 | 공통 ApiResponse 래핑 구조 반영 |
| 에러 응답 스키마 | `{error, message, timestamp}` | `{success, errorCode, errorMessage, timestamp}` | ApiResponse.error() 실제 구조 반영 |

### PAYMENT (payment-service-api.yaml)

| 항목 | 변경 전 | 변경 후 | 사유 |
|------|--------|--------|------|
| `GET /subscriptions/plans` security | `BearerAuth` 필요 | security 제거 (인증 불필요) | Controller에 `@AuthenticationPrincipal` 없음, 공개 API |
| `GET /subscriptions/status` 내부 호출 지원 | 명세 없음 | `userId` 쿼리 파라미터 + `X-Internal-Service-Key` 헤더 추가 | Controller 이중 호출 방식 구현 반영 |
| `GET /subscriptions/status` 내부 호출 401 | 없음 | `userId` 있고 서비스 키 불일치 시 401 명시 | `ApiResponse.error("UNAUTHORIZED", ...)` 반환 로직 반영 |
| `PurchaseResponse.started_at` 포맷 | `date-time` with timezone | LocalDateTime (timezone 없음) | Java LocalDateTime 직렬화 결과 반영 |
| `SubscriptionStatusResponse.started_at` 포맷 | `date-time` with timezone | LocalDateTime (timezone 없음) | Java LocalDateTime 직렬화 결과 반영 |
| 모든 응답 스키마 | 래핑 없음 | `ApiResponse<T>` 래핑 구조 | 공통 ApiResponse 래핑 구조 반영 |
| 에러 응답 스키마 | `{error, message, timestamp}` | `{success, errorCode, errorMessage, timestamp}` | ApiResponse.error() 실제 구조 반영 |

---

## deprecated 처리 항목

없음. 명세에만 존재하고 구현에 없는 엔드포인트는 발견되지 않았음.
구현에 있으나 명세에 없던 엔드포인트들은 모두 신규 추가 처리.

---

## 공통 변경 사항 요약

1. **servers 섹션 현행화**: 전 서비스 `@RequestMapping("/api/v1")` 반영
   - 기존: `https://api.travel-planner.com/v1` (base path `/v1` — 실제와 불일치)
   - 변경: `http://localhost:{port}/api/v1` (로컬 개발), `https://api.travel-planner.com/api/v1` (운영)
   - 서비스별 로컬 포트: AUTH 8081, SCHD 8082, PLCE 8083, MNTR 8084, BRIF 8085, ALTN 8086, PAY 8087
   - SwaggerHub Mock Server URL 제거 (더 이상 사용하지 않음)
2. **ApiResponse 래핑**: 모든 성공 응답에 `{success, data, errorCode, errorMessage, timestamp}` 래핑 구조 추가
3. **에러 응답 구조**: `{error, message, timestamp}` → `{success, errorCode, errorMessage, timestamp}` 통일
4. **LocalDateTime 직렬화**: timezone 오프셋 없는 형식(`2026-03-16T12:00:00`)으로 통일
5. **경로 파라미터 케이스**: snake_case(`trip_id`) → camelCase(`tripId`) 로 변경 (Spring PathVariable 실제 변수명)
6. **인증 요구사항 정정**: Place 서비스 3개 엔드포인트, Payment plans 엔드포인트는 인증 불필요
