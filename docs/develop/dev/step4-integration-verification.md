# Step 4 통합 연동 검증 보고서

> 작성일: 2026-02-24
> 단계: Step 4 — 서비스 간 통합 연동 검증
> 검증자: Verifier (Claude Sonnet 4.6)

---

## 1. 검증 범위 및 방법론

각 client 파일을 직접 읽어 호출 URL을 추출하고, 대상 Controller의 실제 @RequestMapping/@GetMapping/@PostMapping/@PutMapping/@DeleteMapping 어노테이션과 비교했다. DTO 필드 호환성은 @JsonProperty 어노테이션과 필드명을 대조하여 확인했다.

---

## 2. 일치 항목 목록

| # | 의존 관계 | 클라이언트 URL | 컨트롤러 실제 경로 | 판정 |
|---|-----------|----------------|-------------------|------|
| 1 | SCHD → PLCE (상세) | `GET http://localhost:8083/api/v1/places/{placeId}` | `PlceController @RequestMapping("/places") + @GetMapping("/{place_id}")` = `/places/{place_id}` | **불일치** (아래 상세) |
| 2 | ALTN → PLCE (주변) | `GET http://localhost:8083/api/v1/places/nearby?...` | `PlceController @RequestMapping("/places") + @GetMapping("/nearby")` = `/places/nearby` | **불일치** (아래 상세) |
| 3 | BRIF → MNTR (상태) | `GET http://localhost:8084/api/v1/monitor/places/{placeId}/status` | `MntrController @RequestMapping("/api/v1") + @GetMapping("/badges/{placeId}/detail")` = `/api/v1/badges/{placeId}/detail` | **불일치** (아래 상세) |
| 4 | BRIF → PAY (구독) | `GET http://localhost:8087/api/v1/subscriptions/status?userId=...` | `PayController @RequestMapping("/api/v1/subscriptions") + @GetMapping("/status")` = `/api/v1/subscriptions/status` | 경로 일치, 파라미터 불일치 (아래 상세) |
| 5 | ALTN → SCHD (교체) | `POST http://localhost:8082/api/v1/schedule-items/{scheduleItemId}/replace` | `SchdController @RequestMapping("/api/v1") + @PutMapping("/trips/{tripId}/schedule-items/{itemId}/replace")` | **불일치** (아래 상세) |
| 6 | PAY → AUTH (토큰 무효화) | `POST http://localhost:8081/api/v1/auth/token/invalidate` | `AuthController @RequestMapping("/api/v1") + @PostMapping("/auth/token/invalidate")` = `/api/v1/auth/token/invalidate` | **일치** |
| 7 | PAY → AUTH (요청 DTO) | `Map.of("user_id", userId, "new_tier", newTier)` | `TokenInvalidateRequest @JsonProperty("user_id"), @JsonProperty("new_tier")` | **일치** |
| 8 | ALTN → MNTR (배지) | `MonitorServiceClient.getBadges()` — Phase 1 Mock, HTTP 호출 없음 | N/A | **해당 없음** (Mock 전용) |

---

## 3. 불일치 항목 목록

### [불일치-1] PLCE 서비스 경로 접두사 누락 — SCHD 및 ALTN 클라이언트

**심각도: 높음 (High)**

| 항목 | 내용 |
|------|------|
| 파일 | `schedule/src/main/java/com/travelplanner/schedule/client/PlaceServiceClient.java` |
| 파일 | `alternative/src/main/java/com/travelplanner/alternative/client/PlaceServiceClient.java` |
| 클라이언트 호출 URL | `http://localhost:8083/api/v1/places/{placeId}` (SCHD) |
| 클라이언트 호출 URL | `http://localhost:8083/api/v1/places/nearby` (ALTN) |
| 실제 컨트롤러 경로 | `PlceController @RequestMapping("/places")` → `/places/{place_id}`, `/places/nearby` |
| 차이 | 클라이언트는 `/api/v1/places/...`를 호출하지만 PLCE 컨트롤러는 `/places/...`에 매핑됨. `/api/v1` 접두사 없음. |
| 영향 | 런타임 404 오류 발생. SCHD 장소 추가 기능 및 ALTN 대안 검색 기능 전체 장애. |

