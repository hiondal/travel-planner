# 프론트엔드 API 매핑 설계서

## 문서 정보

| 항목 | 내용 |
|------|------|
| 프로젝트 | travel-planner (여행 중 실시간 일정 최적화 가이드 앱) |
| 클라이언트 | Flutter 3.x |
| HTTP 클라이언트 | Dio |
| API 버전 | v1 |
| 작성일 | 2026-02-23 |
| 작성자 | 데브-프론트 |

---

## 4.1 API 경로 매핑 (AppConfig 설계)

### 4.1.1 환경별 호스트 설정

Flutter 앱은 빌드 플레이버(flavor)별로 AppConfig를 분리하여 환경별 API 호스트를 주입한다.

```
API_GROUP: "/api/v1"

[개발 환경 - dev]
AUTH_HOST:        "http://localhost:8081"   // Auth Service
SCHEDULE_HOST:    "http://localhost:8082"   // Schedule Service
PLACE_HOST:       "http://localhost:8083"   // Place Service
MONITOR_HOST:     "http://localhost:8084"   // Monitor Service
BRIEFING_HOST:    "http://localhost:8085"   // Briefing Service
ALTERNATIVE_HOST: "http://localhost:8086"   // Alternative Service
PAYMENT_HOST:     "http://localhost:8087"   // Payment Service

[스테이징 환경 - staging]
AUTH_HOST:        "https://dev-api.travel-planner.com/v1"
SCHEDULE_HOST:    "https://dev-api.travel-planner.com/v1"
PLACE_HOST:       "https://dev-api.travel-planner.com/v1"
MONITOR_HOST:     "https://dev-api.travel-planner.com/v1"
BRIEFING_HOST:    "https://dev-api.travel-planner.com/v1"
ALTERNATIVE_HOST: "https://dev-api.travel-planner.com/v1"
PAYMENT_HOST:     "https://dev-api.travel-planner.com/v1"

[운영 환경 - prod]
AUTH_HOST:        "https://api.travel-planner.com/v1"
SCHEDULE_HOST:    "https://api.travel-planner.com/v1"
PLACE_HOST:       "https://api.travel-planner.com/v1"
MONITOR_HOST:     "https://api.travel-planner.com/v1"
BRIEFING_HOST:    "https://api.travel-planner.com/v1"
ALTERNATIVE_HOST: "https://api.travel-planner.com/v1"
PAYMENT_HOST:     "https://api.travel-planner.com/v1"
```

### 4.1.2 MVP 모놀리스 배포 시 단일 호스트

MVP 단계에서는 7개 서비스가 단일 프로세스로 배포되므로 모든 HOST를 동일하게 설정한다.

```
[MVP 로컬 개발]
모든 *_HOST = "http://localhost:8080"

[MVP 운영]
모든 *_HOST = "https://api.travel-planner.com/v1"
```

### 4.1.3 최종 API 엔드포인트 조합 규칙

```
최종 URL = {*_HOST} + {경로}

예시:
  소셜 로그인: http://localhost:8080/auth/social-login
  일정 조회:   http://localhost:8080/trips/{trip_id}/schedule
  장소 검색:   http://localhost:8080/places/search?keyword=시부야+라멘&city=도쿄
```

> 주의: API_GROUP ("/api/v1") 접두어는 서버 측 라우팅 설정에 따라 HOST에 포함되거나
> 클라이언트에서 경로 앞에 명시적으로 붙이는 방식 중 하나를 팀 합의로 결정한다.
> 본 설계서는 HOST에 버전 경로가 포함된 형태(`https://api.travel-planner.com/v1`)를 기준으로 한다.

### 4.1.4 공통 요청 헤더

| 헤더 | 값 | 적용 범위 |
|------|----|-----------|
| `Authorization` | `Bearer {access_token}` | 인증 필요 모든 API |
| `Content-Type` | `application/json` | POST / PUT 요청 |
| `Accept` | `application/json` | 모든 요청 |

---

## 4.2 API와 화면 상세기능 매칭

### 범례

- **인증**: 미표시=불필요, JWT=Bearer 토큰 필요
- **티어**: FREE=무료, TRIP_PASS=트립패스, PRO=프로
- **비고**: 특이사항 및 처리 주의점

---

### 4.2.1 소셜 로그인 화면

#### API-01. 소셜 로그인

| 항목 | 내용 |
|------|------|
| 화면 | 소셜 로그인 화면 |
| 기능 | Google / Apple OAuth 인증 코드를 서버에 전달하고 JWT 발급 |
| 백엔드 서비스 | Auth Service |
| API 경로 | `POST /auth/social-login` |
| 인증 | 불필요 |
| 티어 제한 | 없음 |