**수정 방법 (2가지 중 택 1):**

옵션 A — PLCE 컨트롤러에 `/api/v1` 접두사 추가 (권장, 타 서비스 패턴과 일치):
```java
// place/src/main/java/com/travelplanner/place/controller/PlceController.java
@RequestMapping("/api/v1/places")  // 기존: @RequestMapping("/places")
```

옵션 B — 클라이언트 URL에서 `/api/v1` 제거:
```java
// schedule PlaceServiceClient
baseUrl + "/places/" + placeId  // 기존: "/api/v1/places/" + placeId

// alternative PlaceServiceClient
String.format("%s/places/nearby?...", baseUrl, ...)  // 기존: "/api/v1/places/nearby"
```

---

### [불일치-2] BRIF → MNTR 엔드포인트 경로 완전 불일치

**심각도: 높음 (High)**

| 항목 | 내용 |
|------|------|
| 파일 | `briefing/src/main/java/com/travelplanner/briefing/client/MonitorServiceClient.java` |
| 클라이언트 호출 URL | `GET http://localhost:8084/api/v1/monitor/places/{placeId}/status` |
| 실제 컨트롤러 경로 | `MntrController @RequestMapping("/api/v1") + @GetMapping("/badges/{placeId}/detail")` → `/api/v1/badges/{placeId}/detail` |
| 차이 | (1) `/monitor/places/` vs `/badges/` 경로 세그먼트 상이, (2) 경로 파라미터 뒤 `/status` vs `/detail` 상이 |
| 영향 | 런타임 404 오류. BRIF 서비스가 장소 상태를 조회하지 못해 브리핑 생성 실패. |

**추가 DTO 불일치:**

- BRIF `MonitorData`는 플랫 구조: `businessStatus`, `congestion`, `weather`, `precipitationProb`, `walkingMinutes`, `transitMinutes`, `distanceM`, `overallStatus`, `placeName`
- MNTR `StatusDetailResponse`는 중첩 구조: `place_id`, `place_name`, `overall_status`, `details{businessStatus{status, value}, congestion{...}, weather{...}, travelTime{...}}`, `reason`, `show_alternative_button`, `updated_at`
- 직접 역직렬화 시 `businessStatus`, `congestion`, `weather`, `precipitationProb` 등 핵심 필드가 모두 null이 됨.

**수정 방법:**

옵션 A — MNTR 컨트롤러에 BRIF가 기대하는 단순 상태 엔드포인트 추가:
```java
// MntrController에 추가
@GetMapping("/monitor/places/{placeId}/status")
public ResponseEntity<MonitorStatusSimpleResponse> getPlaceStatusSimple(@PathVariable String placeId) { ... }
```

옵션 B — BRIF MonitorServiceClient URL 및 MonitorData를 MntrController 실제 경로/구조에 맞게 수정:
```java
// briefing MonitorServiceClient
String url = baseUrl + "/api/v1/badges/" + placeId + "/detail";
// MonitorData 구조를 StatusDetailResponse와 호환되도록 재설계
```

---

### [불일치-3] BRIF → PAY: userId 파라미터 무시

**심각도: 중간 (Medium)**

| 항목 | 내용 |
|------|------|
| 파일 | `briefing/src/main/java/com/travelplanner/briefing/client/PayServiceClient.java` |
| 클라이언트 호출 URL | `GET http://localhost:8087/api/v1/subscriptions/status?userId={userId}` |
| 실제 컨트롤러 | `PayController @GetMapping("/status")` — `@AuthenticationPrincipal`로 userId 추출, `@RequestParam userId` 없음 |
| 차이 | 경로는 일치하지만 userId 쿼리 파라미터가 컨트롤러에 없어 무시됨. Phase 1에서 userId는 하드코딩(`"user-test-001"`)으로 대체됨. |
| 영향 | Phase 1에서는 Mock으로 처리되어 숨겨짐. 실제 인증 적용 시 서비스 간 호출에서 userId 전달이 되지 않아 항상 잘못된 사용자 구독 정보 반환. |

**추가 DTO 불일치:**

- BRIF `SubscriptionInfo`가 기대하는 `todayBriefingCount` 필드가 PAY `SubscriptionStatusResponse`에 없음.
- PAY 응답: `tier`, `status`, `subscription_id`, `started_at`, `expires_at`
- BRIF 기대: `tier`, `todayBriefingCount`
- `todayBriefingCount`는 역직렬화 시 항상 0으로 설정됨 (Java 기본값). BRIF는 이 값을 활용하지 않는 것으로 확인되나 (`BriefingServiceImpl`이 tier만 사용), 향후 사용 시 오동작 위험.

**수정 방법:**

옵션 A — PAY 내부 전용 엔드포인트에 `@RequestParam userId` 추가:
```java
// PayController에 추가
@GetMapping("/internal/status")
public ResponseEntity<SubscriptionInfoResponse> getSubscriptionInfoInternal(
    @RequestParam String userId) { ... }
```

옵션 B — BRIF가 JWT 토큰을 통한 인증으로 PAY를 호출하도록 변경 (서비스 간 API 키 또는 내부 토큰 사용).

---

### [불일치-4] ALTN → SCHD: 경로 구조 불일치 (tripId 누락)

**심각도: 높음 (High)**

| 항목 | 내용 |
|------|------|
| 파일 | `alternative/src/main/java/com/travelplanner/alternative/client/ScheduleServiceClient.java` |
| 클라이언트 호출 URL | `POST http://localhost:8082/api/v1/schedule-items/{scheduleItemId}/replace` |
| 실제 컨트롤러 경로 | `SchdController @RequestMapping("/api/v1") + @PutMapping("/trips/{tripId}/schedule-items/{itemId}/replace")` → `/api/v1/trips/{tripId}/schedule-items/{itemId}/replace` |
| 차이 | (1) `POST` vs `PUT` HTTP 메서드 상이, (2) `/trips/{tripId}/` 경로 세그먼트 누락, (3) URL에 `/schedule-items/` vs 컨트롤러 `/trips/{tripId}/schedule-items/` |
| 영향 | 런타임 404 오류. 대안 카드 선택 후 일정 교체 불가. 단, Phase 1에서 실제 HTTP 호출 코드가 주석 처리되어 있어 Mock으로 처리됨. |

**수정 방법:**

```java
// alternative ScheduleServiceClient — URL 및 메서드 수정
String url = baseUrl + "/api/v1/trips/" + tripId + "/schedule-items/" + scheduleItemId + "/replace";
// RestTemplate의 PUT 사용 (현재 PostForObject 예정이었으나 SCHD는 PUT)
restTemplate.put(url, requestBody);
// 단, ScheduleServiceClient.replaceScheduleItem() 시그니처에 tripId 파라미터 추가 필요
```

---

### [불일치-5] 모든 클라이언트: ApiResponse 래퍼 역직렬화 미처리

**심각도: 높음 (High)**

| 항목 | 내용 |
|------|------|
| 해당 파일 | `schedule/.../client/PlaceServiceClient.java`, `briefing/.../client/MonitorServiceClient.java`, `briefing/.../client/PayServiceClient.java` |
| 문제 | 모든 백엔드 컨트롤러가 `ApiResponse<T>` 래퍼로 응답을 감싸 반환함: `{"success":true,"data":{...},"timestamp":"..."}` |
| 현재 코드 | `restTemplate.getForEntity(url, PlaceDetail.class)` — 루트 객체를 `PlaceDetail`로 직렬화 시도 |
| 실제 응답 구조 | `{"success":true,"data":{"place_id":"...","name":"...","business_hours":[...],...},"timestamp":"..."}` |
| 영향 | Jackson이 루트의 `success`, `data`, `timestamp` 필드를 `PlaceDetail`에 매핑할 수 없어 모든 필드가 null. `place_id`, `name` 등이 null로 반환됨. |

**수정 방법 (공통 패턴 적용):**