**요청 JSON**
```json
{
  "provider": "google",
  "oauth_code": "4/0AX4XfWh..."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `provider` | string | Y | OAuth 제공자 (`google` \| `apple`) |
| `oauth_code` | string | Y | 플랫폼 SDK로 획득한 OAuth 인증 코드 |

**응답 JSON (200 성공)**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
  "user_profile": {
    "user_id": "usr_01HX...",
    "nickname": "도쿄여행자",
    "avatar_url": "https://lh3.googleusercontent.com/...",
    "tier": "FREE",
    "is_new_user": false
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `access_token` | string | JWT Access Token (만료 30분) |
| `refresh_token` | string | Refresh Token (로컬 Secure Storage에 저장) |
| `user_profile.user_id` | string | 사용자 고유 ID |
| `user_profile.tier` | string | 구독 티어 (`FREE` \| `TRIP_PASS` \| `PRO`) |
| `user_profile.is_new_user` | boolean | 최초 로그인 여부 (온보딩 화면 분기에 사용) |

**에러 응답**

| HTTP 코드 | error 코드 | 처리 방법 |
|-----------|-----------|----------|
| 401 | `UNAUTHORIZED` | "로그인에 실패했습니다" 토스트 표시 |
| 503 | `SERVICE_UNAVAILABLE` | "인터넷 연결을 확인해주세요" 토스트 표시 |

---

#### API-02. Access Token 갱신

| 항목 | 내용 |
|------|------|
| 화면 | 앱 전역 (Dio Interceptor에서 자동 처리) |
| 기능 | Access Token 만료 시 Refresh Token으로 재발급 |
| 백엔드 서비스 | Auth Service |
| API 경로 | `POST /auth/token/refresh` |
| 인증 | 불필요 |
| 티어 제한 | 없음 |

**요청 JSON**
```json
{
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

**응답 JSON (200 성공)**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 1800
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `access_token` | string | 새 JWT Access Token |
| `expires_in` | integer | 만료까지 남은 초 (1800 = 30분) |

**에러 응답**

| HTTP 코드 | error 코드 | 처리 방법 |
|-----------|-----------|----------|
| 401 | `UNAUTHORIZED` | Refresh Token 만료 → 로그인 화면으로 강제 이동 |

---

#### API-03. 로그아웃

| 항목 | 내용 |
|------|------|
| 화면 | 설정 화면 |
| 기능 | 서버 Refresh Token 무효화 및 로컬 토큰 삭제 |
| 백엔드 서비스 | Auth Service |
| API 경로 | `POST /auth/logout` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**요청 JSON**
```json
{
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

**응답 (204 No Content)**

에러 여부와 관계없이 로컬 토큰을 삭제하고 로그인 화면으로 이동한다.

---

### 4.2.2 온보딩 화면

#### API-04. 사용자 동의 이력 저장

| 항목 | 내용 |
|------|------|
| 화면 | 온보딩 화면 (위치정보 / Push 알림 동의) |
| 기능 | 플랫폼 권한 요청 결과를 서버에 저장 |
| 백엔드 서비스 | Auth Service |
| API 경로 | `POST /users/consent` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**요청 JSON**
```json
{
  "location": true,
  "push": true,
  "timestamp": "2026-03-15T09:00:00Z"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `location` | boolean | Y | 위치정보 수집 동의 여부 |
| `push` | boolean | Y | Push 알림 권한 동의 여부 |
| `timestamp` | string | Y | 동의 일시 (ISO 8601, 클라이언트 현재 시각) |

**응답 JSON (201 성공)**
```json
{
  "consent_id": "cns_01HX...",
  "location": true,
  "push": true,
  "consented_at": "2026-03-15T09:00:00Z"
}
```

**에러 응답**

| HTTP 코드 | error 코드 | 처리 방법 |
|-----------|-----------|----------|
| 400 | `BAD_REQUEST` | 필수 파라미터 누락 확인 후 재전송 |
| 401 | `UNAUTHORIZED` | 로그인 화면으로 이동 |

---

### 4.2.3 여행 목록 화면

#### API-05. 여행 목록 조회

> 주의: schedule-service-api.yaml에는 목록 조회(`GET /trips`) API가 정의되어 있지 않다.
> `GET /trips/{trip_id}` (단건 조회)만 존재한다.
> 목록 조회 API는 백엔드 팀과 추가 협의가 필요하다. 아래는 설계 가이드 기준 명세이다.

| 항목 | 내용 |
|------|------|
| 화면 | 여행 목록 화면 |
| 기능 | 로그인 사용자의 여행 목록 조회 |
| 백엔드 서비스 | Schedule Service |
| API 경로 | `GET /trips` (추가 협의 필요) |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**요청 Query Parameter**
```
없음 (JWT에서 user_id 추출)
```

**응답 JSON (200 성공, 설계 가이드 기준)**
```json
{
  "trips": [
    {
      "trip_id": "trip_01HX...",
      "name": "도쿄 3박4일",
      "start_date": "2026-03-15",
      "end_date": "2026-03-18",
      "city": "도쿄",
      "status": "ACTIVE"
    }
  ]
}
```

---

#### API-06. 여행 단건 조회

| 항목 | 내용 |
|------|------|
| 화면 | 여행 목록 화면 (항목 탭 시) / 일정표 화면 진입 시 |
| 기능 | 특정 여행의 기본 정보 조회 |
| 백엔드 서비스 | Schedule Service |
| API 경로 | `GET /trips/{trip_id}` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**Path Parameter**

| 파라미터 | 설명 |
|---------|------|
| `trip_id` | 여행 일정 ID (예: `trip_01HX...`) |

**응답 JSON (200 성공)**
```json
{
  "trip_id": "trip_01HX...",
  "name": "도쿄 3박4일",
  "start_date": "2026-03-15",
  "end_date": "2026-03-18",
  "city": "도쿄",
  "status": "ACTIVE",
  "schedule_items": []
}
```

**에러 응답**

| HTTP 코드 | error 코드 | 처리 방법 |
|-----------|-----------|----------|
| 404 | `NOT_FOUND` | "여행 정보를 찾을 수 없습니다" 안내 후 목록으로 이동 |

---

### 4.2.4 여행 생성 화면

#### API-07. 여행 일정 생성

| 항목 | 내용 |
|------|------|
| 화면 | 여행 생성 화면 |
| 기능 | 여행명 / 기간 / 도시 입력 후 여행 생성 |
| 백엔드 서비스 | Schedule Service |
| API 경로 | `POST /trips` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**요청 JSON**
```json
{
  "name": "도쿄 3박4일",
  "start_date": "2026-03-15",
  "end_date": "2026-03-18",
  "city": "도쿄"
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `name` | string | Y | 1~50자 | 여행명 |
| `start_date` | string | Y | YYYY-MM-DD | 여행 시작일 |
| `end_date` | string | Y | YYYY-MM-DD, 시작일 이후 | 여행 종료일 |
| `city` | string | Y | 지원 도시 목록에서 선택 | 방문 도시 |

**응답 JSON (201 성공)**
```json
{
  "trip_id": "trip_01HX...",
  "name": "도쿄 3박4일",
  "start_date": "2026-03-15",
  "end_date": "2026-03-18",
  "city": "도쿄",
  "status": "ACTIVE",
  "schedule_items": []
}
```

**에러 응답**

| HTTP 코드 | error 코드 | 처리 방법 |
|-----------|-----------|----------|
| 400 | `BAD_REQUEST` | 입력 값 유효성 오류 안내 (여행명 길이 등) |
| 428 | `CONSENT_REQUIRED` | 위치정보 동의 화면으로 이동 후 재시도 |

**428 응답 JSON (위치정보 동의 필요)**
```json
{
  "error": "CONSENT_REQUIRED",
  "message": "위치정보 수집 동의가 필요합니다.",
  "consent_required": true
}
```

---

### 4.2.5 일정표 화면

#### API-08. 일정표 조회 (장소 목록)

| 항목 | 내용 |
|------|------|
| 화면 | 일정표 화면 |
| 기능 | 여행 일정의 장소 목록을 시간순으로 조회 |
| 백엔드 서비스 | Schedule Service |
| API 경로 | `GET /trips/{trip_id}/schedule` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**Path Parameter**

| 파라미터 | 설명 |
|---------|------|
| `trip_id` | 여행 일정 ID |

**응답 JSON (200 성공)**
```json
{
  "trip_id": "trip_01HX...",
  "name": "도쿄 3박4일",
  "city": "도쿄",
  "schedule_items": [
    {
      "schedule_item_id": "si_01HX...",
      "place_id": "place_abc123",
      "place_name": "이치란 라멘 시부야",
      "visit_datetime": "2026-03-16T12:00:00+09:00",
      "timezone": "Asia/Tokyo",
      "order": 1,
      "outside_business_hours": false
    },
    {
      "schedule_item_id": "si_02HX...",
      "place_id": "place_def456",
      "place_name": "시부야 스크램블 교차로",
      "visit_datetime": "2026-03-16T15:00:00+09:00",
      "timezone": "Asia/Tokyo",
      "order": 2,
      "outside_business_hours": false
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `schedule_items[].schedule_item_id` | string | 일정 항목 ID |
| `schedule_items[].place_id` | string | 장소 ID (상태 배지 조회에 사용) |
| `schedule_items[].visit_datetime` | string | 방문 일시 (IANA 타임존 기준 ISO 8601) |
| `schedule_items[].order` | integer | 방문 순서 (화면 정렬에 사용) |
| `schedule_items[].outside_business_hours` | boolean | 영업시간 외 등록 여부 (경고 아이콘 표시에 사용) |

---

#### API-09. 상태 배지 일괄 조회

| 항목 | 내용 |
|------|------|
| 화면 | 일정표 화면 (각 장소 카드의 상태 배지) |
| 기능 | 일정표 장소 목록의 place_id를 수집하여 상태 배지 일괄 조회 |
| 백엔드 서비스 | Monitor Service |
| API 경로 | `GET /badges` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**Query Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `place_ids` | string | Y | 조회할 장소 ID 목록 (콤마 구분, 최대 한 번에 조회 가능한 수 협의 필요) |

**요청 예시**
```
GET /badges?place_ids=place_abc123,place_def456,place_xyz789
```

**응답 JSON (200 성공)**
```json
{
  "badges": [
    {
      "place_id": "place_abc123",
      "status": "GREEN",
      "icon": "CHECK",
      "label": null,
      "color_hex": "#4CAF50",
      "updated_at": "2026-03-16T11:45:00+09:00"
    },
    {
      "place_id": "place_def456",
      "status": "YELLOW",
      "icon": "EXCLAMATION",
      "label": null,
      "color_hex": "#FFC107",
      "updated_at": "2026-03-16T11:45:00+09:00"
    },
    {
      "place_id": "place_xyz789",
      "status": "GREY",
      "icon": "QUESTION",
      "label": "데이터 미확인",
      "color_hex": "#9E9E9E",
      "updated_at": "2026-03-16T10:30:00+09:00"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `status` | string | 상태값: `GREEN`=정상, `YELLOW`=주의, `RED`=위험, `GREY`=미확인 |
| `icon` | string | 아이콘: `CHECK`, `EXCLAMATION`, `X`, `QUESTION` |
| `color_hex` | string | 배지 색상 (색약 사용자 지원을 위해 아이콘과 병행 사용) |

> 호출 타이밍: 일정표 조회(API-08) 완료 후 place_id 목록을 수집하여 병렬 호출한다.
> 캐시 TTL이 10분이므로 화면 진입 시마다 호출하여 최신 상태를 표시한다.

---

### 4.2.6 장소 검색 화면

#### API-10. 키워드 기반 장소 검색

| 항목 | 내용 |
|------|------|
| 화면 | 장소 검색 화면 |
| 기능 | 키워드와 도시로 장소 검색 (최대 10개 결과) |
| 백엔드 서비스 | Place Service |
| API 경로 | `GET /places/search` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**Query Parameter**

| 파라미터 | 타입 | 필수 | 제약 | 설명 |
|---------|------|------|------|------|
| `keyword` | string | Y | 최소 2자 | 검색 키워드 |
| `city` | string | Y | - | 검색 도시 (지원 도시 목록) |

**요청 예시**
```
GET /places/search?keyword=시부야+라멘&city=도쿄
```

**응답 JSON (200 성공)**
```json
{
  "places": [
    {
      "place_id": "place_abc123",
      "name": "이치란 라멘 시부야",
      "address": "도쿄 시부야구 도겐자카 1-22-7",
      "rating": 4.2,
      "business_hours": [
        { "day": "월요일", "open": "11:00", "close": "22:00" }
      ],
      "coordinates": {
        "lat": 35.6595,
        "lng": 139.7004
      }
    },
    {
      "place_id": "place_def456",
      "name": "후쿠로쿠 라멘",
      "address": "도쿄 시부야구 우다가와초 13-11",
      "rating": 4.0,
      "business_hours": [
        { "day": "월요일", "open": "11:30", "close": "21:00" }
      ],
      "coordinates": {
        "lat": 35.6621,
        "lng": 139.6982
      }
    }
  ]
}
```

**에러 응답**

| HTTP 코드 | error 코드 | 처리 방법 |
|-----------|-----------|----------|
| 400 | `BAD_REQUEST` | 검색어 2자 미만 → 입력 안내 |
| 503 | `SERVICE_UNAVAILABLE` | "장소 검색을 일시적으로 사용할 수 없습니다" 표시 |

---

### 4.2.7 장소 상세 바텀시트

#### API-11. 장소 상세 정보 조회

| 항목 | 내용 |
|------|------|
| 화면 | 장소 상세 바텀시트 |
| 기능 | 장소 검색 결과 탭 시 상세 정보(영업시간, 위치, 사진 등) 조회 |
| 백엔드 서비스 | Place Service |
| API 경로 | `GET /places/{place_id}` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**Path Parameter**

| 파라미터 | 설명 |
|---------|------|
| `place_id` | 장소 ID |

**응답 JSON (200 성공)**
```json
{
  "place_id": "place_abc123",
  "name": "이치란 라멘 시부야",
  "address": "도쿄 시부야구 도겐자카 1-22-7",
  "category": "음식점",
  "rating": 4.2,
  "business_hours": [
    { "day": "월요일", "open": "11:00", "close": "22:00" },
    { "day": "화요일", "open": "11:00", "close": "22:00" }
  ],
  "coordinates": {
    "lat": 35.6595,
    "lng": 139.7004
  },
  "timezone": "Asia/Tokyo",
  "photo_url": "https://maps.googleapis.com/maps/api/place/photo..."
}
```

**에러 응답**

| HTTP 코드 | error 코드 | 처리 방법 |
|-----------|-----------|----------|
| 404 | `NOT_FOUND` | "장소 정보를 불러올 수 없습니다" 안내 |

---

#### API-12. 반경 기반 주변 장소 검색

| 항목 | 내용 |
|------|------|
| 화면 | 장소 상세 바텀시트 (지도 주변 장소 표시) |
| 기능 | 특정 좌표 기준 반경 내 카테고리별 영업 중 장소 검색 |
| 백엔드 서비스 | Place Service |
| API 경로 | `GET /places/nearby` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**Query Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `lat` | number | Y | 기준 위도 |
| `lng` | number | Y | 기준 경도 |
| `category` | string | Y | 장소 카테고리 (예: `라멘`) |
| `radius` | integer | Y | 검색 반경 미터 (`1000` \| `2000` \| `3000`) |

**요청 예시**
```
GET /places/nearby?lat=35.6595&lng=139.7004&category=라멘&radius=1000
```

**응답 JSON (200 성공)**
```json
{
  "places": [
    {
      "place_id": "place_xyz789",
      "name": "후쿠로쿠 라멘",
      "address": "도쿄 시부야구 우다가와초 13-11",
      "distance_m": 320,
      "rating": 4.0,
      "category": "라멘",
      "coordinates": {
        "lat": 35.6621,
        "lng": 139.6982
      },
      "is_open": true
    }
  ],
  "radius_used": 1000
}
```

---

### 4.2.8 장소 추가

#### API-13. 일정에 장소 추가

| 항목 | 내용 |
|------|------|
| 화면 | 장소 상세 바텀시트 → "일정 추가" 버튼 탭 |
| 기능 | 선택한 장소와 방문 일시를 일정표에 추가 |
| 백엔드 서비스 | Schedule Service |
| API 경로 | `POST /trips/{trip_id}/schedule-items` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**Path Parameter**

| 파라미터 | 설명 |
|---------|------|
| `trip_id` | 여행 일정 ID |

**요청 JSON (영업시간 내 추가)**
```json
{
  "place_id": "place_abc123",
  "visit_datetime": "2026-03-16T12:00:00+09:00",
  "timezone": "Asia/Tokyo",
  "force": false
}
```

**요청 JSON (영업시간 외 강제 추가)**
```json
{
  "place_id": "place_abc123",
  "visit_datetime": "2026-03-16T09:00:00+09:00",
  "timezone": "Asia/Tokyo",
  "force": true
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `place_id` | string | Y | 추가할 장소 ID |
| `visit_datetime` | string | Y | 방문 일시 (ISO 8601, 현지 시간 기준) |
| `timezone` | string | Y | IANA 타임존 (예: `Asia/Tokyo`) |
| `force` | boolean | N | 영업시간 외 강제 추가 여부 (기본값 `false`) |

**응답 JSON (201 성공)**
```json
{
  "schedule_item_id": "si_01HX...",
  "place_id": "place_abc123",
  "place_name": "이치란 라멘 시부야",
  "visit_datetime": "2026-03-16T12:00:00+09:00",
  "timezone": "Asia/Tokyo",
  "order": 1,
  "outside_business_hours": false
}
```

**응답 JSON (200 영업시간 외 경고, force=false 시)**
```json
{
  "warning": "OUTSIDE_BUSINESS_HOURS",
  "message": "영업시간 외입니다 (영업시간: 11:00~22:00)",
  "business_hours": "11:00~22:00"
}
```

> 클라이언트 처리: 200 응답 수신 시 "영업시간 외입니다. 그래도 추가하시겠습니까?"
> 확인 다이얼로그를 표시하고 확인 시 `force=true`로 재요청한다.

**에러 응답**

| HTTP 코드 | error 코드 | 처리 방법 |
|-----------|-----------|----------|
| 400 | `BAD_REQUEST` | 입력 값 오류 안내 |
| 404 | `NOT_FOUND` | 여행 또는 장소 없음 안내 |

---

#### API-14. 일정 장소 삭제

| 항목 | 내용 |
|------|------|
| 화면 | 일정표 화면 (장소 카드 스와이프 삭제) |
| 기능 | 일정표에서 특정 장소 삭제 |
| 백엔드 서비스 | Schedule Service |
| API 경로 | `DELETE /trips/{trip_id}/schedule-items/{item_id}` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**Path Parameter**

| 파라미터 | 설명 |
|---------|------|
| `trip_id` | 여행 일정 ID |
| `item_id` | 일정 장소 항목 ID (schedule_item_id) |

**응답 (204 No Content)**

> 삭제 완료 후 일정표를 로컬에서 해당 항목 제거하고 UI를 갱신한다.

---

### 4.2.9 장소 교체

#### API-15. 일정 장소 교체 (대안 선택 후)

| 항목 | 내용 |
|------|------|
| 화면 | 대안 카드 화면 → "이걸로 변경" 버튼 탭 |
| 기능 | 기존 장소를 대안 장소로 교체하고 후속 이동시간 재계산 |
| 백엔드 서비스 | Schedule Service |
| API 경로 | `PUT /trips/{trip_id}/schedule-items/{item_id}/replace` |
| 인증 | JWT 필요 |
| 티어 제한 | TRIP_PASS 이상 (대안 선택 후 호출되므로 간접 제한) |

**Path Parameter**

| 파라미터 | 설명 |
|---------|------|
| `trip_id` | 여행 일정 ID |
| `item_id` | 교체 대상 일정 항목 ID |

**요청 JSON**
```json
{
  "new_place_id": "place_xyz789"
}
```

**응답 JSON (200 성공)**
```json
{
  "schedule_item_id": "si_01HX...",
  "original_place": {
    "place_id": "place_abc123",
    "place_name": "이치란 라멘 시부야"
  },
  "new_place": {
    "place_id": "place_xyz789",
    "place_name": "후쿠로쿠 라멘"
  },
  "travel_time_diff_minutes": -5,
  "updated_schedule_items": [
    {
      "schedule_item_id": "si_01HX...",
      "place_id": "place_xyz789",
      "place_name": "후쿠로쿠 라멘",
      "visit_datetime": "2026-03-16T12:00:00+09:00"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `travel_time_diff_minutes` | integer | 이동시간 변화 (음수=단축, 양수=증가) |
| `updated_schedule_items` | array | 이동시간 재계산이 적용된 후속 일정 항목 목록 |

> 응답 수신 후 일정표 전체를 `updated_schedule_items` 기준으로 갱신한다.

---

### 4.2.10 브리핑 목록 화면

#### API-16. 브리핑 목록 조회

| 항목 | 내용 |
|------|------|
| 화면 | 브리핑 목록 화면 |
| 기능 | 오늘 날짜 기준 수신한 브리핑 목록을 최신순으로 조회 |
| 백엔드 서비스 | Briefing Service |
| API 경로 | `GET /briefings` |
| 인증 | JWT 필요 |
| 티어 제한 | FREE: 일 1회, TRIP_PASS 이상: 무제한 |

**Query Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `date` | string | N | 조회 날짜 (기본값: 오늘, YYYY-MM-DD) |

**요청 예시**
```
GET /briefings?date=2026-03-16
```

**응답 JSON (200 성공)**
```json
{
  "date": "2026-03-16",
  "briefings": [
    {
      "briefing_id": "brif_01HX...",
      "type": "SAFE",
      "place_name": "이치란 라멘 시부야",
      "created_at": "2026-03-16T11:40:00+09:00",
      "expired": false
    },
    {
      "briefing_id": "brif_02HX...",
      "type": "WARNING",
      "place_name": "시부야 스크램블 교차로",
      "created_at": "2026-03-16T14:30:00+09:00",
      "expired": false
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `type` | string | 브리핑 유형: `SAFE`=안심(파란 아이콘), `WARNING`=주의(노란 아이콘) |
| `expired` | boolean | 만료 여부 (방문 예정 시간 경과) |

---

### 4.2.11 브리핑 상세 화면

#### API-17. 브리핑 상세 조회

| 항목 | 내용 |
|------|------|
| 화면 | 브리핑 상세 화면 |
| 기능 | 브리핑 항목 탭 시 영업상태 / 혼잡도 / 날씨 / 이동시간 상세 조회 |
| 백엔드 서비스 | Briefing Service |
| API 경로 | `GET /briefings/{briefing_id}` |
| 인증 | JWT 필요 |
| 티어 제한 | FREE: 일 1회 한도 내, TRIP_PASS 이상: 무제한 |

**Path Parameter**

| 파라미터 | 설명 |
|---------|------|
| `briefing_id` | 브리핑 ID |

**응답 JSON (200 성공 - 안심 브리핑)**
```json
{
  "briefing_id": "brif_01HX...",
  "type": "SAFE",
  "place_id": "place_abc123",
  "place_name": "이치란 라멘 시부야",
  "departure_time": "2026-03-16T12:00:00+09:00",
  "created_at": "2026-03-16T11:40:00+09:00",
  "expired": false,
  "expire_message": null,
  "content": {
    "business_status": "영업 중",
    "congestion": "보통",
    "weather": "맑음",
    "travel_time": {
      "walking_minutes": 15,
      "transit_minutes": null,
      "distance_m": 420
    },
    "summary": "현재까지 모든 항목 정상입니다. 예정대로 출발하세요."
  },
  "alternative_link": null
}
```

**응답 JSON (200 성공 - 주의 브리핑, 만료)**
```json
{
  "briefing_id": "brif_02HX...",
  "type": "WARNING",
  "place_id": "place_def456",
  "place_name": "시부야 스크램블 교차로",
  "departure_time": "2026-03-16T15:00:00+09:00",
  "created_at": "2026-03-16T14:30:00+09:00",
  "expired": true,
  "expire_message": "이미 지난 브리핑입니다",
  "content": {
    "business_status": "영업 중",
    "congestion": "혼잡",
    "weather": "맑음",
    "travel_time": {
      "walking_minutes": 20,
      "transit_minutes": 8,
      "distance_m": 620
    },
    "summary": "혼잡도이(가) 감지되었습니다. 대안을 확인해보세요."
  },
  "alternative_link": "/alternatives?place_id=place_def456"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `expired` | boolean | 만료 시 배너 표시 및 최신 상태 조회 버튼 안내 |
| `alternative_link` | string\|null | WARNING 타입 시 대안 검색 진입 링크 (null이면 버튼 미표시) |

**에러 응답**

| HTTP 코드 | error 코드 | 처리 방법 |
|-----------|-----------|----------|
| 403 | `FORBIDDEN` | "권한이 없습니다" 안내 후 목록으로 이동 |
| 404 | `NOT_FOUND` | "브리핑을 찾을 수 없습니다" 안내 |

---

### 4.2.12 대안 카드 화면

#### API-18. 대안 장소 검색 (카드 3장 생성)

| 항목 | 내용 |
|------|------|
| 화면 | 대안 카드 화면 |
| 기능 | 원래 장소와 동일 카테고리, 반경 1km 이내 대안 장소 카드 3장 조회 |
| 백엔드 서비스 | Alternative Service |
| API 경로 | `POST /alternatives/search` |
| 인증 | JWT 필요 |
| 티어 제한 | TRIP_PASS 이상 (FREE 사용자 → 402 Paywall) |

**요청 JSON**
```json
{
  "place_id": "place_abc123",
  "category": "라멘",
  "location": {
    "lat": 35.6595,
    "lng": 139.7004
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `place_id` | string | Y | 원래 장소 ID |
| `category` | string | Y | 원래 장소와 동일 카테고리 |
| `location.lat` | number | Y | 원래 장소 위도 |
| `location.lng` | number | Y | 원래 장소 경도 |

**응답 JSON (200 성공)**
```json
{
  "original_place_id": "place_abc123",
  "cards": [
    {
      "alt_id": "alt_01HX...",
      "place_id": "place_xyz789",
      "name": "후쿠로쿠 라멘",
      "distance_m": 320,
      "rating": 4.0,
      "congestion": "낮음",
      "reason": "근거리 영업 중 동일 카테고리",
      "status_label": null,
      "coordinates": { "lat": 35.6621, "lng": 139.6982 },
      "travel_time": {
        "walking_minutes": 5,
        "transit_minutes": null
      }
    },
    {
      "alt_id": "alt_02HX...",
      "place_id": "place_ghi012",
      "name": "멘야 무사시",
      "distance_m": 650,
      "rating": 4.3,
      "congestion": "보통",
      "reason": "높은 평점",
      "status_label": null,
      "coordinates": { "lat": 35.6571, "lng": 139.7031 },
      "travel_time": {
        "walking_minutes": 9,
        "transit_minutes": 4
      }
    },
    {
      "alt_id": "alt_03HX...",
      "place_id": "place_jkl345",
      "name": "라멘 산쿠로",
      "distance_m": 820,
      "rating": 3.8,
      "congestion": "낮음",
      "reason": "혼잡도 낮음",
      "status_label": "주의 필요",
      "coordinates": { "lat": 35.6610, "lng": 139.7051 },
      "travel_time": {
        "walking_minutes": 12,
        "transit_minutes": 5
      }
    }
  ],
  "radius_used": 1000
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `cards[].alt_id` | string | 대안 카드 식별자 (선택 시 사용) |
| `cards[].congestion` | string | 혼잡도: `낮음` \| `보통` \| `혼잡` |
| `cards[].status_label` | string\|null | `null`=정상, `주의 필요`=노랑, `정보 미확인`=회색 |
| `radius_used` | integer | 실제 사용 반경 (1000/2000/3000) |

**응답 JSON (402 Free 티어 Paywall)**
```json
{
  "paywall": true,
  "message": "대안 카드는 Trip Pass/Pro 전용 기능입니다.",
  "upgrade_url": "/subscriptions/plans"
}
```

> 클라이언트 처리: 402 수신 시 `upgrade_url` 경로를 기준으로 구독 플랜 화면으로 이동한다.

---

#### API-19. 대안 카드 선택 및 일정 반영

| 항목 | 내용 |
|------|------|
| 화면 | 대안 카드 화면 → 카드 탭 |
| 기능 | 선택한 대안 카드를 기존 일정에 반영 (내부적으로 SCHD 서비스 호출) |
| 백엔드 서비스 | Alternative Service |
| API 경로 | `POST /alternatives/{alt_id}/select` |
| 인증 | JWT 필요 |
| 티어 제한 | TRIP_PASS 이상 |

**Path Parameter**

| 파라미터 | 설명 |
|---------|------|
| `alt_id` | 선택한 대안 카드 ID |

**요청 JSON**
```json
{
  "original_place_id": "place_abc123",
  "schedule_item_id": "si_01HX...",
  "trip_id": "trip_01HX...",
  "selected_rank": 1,
  "elapsed_seconds": 12
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `original_place_id` | string | Y | 교체 대상 원래 장소 ID |
| `schedule_item_id` | string | Y | 일정 항목 ID |
| `trip_id` | string | Y | 여행 일정 ID |
| `selected_rank` | integer | Y | 선택한 카드 순위 (1~3) — KPI 측정용 |
| `elapsed_seconds` | integer | Y | 대안 검색 시작 ~ 선택까지 경과 시간 (초) — KPI 측정용 |

**응답 JSON (200 성공)**
```json
{
  "schedule_item_id": "si_01HX...",
  "original_place": {
    "place_id": "place_abc123",
    "name": "이치란 라멘 시부야"
  },
  "new_place": {
    "place_id": "place_xyz789",
    "name": "후쿠로쿠 라멘"
  },
  "travel_time_diff_minutes": -3
}
```

> 응답 수신 후 일정표 화면으로 이동하고, 변경된 장소를 하이라이트하여 사용자에게 안내한다.

---

### 4.2.13 상태 상세 화면

#### API-20. 장소 상태 상세 조회

| 항목 | 내용 |
|------|------|
| 화면 | 상태 상세 화면 (일정표 배지 탭 시) |
| 기능 | 특정 장소의 영업상태 / 혼잡도 / 날씨 / 이동시간 상세 정보 조회 |
| 백엔드 서비스 | Monitor Service |
| API 경로 | `GET /badges/{place_id}/detail` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**Path Parameter**

| 파라미터 | 설명 |
|---------|------|
| `place_id` | 장소 ID |

**응답 JSON (200 성공)**
```json
{
  "place_id": "place_def456",
  "place_name": "시부야 스크램블 교차로",
  "overall_status": "YELLOW",
  "details": {
    "business_status": {
      "status": "NORMAL",
      "value": "영업 중"
    },
    "congestion": {
      "status": "WARNING",
      "value": "혼잡",
      "is_unknown": false
    },
    "weather": {
      "status": "NORMAL",
      "value": "맑음",
      "precipitation_prob": 15
    },
    "travel_time": {
      "status": "NORMAL",
      "walking_minutes": 15,
      "transit_minutes": 8,
      "distance_m": 620
    }
  },
  "reason": "혼잡도 높음",
  "show_alternative_button": true,
  "updated_at": "2026-03-16T11:45:00+09:00"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `details.*.status` | string | `NORMAL`=정상, `WARNING`=주의, `DANGER`=위험 |
| `show_alternative_button` | boolean | `true`이면 "대안 보기" 버튼 표시 |
| `details.weather.precipitation_prob` | integer | 강수 확률 (%) |
| `details.travel_time.transit_minutes` | integer\|null | 500m 이상 시에만 제공 |

---

### 4.2.14 구독 관리 화면

#### API-21. 구독 상태 조회

| 항목 | 내용 |
|------|------|
| 화면 | 설정 화면 > 구독 관리 |
| 기능 | 현재 구독 티어 및 만료일 표시 |
| 백엔드 서비스 | Payment Service |
| API 경로 | `GET /subscriptions/status` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**응답 JSON (200 성공 - FREE 티어)**
```json
{
  "tier": "FREE",
  "status": "ACTIVE",
  "subscription_id": null,
  "started_at": null,
  "expires_at": null
}
```

**응답 JSON (200 성공 - Trip Pass)**
```json
{
  "tier": "TRIP_PASS",
  "status": "ACTIVE",
  "subscription_id": "sub_01HX...",
  "started_at": "2026-03-16T10:00:00Z",
  "expires_at": null
}
```

**응답 JSON (200 성공 - Pro, 해지 예정)**
```json
{
  "tier": "PRO",
  "status": "CANCELLING",
  "subscription_id": "sub_02HX...",
  "started_at": "2026-02-01T00:00:00Z",
  "expires_at": "2026-04-01T00:00:00Z"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `tier` | string | `FREE` \| `TRIP_PASS` \| `PRO` |
| `status` | string | `ACTIVE`=활성, `CANCELLED`=해지, `CANCELLING`=해지 예정 |
| `expires_at` | string\|null | `CANCELLING` 상태 시 서비스 종료 일시 표시 |

---

#### API-22. 구독 플랜 목록 조회

| 항목 | 내용 |
|------|------|
| 화면 | 구독 플랜 선택 화면 (Paywall) |
| 기능 | Trip Pass / Pro 플랜의 가격 및 혜택 목록 조회 |
| 백엔드 서비스 | Payment Service |
| API 경로 | `GET /subscriptions/plans` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 |

**응답 JSON (200 성공)**
```json
{
  "plans": [
    {
      "plan_id": "plan_trip_pass",
      "name": "Trip Pass",
      "tier": "TRIP_PASS",
      "price": {
        "amount": 4900,
        "currency": "KRW",
        "period": "1회"
      },
      "features": [
        "대안 카드 기능 이용",
        "무제한 브리핑"
      ],
      "apple_product_id": "com.travel-planner.trippass",
      "google_product_id": "travel_planner_trippass"
    },
    {
      "plan_id": "plan_pro",
      "name": "Pro",
      "tier": "PRO",
      "price": {
        "amount": 9900,
        "currency": "KRW",
        "period": "월"
      },
      "features": [
        "대안 카드 기능 이용",
        "무제한 브리핑",
        "AI 컨시어지 가이드",
        "우선 지원"
      ],
      "apple_product_id": "com.travel-planner.pro.monthly",
      "google_product_id": "travel_planner_pro_monthly"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `apple_product_id` | string | iOS 인앱 결제 시 StoreKit에 전달할 제품 ID |
| `google_product_id` | string | Android 인앱 결제 시 Play Billing에 전달할 제품 ID |

---

### 4.2.15 결제 화면

#### API-23. 구독 구매 (인앱 결제 영수증 검증)

| 항목 | 내용 |
|------|------|
| 화면 | 구독 플랜 선택 화면 → 플랜 탭 후 인앱 결제 완료 시 |
| 기능 | Apple IAP / Google Play 인앱 결제 완료 후 영수증을 서버에 전달하여 검증 및 티어 즉시 활성화 |
| 백엔드 서비스 | Payment Service |
| API 경로 | `POST /subscriptions/purchase` |
| 인증 | JWT 필요 |
| 티어 제한 | 없음 (FREE → TRIP_PASS/PRO 업그레이드 목적) |

**요청 JSON (Apple IAP)**
```json
{
  "plan_id": "plan_trip_pass",
  "receipt": "MIIT3QYJKoZIhvcNAQcCoIIT...",
  "provider": "apple"
}
```

**요청 JSON (Google Play)**
```json
{
  "plan_id": "plan_pro",
  "receipt": "eyJwdXJjaGFzZVRva2VuIjoiQUV1aHA0Li4uIn0=",
  "provider": "google"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `plan_id` | string | Y | 구독 플랜 ID |
| `receipt` | string | Y | 인앱 결제 영수증 데이터 |
| `provider` | string | Y | 결제 제공자 (`apple` \| `google`) |

**응답 JSON (201 성공)**
```json
{
  "subscription_id": "sub_01HX...",
  "tier": "TRIP_PASS",
  "status": "ACTIVE",
  "started_at": "2026-03-16T10:00:00Z",
  "expires_at": null,
  "new_access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "activated_features": [
    "대안 카드 기능 이용",
    "무제한 브리핑"
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `new_access_token` | string | 새 구독 티어가 반영된 JWT — 기존 토큰 대체 저장 필수 |
| `activated_features` | array | 활성화된 기능 목록 (결제 완료 화면에 표시) |

> 클라이언트 처리: `new_access_token`을 수신하면 로컬 Secure Storage의 Access Token을
> 즉시 교체한다. 이후 요청부터 새 토큰으로 인증하여 TRIP_PASS/PRO 기능이 즉시 활성화된다.

**에러 응답**

| HTTP 코드 | error 코드 | 처리 방법 |
|-----------|-----------|----------|
| 402 | `PAYMENT_VERIFICATION_FAILED` | "결제에 실패했습니다. 다시 시도해주세요" 표시 |
| 503 | `SERVICE_UNAVAILABLE` | "결제 서비스가 일시적으로 이용 불가합니다" + 재시도 버튼 |

---

## 4.3 화면별 API 호출 요약표

| # | 화면 | 기능 | 서비스 | 경로 | Method | 인증 | 티어 |
|---|------|------|--------|------|--------|------|------|
| 01 | 소셜 로그인 | 소셜 로그인 | Auth | `/auth/social-login` | POST | - | ALL |
| 02 | 앱 전역 | Access Token 갱신 | Auth | `/auth/token/refresh` | POST | - | ALL |
| 03 | 설정 | 로그아웃 | Auth | `/auth/logout` | POST | JWT | ALL |
| 04 | 온보딩 | 동의 이력 저장 | Auth | `/users/consent` | POST | JWT | ALL |
| 05 | 여행 목록 | 여행 목록 조회 | Schedule | `/trips` | GET | JWT | ALL |
| 06 | 여행 목록 | 여행 단건 조회 | Schedule | `/trips/{trip_id}` | GET | JWT | ALL |
| 07 | 여행 생성 | 여행 일정 생성 | Schedule | `/trips` | POST | JWT | ALL |
| 08 | 일정표 | 일정표 조회 | Schedule | `/trips/{trip_id}/schedule` | GET | JWT | ALL |
| 09 | 일정표 | 상태 배지 조회 | Monitor | `/badges` | GET | JWT | ALL |
| 10 | 장소 검색 | 키워드 장소 검색 | Place | `/places/search` | GET | JWT | ALL |
| 11 | 장소 상세 바텀시트 | 장소 상세 조회 | Place | `/places/{place_id}` | GET | JWT | ALL |
| 12 | 장소 상세 바텀시트 | 주변 장소 검색 | Place | `/places/nearby` | GET | JWT | ALL |
| 13 | 장소 추가 | 일정에 장소 추가 | Schedule | `/trips/{trip_id}/schedule-items` | POST | JWT | ALL |
| 14 | 일정표 | 일정 장소 삭제 | Schedule | `/trips/{trip_id}/schedule-items/{item_id}` | DELETE | JWT | ALL |
| 15 | 대안 카드 | 장소 교체 | Schedule | `/trips/{trip_id}/schedule-items/{item_id}/replace` | PUT | JWT | TRIP_PASS+ |
| 16 | 브리핑 목록 | 브리핑 목록 조회 | Briefing | `/briefings` | GET | JWT | ALL* |
| 17 | 브리핑 상세 | 브리핑 상세 조회 | Briefing | `/briefings/{briefing_id}` | GET | JWT | ALL* |
| 18 | 대안 카드 | 대안 장소 검색 | Alternative | `/alternatives/search` | POST | JWT | TRIP_PASS+ |
| 19 | 대안 카드 | 대안 선택 반영 | Alternative | `/alternatives/{alt_id}/select` | POST | JWT | TRIP_PASS+ |
| 20 | 상태 상세 | 장소 상태 상세 | Monitor | `/badges/{place_id}/detail` | GET | JWT | ALL |
| 21 | 구독 관리 | 구독 상태 조회 | Payment | `/subscriptions/status` | GET | JWT | ALL |
| 22 | 결제(Paywall) | 구독 플랜 조회 | Payment | `/subscriptions/plans` | GET | JWT | ALL |
| 23 | 결제 | 구독 구매 | Payment | `/subscriptions/purchase` | POST | JWT | ALL |

> *ALL*: FREE 사용자는 일 1회 한도, TRIP_PASS 이상은 무제한

---

## 4.4 에러 공통 처리 정책

### 4.4.1 HTTP 상태 코드별 글로벌 처리

| HTTP 코드 | 처리 방법 |
|-----------|----------|
| 400 | 화면별 입력 오류 메시지 표시 |
| 401 | Access Token 갱신 시도 → 실패 시 로그인 화면으로 이동 |
| 402 | Paywall 화면으로 이동 (upgrade_url 활용) |
| 403 | "권한이 없습니다" 토스트 표시 |
| 404 | "정보를 찾을 수 없습니다" 토스트 표시 |
| 428 | 위치정보 동의 화면으로 이동 |
| 500 | "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요" 토스트 |
| 503 | "서비스가 일시적으로 이용 불가합니다" 토스트 + 재시도 버튼 |

### 4.4.2 공통 에러 응답 스키마

```json
{
  "error": "ERROR_CODE",
  "message": "사용자에게 표시할 메시지",
  "timestamp": "2026-03-16T11:45:00Z"
}
```

### 4.4.3 토큰 갱신 인터셉터 동작 흐름

```
API 요청 전송
  → 401 응답 수신
    → Refresh Token으로 POST /auth/token/refresh 호출
      → 성공: 새 Access Token 저장 후 원래 요청 재전송
      → 실패(401): 로컬 토큰 전체 삭제 → 로그인 화면으로 이동
```

---

## 4.5 API 연동 흐름 다이어그램

### 4.5.1 일정표 화면 진입 시 API 호출 순서

```
1. GET /trips/{trip_id}/schedule          (Schedule Service)
   └─ 응답에서 place_id 목록 추출
2. GET /badges?place_ids={...}            (Monitor Service, 병렬)
   └─ 각 장소 배지 색상/아이콘 적용
```

### 4.5.2 장소 추가 흐름

```
1. GET /places/search?keyword={}&city={}  (Place Service)
   └─ 검색 결과 목록 표시
2. GET /places/{place_id}                 (Place Service)
   └─ 바텀시트에 상세 정보 표시
3. POST /trips/{trip_id}/schedule-items  (Schedule Service)
   └─ 200 (영업시간 외 경고) → 확인 다이얼로그 → force=true 재요청
   └─ 201 (성공) → 일정표 화면 갱신
```

### 4.5.3 대안 선택 흐름

```
1. POST /alternatives/search              (Alternative Service)
   └─ 402 → 구독 플랜 화면으로 이동
   └─ 200 → 대안 카드 3장 표시
2. POST /alternatives/{alt_id}/select     (Alternative Service)
   └─ 200 → 일정표 화면으로 이동 + 변경 하이라이트
```

### 4.5.4 결제 흐름

```
1. GET /subscriptions/plans               (Payment Service)
   └─ 플랜 목록 표시
2. 인앱 결제 (Apple StoreKit / Google Play Billing)
   └─ 영수증 획득
3. POST /subscriptions/purchase           (Payment Service)
   └─ 201 → new_access_token 저장 → 대안 기능 즉시 활성화
   └─ 402 → "결제 실패" 안내
   └─ 503 → "서비스 일시 불가" + 재시도
```

---

*본 설계서는 구현 코드를 포함하지 않으며, Flutter 클라이언트 개발 시 참조하는 API 연동 명세입니다.*