```java
// 방법 1: ApiResponse 래퍼를 언래핑하는 제네릭 헬퍼 사용
public PlaceDetail getPlaceDetail(String placeId) {
    ResponseEntity<ApiResponse<PlaceDetail>> response = restTemplate.exchange(
        baseUrl + "/api/v1/places/" + placeId,
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<ApiResponse<PlaceDetail>>() {}
    );
    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        return response.getBody().getData();  // data 필드 언래핑
    }
    return PlaceDetail.unknown(placeId);
}

// 방법 2: 서비스 간 통신에서 ApiResponse 없이 직접 DTO 반환하는 내부 전용 엔드포인트 사용
```

---

### [불일치-6] 프론트엔드 → SCHD: 장소 추가/삭제 경로 불일치

**심각도: 높음 (High)**

| 항목 | 내용 |
|------|------|
| 파일 | `frontend/lib/features/schedule/data/datasources/schedule_datasource.dart` |
| 프론트엔드 POST 경로 | `POST /trips/{tripId}/schedule` |
| 백엔드 실제 경로 | `POST /trips/{tripId}/schedule-items` |
| 프론트엔드 DELETE 경로 | `DELETE /trips/{tripId}/schedule/{scheduleItemId}` |
| 백엔드 실제 경로 | `DELETE /trips/{tripId}/schedule-items/{itemId}` |
| 프론트엔드 PUT 경로 | `PUT /trips/{tripId}/schedule/reorder` |
| 백엔드 실제 경로 | 해당 엔드포인트 없음 (reorder API 미구현) |
| 영향 | 장소 추가/삭제 404 오류. Prism Mock은 API 스펙 기반이므로 Mock에서는 정상이나 실제 백엔드 연결 시 전면 장애. |

**수정 방법:**

```dart
// schedule_datasource.dart 수정

// POST 장소 추가
'/trips/$tripId/schedule-items',   // 기존: '/trips/$tripId/schedule'

// DELETE 장소 삭제
'/trips/$tripId/schedule-items/$scheduleItemId',  // 기존: '/trips/$tripId/schedule/$scheduleItemId'

// PUT reorder — 백엔드 미구현이므로 엔드포인트 구현 또는 Dart 코드 제거 필요
```

---

### [불일치-7] 프론트엔드 → MNTR: 경로 완전 불일치

**심각도: 높음 (High)**

| 항목 | 내용 |
|------|------|
| 파일 | `frontend/lib/features/monitoring/data/datasources/monitoring_datasource.dart` |
| 프론트엔드 GET 경로 | `GET /monitor/places/{placeId}/status` |
| 백엔드 실제 경로 | `GET /api/v1/badges/{placeId}/detail` (MntrController) |
| 프론트엔드 GET 경로 | `GET /monitor/trips/{tripId}/status` |
| 백엔드 실제 경로 | 해당 엔드포인트 없음 (미구현) |
| 영향 | 장소 상태 조회 404 오류. 실시간 모니터링 기능 전체 장애. |

---

### [불일치-8] 프론트엔드 → ALTN: 경로 및 메서드 불일치

**심각도: 높음 (High)**

| 항목 | 내용 |
|------|------|
| 파일 | `frontend/lib/features/briefing/data/datasources/alternative_datasource.dart` |
| 프론트엔드 GET 경로 | `GET /alternatives?briefing_id={briefingId}` |
| 백엔드 실제 경로 | `POST /api/v1/alternatives/search` (AltnController) — GET 아닌 POST |
| 프론트엔드 POST 경로 | `POST /alternatives/{alternativeId}/apply` |
| 백엔드 실제 경로 | `POST /api/v1/alternatives/{altId}/select` |
| 영향 | 대안 장소 목록 조회 404. 대안 카드 적용 경로 불일치 (`apply` vs `select`). |

---

### [불일치-9] 프론트엔드 인터셉터: 토큰 갱신 경로 불일치

**심각도: 높음 (High)**

| 항목 | 내용 |
|------|------|
| 파일 | `frontend/lib/core/network/auth_interceptor.dart` |
| 인터셉터 호출 경로 | `POST /auth/refresh` |
| 백엔드 실제 경로 | `POST /api/v1/auth/token/refresh` (AuthController) |
| 영향 | 401 오류 발생 시 자동 토큰 갱신 실패 → 모든 인증 필요 기능 장애 전파. |

---

### [불일치-10] 프론트엔드 → AUTH: /auth/me 엔드포인트 미존재

**심각도: 중간 (Medium)**

| 항목 | 내용 |
|------|------|
| 파일 | `frontend/lib/features/auth/data/datasources/auth_datasource.dart` |
| 프론트엔드 GET 경로 | `GET /auth/me` |
| 백엔드 실제 경로 | 해당 엔드포인트 없음. AuthController에 `/auth/me` 없음. |
| 영향 | 사용자 프로필 조회 404 오류. 로그인 후 내 정보 조회 기능 장애. |

---

### [불일치-11] 프론트엔드 → BRIF: 브리핑 목록 쿼리 파라미터 불일치

**심각도: 중간 (Medium)**

| 항목 | 내용 |
|------|------|
| 파일 | `frontend/lib/features/briefing/data/datasources/briefing_datasource.dart` |
| 프론트엔드 파라미터 | `GET /briefings?trip_id={tripId}` |
| 백엔드 파라미터 | `@RequestParam(required = false) LocalDate date` (날짜 기반 조회) |
| 차이 | 프론트엔드는 `trip_id`로 필터링을 시도하나, 백엔드는 `date` 파라미터만 수신하고 `trip_id`는 무시됨. |
| 영향 | 특정 여행의 브리핑 필터링 불가. 항상 오늘 날짜의 전체 브리핑 반환. |

---

## 4. 수정 필요 사항 요약

### 즉시 수정 필요 (런타임 404 / 데이터 손실)

| 우선순위 | 항목 | 수정 대상 파일 | 수정 내용 |
|----------|------|----------------|-----------|
| P0 | [불일치-1] PLCE 경로 접두사 | `place/.../PlceController.java` | `@RequestMapping("/places")` → `@RequestMapping("/api/v1/places")` |
| P0 | [불일치-2] BRIF→MNTR 경로 | `briefing/.../MonitorServiceClient.java` | URL `/api/v1/monitor/places/{id}/status` → `/api/v1/badges/{id}/detail` + MonitorData DTO 재설계 |
| P0 | [불일치-4] ALTN→SCHD 경로 | `alternative/.../ScheduleServiceClient.java` | tripId 파라미터 추가, URL 수정, HTTP 메서드 POST→PUT 변경 |
| P0 | [불일치-5] ApiResponse 언래핑 | `schedule/.../PlaceServiceClient.java`, `briefing/.../MonitorServiceClient.java`, `briefing/.../PayServiceClient.java` | `ParameterizedTypeReference<ApiResponse<T>>`로 역직렬화 후 `.getData()` 호출 |
| P0 | [불일치-6] 프론트 SCHD 경로 | `schedule_datasource.dart` | `/schedule` → `/schedule-items` 경로 수정, reorder API 처리 |
| P0 | [불일치-7] 프론트 MNTR 경로 | `monitoring_datasource.dart` | `/monitor/places/{id}/status` → `/badges/{id}/detail` |
| P0 | [불일치-8] 프론트 ALTN 경로 | `alternative_datasource.dart` | GET→POST, `/alternatives` → `/alternatives/search`, `/apply` → `/select` |
| P0 | [불일치-9] 토큰 갱신 경로 | `auth_interceptor.dart` | `/auth/refresh` → `/auth/token/refresh` |

### 중간 우선순위 수정 (기능 불완전)

| 우선순위 | 항목 | 수정 대상 파일 | 수정 내용 |
|----------|------|----------------|-----------|
| P1 | [불일치-3] BRIF→PAY userId 전달 | `payment/.../PayController.java` | 서비스 간 내부 호출용 `@RequestParam userId` 파라미터 추가 또는 내부 전용 엔드포인트 추가 |
| P1 | [불일치-3] todayBriefingCount 누락 | `payment/.../SubscriptionStatusResponse.java` | `todayBriefingCount` 필드 추가 또는 BRIF SubscriptionInfo에서 필드 제거 |
| P1 | [불일치-10] /auth/me 미구현 | `auth/.../AuthController.java` | `GET /api/v1/auth/me` 또는 `GET /api/v1/users/me` 엔드포인트 추가 |
| P1 | [불일치-11] 브리핑 쿼리 파라미터 | `briefing/.../BriefController.java` 또는 `briefing_datasource.dart` | 백엔드에 `@RequestParam trip_id` 추가하거나, 프론트엔드 파라미터를 `date`로 변경 |

---

## 5. 검증 결과 매트릭스

### 서비스 간 REST 클라이언트 호출 경로

| 의존 관계 | 경로 일치 | DTO 호환 | ApiResponse 언래핑 | 종합 |
|-----------|----------|----------|-------------------|------|
| SCHD → PLCE | 불일치 (`/api/v1/places/` vs `/places/`) | 불일치 (lat/lng flat vs nested coordinates) | 미처리 | **FAIL** |
| ALTN → PLCE | 불일치 (`/api/v1/places/nearby` vs `/places/nearby`) | 해당 없음 (PlaceCandidate 배열) | 미처리 | **FAIL** |
| ALTN → MNTR | Phase 1 Mock (HTTP 호출 없음) | N/A | N/A | 해당 없음 |
| ALTN → SCHD | 불일치 (`/api/v1/schedule-items/{id}/replace` vs `/api/v1/trips/{tripId}/schedule-items/{id}/replace`) | 미확인 | 미처리 | **FAIL** |
| BRIF → MNTR | 불일치 (`/api/v1/monitor/places/{id}/status` vs `/api/v1/badges/{id}/detail`) | 불일치 (flat vs nested) | 미처리 | **FAIL** |
| BRIF → PAY | 경로 일치, userId 파라미터 무시 | 불일치 (`todayBriefingCount` 누락) | 미처리 | **FAIL** |
| PAY → AUTH | 일치 | 일치 | 미처리 | **PARTIAL** |

### 프론트엔드 API 매핑

| 피처 | 경로 일치 | 비고 |
|------|----------|------|
| auth (social-login, logout, token/refresh) | 일치 | Prism Mock 기준 |
| auth (token 자동 갱신 인터셉터) | **불일치** | `/auth/refresh` vs `/auth/token/refresh` |
| auth (GET /auth/me) | **불일치** | 백엔드 미존재 |
| schedule (GET trips, GET/POST /trips/{id}) | 일치 | |
| schedule (POST schedule-items) | **불일치** | `/schedule` vs `/schedule-items` |
| schedule (DELETE schedule-item) | **불일치** | `/schedule/{id}` vs `/schedule-items/{id}` |
| schedule (PUT reorder) | **불일치** | 백엔드 미구현 |
| place (search, detail) | 일치 | |
| monitoring (getPlaceStatus) | **불일치** | `/monitor/places/{id}/status` vs `/badges/{id}/detail` |
| monitoring (getTripStatus) | **불일치** | 백엔드 미구현 |
| payment (plans, status, purchase) | 일치 | |
| briefing (getBriefings, getBriefing) | 경로 일치, 파라미터 불일치 | trip_id vs date |
| alternative (getAlternatives) | **불일치** | GET vs POST, 경로 다름 |
| alternative (applyAlternative) | **불일치** | `/apply` vs `/select` |

---

## 6. 결론

**전체 판정: FAIL**

백엔드 서비스 간 7개 의존 관계 중 1개만 완전 일치(PAY→AUTH 경로), 나머지 6개는 경로 불일치 또는 DTO 불호환 문제를 가지고 있다. 프론트엔드 14개 API 호출 중 7개가 경로 불일치 또는 미구현 엔드포인트를 호출하고 있다.

**핵심 구조적 문제 2가지:**

1. **PLCE 컨트롤러 경로 접두사 누락**: 모든 다른 서비스는 `@RequestMapping("/api/v1")` 패턴을 사용하나 PLCE만 `@RequestMapping("/places")`를 사용하여 모든 서비스 간 호출이 실패함.

2. **ApiResponse 래퍼 미처리**: 모든 서비스 간 클라이언트가 `restTemplate.getForEntity(url, TargetDto.class)`를 사용하나, 실제 응답은 `{"success":true,"data":{...}}`로 래핑되어 있어 역직렬화 시 모든 필드가 null이 됨. Phase 1에서 Mock으로 가려지지만 실제 서비스 연결 시 전면 장애 발생.

---

*검증 완료: 2026-02-24*
