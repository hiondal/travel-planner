# 논리 아키텍처 설계서

> 작성자: 아키 (소프트웨어 아키텍트)
> 작성일: 2026-02-23
> 근거: architecture.md 선정 9개 MVP 패턴 + userstory.md 9개 Epic + 7개 마이크로서비스
> 관련 문서: `docs/design/architecture.md` (패턴 선정), `docs/design/logical-architecture.mmd` (Mermaid 다이어그램)

---

## 1. 개요

### 1.1 설계 원칙

1. **서비스 경계 명확화**: 이벤트 스토밍에서 도출한 7개 Bounded Context를 서비스로 구현. MVP에서는 모놀리스 내 패키지 분리, Phase 2에서 마이크로서비스 분리 가능하도록 설계.

2. **외부 API 안정성**: Cache-Aside + Circuit Breaker + Retry 삼총사로 Google Places/OpenWeatherMap/Google Directions 3개 API 장애에 대응. 부분 실패 허용(Graceful Degradation).

3. **이벤트 기반 비동기 통신**: Publisher-Subscriber + Choreography 조합으로 "일정 등록 → 모니터링 시작 → 상태 변경 → 알림 → 브리핑 → 대안" 흐름을 느슨하게 결합.

4. **스케줄링 신뢰성**: 15분 주기 데이터 수집과 출발 전 브리핑 트리거를 `job_execution_log` 테이블 기반 Supervisor로 보장.

5. **인증/인가 일원화**: API Gateway에서 JWT 검증 + Federated Identity로 소셜 로그인 위임. 구독 티어 기반 접근 제어는 토큰 클레임 기반.

6. **비용 최적화**: 인바운드 Rate Limiting(API Gateway) + 아웃바운드 Rate Limiting(resilience4j)으로 외부 API 호출 비용 제어.

### 1.2 핵심 컴포넌트 정의

| 컴포넌트 | 역할 | 관련 패턴 |
|----------|------|---------|
| **API Gateway** | 단일 진입점, JWT 검증, 라우팅, 인바운드 Rate Limiting, CORS | API Gateway, Rate Limiting |
| **서비스 모듈 (7개)** | AUTH, SCHD, PLCE, MNTR, BRIF, ALTN, PAY | 각 서비스별 패턴 적용 |
| **이벤트 버스** | 서비스 간 비동기 통신 인프라 (MVP: Spring ApplicationEvent + @TransactionalEventListener) | Publisher-Subscriber, Choreography |
| **캐시 계층** | Redis 기반 장소 데이터, 배지 데이터 캐싱 (Cache-Aside, 5분 TTL) | Cache-Aside |
| **스케줄러 (Supervisor)** | job_execution_log 테이블 기반 작업 실행, 실패 감지, 재시도 (3회 제한) | Scheduler Agent Supervisor |
| **외부 API 클라이언트** | Circuit Breaker + Retry + 아웃바운드 Rate Limiting 적용 | Circuit Breaker, Retry, Rate Limiting |
| **데이터베이스** | 논리 모델 기반 영속성 (PostgreSQL) | - |

---

## 2. 서비스 아키텍처

### 2.1 서비스별 책임 및 경계

#### AUTH (Authentication & Authorization)
**책임**: 사용자 인증, 토큰 발급/갱신, 구독 티어 관리

**핵심 기능**:
- 소셜 로그인 (Google/Apple OAuth 2.0) - UFR-AUTH-010
- JWT Access Token(30분) + Refresh Token 발급 및 갱신
- 온보딩 상태 관리 (첫 로그인 시 닉네임/프로필 이미지 설정)
- 구독 티어 정보 조회 (Free/Trip Pass/Pro)

**통신 방식**: 동기 (인증은 즉시 응답 필수)
**적용 패턴**: Federated Identity

**API 경계**:
- `POST /auth/login`: 소셜 로그인
- `POST /auth/refresh`: 토큰 갱신
- `GET /auth/profile`: 사용자 정보 조회
- `GET /auth/subscription`: 구독 정보 조회

---

#### SCHD (Schedule & Itinerary Management)
**책임**: 여행 일정, 장소, 이동시간 관리

**핵심 기능**:
- 여행/일정/장소 CRUD (UFR-SCHD-005~070)
- 장소 추가 시 모니터링 대상 등록 (이벤트 발행)
- 장소 교체 시 모니터링 대상 변경 (이벤트 발행)
- 이동시간 재계산 (Google Directions API via PLCE 서비스)
- 일정 상태 조회 (상태 배지 캐시 조회)

**통신 방식**: 동기(일정 CRUD) + 비동기(모니터링 대상 변경)
**적용 패턴**: Cache-Aside, Publisher-Subscriber, Choreography

**API 경계**:
- `POST /trips`: 여행 생성
- `GET /trips/{tripId}`: 여행 조회
- `POST /trips/{tripId}/schedules`: 일정 생성
- `PUT /trips/{tripId}/schedules/{scheduleId}`: 일정 수정
- `POST /schedules/{scheduleId}/places`: 장소 추가
- `PUT /schedules/{scheduleId}/places/{placeId}`: 장소 교체
- `GET /schedules/{scheduleId}/places`: 장소 목록 조회

**이벤트**:
- `MonitoringTargetRegistered` (구독: MNTR)
- `MonitoringTargetChanged` (구독: MNTR)
- `PlaceReplacedInSchedule` (구독: ALTN)

---

#### PLCE (Place & Location Service)
**책임**: 장소 검색, 상세 정보, 캐싱 관리

**핵심 기능**:
- Google Places API 연동 (장소 검색, 상세 정보) - UFR-PLCE-010~030
- 장소 데이터 캐싱 (Cache-Aside, 5분 TTL)
- Google Directions API 연동 (이동시간 계산)
- 캐시 폴백 (API 실패 시 과거 데이터 사용)

**통신 방식**: 동기 (사용자 검색 응답 필수), 외부 API 의존
**적용 패턴**: Cache-Aside, Circuit Breaker, Retry, Rate Limiting (아웃바운드)

**API 경계**:
- `GET /places/search?query={query}&lat={lat}&lng={lng}`: 장소 검색
- `GET /places/{placeId}`: 장소 상세 정보
- `POST /directions/calculate`: 이동시간 계산

**외부 API 연동**:
- Google Places API (검색, 상세 정보)
- Google Directions API (이동시간)
- Circuit Breaker 상태: 3회 실패 시 60초 open, 캐시 폴백

---

#### MNTR (Real-time Monitoring Service)
**책임**: 외부 데이터 수집, 상태 판정, 상태 변경 알림

**핵심 기능**:
- 15분 주기 데이터 수집 (날씨, 혼잡도, 영업시간) - UFR-MNTR-010~050
- 3개 외부 API 병렬 호출 (Google Places, OpenWeatherMap, Google Directions)
- 상태 배지 판정 (안심/주의/위험/불가능)
- 상태 변경 시 이벤트 발행 (BRIF, ALTN 구독)
- 모니터링 대상 변경 감지 (SCHD 이벤트 구독)

**통신 방식**: 비동기 (스케줄러 기반), 외부 API 의존
**적용 패턴**: Scheduler Agent Supervisor, Publisher-Subscriber, Circuit Breaker, Retry, Cache-Aside, Rate Limiting

**스케줄링**:
- 15분 주기 데이터 수집 (Spring @Scheduled)
- job_execution_log 테이블 기반 Supervisor (5분 주기)
- 실패 시 3회 재시도, 타임아웃 3분

**API 경계**:
- `POST /monitoring/collect`: 데이터 수집 (스케줄러 호출)
- `POST /monitoring/badge/update`: 상태 배지 업데이트
- `GET /monitoring/status/{placeId}`: 상태 조회

**이벤트**:
- 구독: `MonitoringTargetRegistered`, `MonitoringTargetChanged` (SCHD)
- 발행: `StatusBadgeUpdated` (BRIF, ALTN 구독), `StatusDeteriorated` (Push 알림 트리거)

**외부 API 연동**:
- Google Places API (혼잡도, 영업시간)
- OpenWeatherMap API (날씨)
- Google Directions API (교통)
- Rate Limiting: 일일 한도 관리 (Google Places 1,000회/일)

---

#### BRIF (Briefing Service)
**책임**: 출발 전 브리핑 생성, 상태 기반 브리핑 생성, Push 발송

**핵심 기능**:
- 출발 15~30분 전 브리핑 생성 - UFR-BRIF-010~060
- 상태 기반 브리핑 (안심/주의)
- 멱등성 보장 (동일 장소+출발시간 중복 생성 방지)
- 구독 티어 기반 브리핑 제공 (Free: 기본, Trip Pass: 상세, Pro: AI 컨시어지)
- Firebase Cloud Messaging (FCM) Push 발송

**통신 방식**: 비동기 (스케줄러 기반 또는 이벤트 구독)
**적용 패턴**: Scheduler Agent Supervisor, Publisher-Subscriber, Cache-Aside

**멱등성 설계**:
- 멱등성 키: `(user_id, place_id, DATE(scheduled_departure_time))`
- 데이터베이스 유니크 제약으로 중복 생성 방지

**스케줄링**:
- 출발 15~30분 전 브리핑 생성 (Spring @Scheduled)
- job_execution_log 테이블 기반 Supervisor

**API 경계**:
- `POST /briefing/generate`: 브리핑 생성 (스케줄러 호출)
- `GET /briefing/{tripId}/{scheduleId}`: 브리핑 조회
- `POST /briefing/send-push`: Push 발송 (내부)

**이벤트**:
- 구독: `StatusBadgeUpdated` (MNTR) - 주의 브리핑 트리거
- 발행: `BriefingGenerated` (추후 사용 가능)

---

#### ALTN (Alternative Suggestion Service)
**책임**: 대안 장소 검색, 스코어링, 카드 생성

**핵심 기능**:
- 주의/위험 상태 시 대안 장소 검색 - UFR-ALTN-010~050
- 반경 확장 폴백 (1km -> 2km -> 5km)
- 상태 기반 스코어링 (현재 상태, 평점, 거리, 혼잡도)
- 대안 카드 생성 및 캐싱
- 구독 티어 기반 대안 개수 제한 (Free: 1개, Trip Pass: 3개, Pro: 5개)

**통신 방식**: 동기 (사용자 선택 응답 필수) + 비동기 (이벤트 구독)
**적용 패턴**: Cache-Aside, Circuit Breaker, Choreography

**파이프라인 성능**:
- 전체 파이프라인: 정상 3초, 최대 5초 (NFR-PERF-020)
- 장소 검색 (1초) -> 상태 확인 (1초) -> 스코어링 (1초) -> 카드 생성 (0.5초)

**API 경계**:
- `POST /alternatives/search?lat={lat}&lng={lng}&radius={radius}`: 대안 검색
- `GET /alternatives/{scheduleId}`: 대안 카드 목록 조회
- `POST /alternatives/{cardId}/adopt`: 대안 선택 (일정 교체)

**이벤트**:
- 구독: `StatusDeteriorated` (MNTR) - 주의/위험 상태
- 발행: `AlternativeAdopted` (SCHD 구독 - 일정 교체)

---

#### PAY (Payment & Subscription Service)
**책임**: 인앱 결제, 구독 관리, 구독 상태 동기화

**핵심 기능**:
- Apple In-App Purchase (IAP) 연동 - UFR-PAY-010
- Google Play Billing 연동
- 구독 상태 관리 (활성, 일시중지, 취소)
- 구독 토큰 갱신 및 검증

**통신 방식**: 동기 (결제 즉시 반영)
**적용 패턴**: Federated Identity

**API 경계**:
- `POST /subscriptions/purchase`: 구독 구매
- `POST /subscriptions/restore`: 구독 복구 (iOS)
- `GET /subscriptions/status`: 구독 상태 조회
- `POST /subscriptions/cancel`: 구독 취소

**외부 연동**:
- Apple In-App Purchase Server API
- Google Play Billing Library

---

### 2.2 서비스 간 통신 전략

#### 동기 통신 (Synchronous)
**사용 시점**: 사용자 응답이 필요한 경우, 즉시 결과 필요

**서비스 조합**:
1. API Gateway -> 모든 서비스 (라우팅)
2. AUTH -> 외부 OAuth (소셜 로그인)
3. SCHD -> PLCE (이동시간 계산)
4. ALTN -> PLCE (대안 검색)
5. ALTN -> MNTR (상태 확인)

**응답시간**: 사용자 대면 API p95 < 2초 (NFR-PERF-010)

#### 비동기 통신 (Asynchronous via Event Bus)
**사용 시점**: 후처리 작업, 다중 구독자, 시간 여유

**이벤트 흐름**:

```
[일정 등록/변경]
SCHD --[MonitoringTargetRegistered/MonitoringTargetChanged]--> Event Bus
                                                                |
                                                          MNTR (구독)
                                                                |
[15분 주기 데이터 수집 완료]
MNTR --[StatusBadgeUpdated/StatusDeteriorated]--> Event Bus
                                                    |
                                          +---> BRIF (구독, 주의 브리핑)
                                          |
                                          +---> ALTN (구독, 대안 검색)
                                          |
                                          +--> SCHD (캐시 갱신)

[대안 선택]
ALTN --[AlternativeAdopted]--> Event Bus
                                 |
                            SCHD (구독, 일정 교체)
                                 |
                            MNTR (구독, 모니터링 변경)
```

**이벤트 버스 구현**:
- MVP: Spring ApplicationEvent + @TransactionalEventListener(phase = AFTER_COMMIT)
- 이벤트 유실 복구: outbox_events 테이블 기반 보상 조회
- Phase 2: AWS SQS/SNS 전환

---

### 2.3 주요 사용자 플로우

#### 플로우 1: 여행 일정 등록 및 모니터링 시작 (UFR-SCHD-005 + UFR-MNTR-010)
```
1. 사용자가 여행 생성 (API Gateway -> SCHD)
2. 일정 생성 및 장소 추가 (SCHD)
3. 장소 추가 완료 시 MonitoringTargetRegistered 이벤트 발행
4. MNTR이 이벤트 구독, 모니터링 대상 등록
5. 다음 15분 주기 스케줄러 실행 시 데이터 수집 시작
   - job_execution_log에 작업 등록 (PENDING)
   - Google Places, OpenWeatherMap, Google Directions 병렬 호출
   - Circuit Breaker + Retry + Rate Limiting 적용
   - 수집 완료 후 배지 판정, 상태 배지 업데이트
6. StatusBadgeUpdated 이벤트 발행
7. SCHD, BRIF, ALTN이 이벤트 구독하여 각각 처리
   - SCHD: 캐시 갱신
   - BRIF: 조건 확인 후 브리핑 생성 (출발 15~30분 전 && 주의)
   - ALTN: 캐시 갱신
```

**응답시간**: 일정 생성 < 1초, 모니터링 시작 (비동기) < 5분

---

#### 플로우 2: 출발 전 브리핑 생성 및 Push 발송 (UFR-BRIF-010 + UFR-BRIF-050)
```
1. 스케줄러가 출발 15~30분 전 브리핑 생성 트리거
2. job_execution_log에 작업 등록 (PENDING)
3. BRIF 서비스가 브리핑 생성
   - 멱등성 키 확인 (user_id, place_id, DATE(scheduled_departure_time))
   - 구독 티어 확인 (AUTH 토큰 클레임)
   - 현재 상태 조회 (MNTR 캐시 via Cache-Aside)
   - 브리핑 템플릿 선택 (안심/주의)
4. 브리핑 생성 완료, FCM Push 발송
5. job_execution_log에 작업 상태 UPDATE (COMPLETED)
```

**응답시간**: 브리핑 생성 < 2초, Push 발송 < 10초 (NFR-PERF-030)

---

#### 플로우 3: 대안 카드 검색 및 선택 (UFR-ALTN-010 + UFR-ALTN-030)
```
1. 사용자가 주의/위험 상태 시 대안 검색 요청 (API Gateway -> ALTN)
2. ALTN이 대안 검색 파이프라인 실행 (p95 < 3초, 최대 5초)
   - PLCE 서비스에 대안 장소 검색 요청 (반경 1km)
   - MNTR 서비스에 대안 장소 상태 조회 (캐시)
   - 스코어링 계산 (상태, 평점, 거리, 혼잡도)
   - 구독 티어 기반 개수 제한 (Free: 1개, Trip Pass: 3개, Pro: 5개)
   - 대안 카드 생성
3. 사용자가 대안 선택 (AlternativeAdopted 이벤트 발행)
4. SCHD가 이벤트 구독, 일정 교체 처리
   - 기존 장소 제거, 새 장소 추가
   - MonitoringTargetChanged 이벤트 발행
5. MNTR이 이벤트 구독, 모니터링 대상 변경
   - 기존 수집 데이터 유지 또는 초기화
   - 다음 주기부터 새 장소 모니터링
```

**응답시간**: 대안 검색 < 5초, 일정 교체 < 1초

---

## 3. 데이터 흐름 및 캐싱 전략

### 3.1 캐싱 전략 (Cache-Aside)

**캐시 대상 데이터**:

| 데이터 | TTL | 사용처 | 갱신 방식 |
|--------|-----|-------|---------|
| 장소 상세 (Place) | 5분 | PLCE 서비스, BRIF, ALTN | 만료 또는 명시적 갱신 |
| 상태 배지 (StatusBadge) | 5분 | SCHD UI, BRIF, ALTN | MNTR 업데이트 시 갱신 |
| 브리핑 (Briefing) | 30분 | 사용자 조회 | 생성 시 저장 |
| 대안 카드 (AlternativeCard) | 10분 | 사용자 조회 | 생성 시 저장 |
| 구독 정보 (Subscription) | 1시간 | API Gateway (토큰 검증) | PAY 업데이트 시 갱신 |

**Cache-Aside 패턴 구현**:
```
1. 데이터 요청
2. 캐시 히트 -> 캐시 데이터 반환
3. 캐시 미스 또는 만료
   - DB 또는 외부 API 조회
   - 결과를 캐시에 저장 (TTL 설정)
   - 데이터 반환
4. 외부 API 실패 (Circuit Breaker open)
   - 캐시가 있으면 캐시 데이터 반환 (Graceful Degradation)
   - 캐시가 없으면 에러 반환
```

**외부 API 폴백 시나리오**:
- Circuit Breaker open 상태일 때: 캐시 데이터 사용 (최대 30분 전 데이터)
- 캐시 데이터도 없을 때: 사용자에게 "데이터를 일시적으로 사용할 수 없습니다" 메시지

---

### 3.2 데이터 흐름 (외부 API 의존)

**수집 데이터 흐름** (MNTR 서비스):
```
Google Places API
    |-- 장소 혼잡도
    |-- 영업 상태
    |-- 평점
         |
         v
    MNTR 서비스 --[Circuit Breaker + Retry + Rate Limiting]--> Cache (Redis)
         ^                                                          |
         |                                                          v
         +-- OpenWeatherMap API                                BRIF, ALTN (캐시 조회)
         |-- 날씨 정보
         |
         +-- Google Directions API
         |-- 교통 정보
         |-- 이동시간
```

**데이터 갱신 타이밍**:
- **주기적 갱신**: 15분 주기 (스케줄러)
- **이벤트 기반 갱신**: 장소 변경 시 (MonitoringTargetChanged)
- **수요 기반 갱신**: 사용자 요청 시 캐시 미스 -> API 호출

---

## 4. 확장성 및 성능 고려사항

### 4.1 확장성 설계

**모듈 분리 가능성**:
- MVP: 모놀리스 내 패키지 분리
- Phase 2: 마이크로서비스 분리 시 각 서비스별 독립 배포 가능
- 서비스 경계: Event Bus 기반 느슨한 결합

**도시 확장 지원**:
- 초기: 5개 도시 (서울, 도쿄, 방콕, 싱가포르, 홍콩)
- Phase 2: 15개+ 도시
- 확장 시 필요 작업:
  - 각 도시별 외부 API 설정 (Google Places, OpenWeatherMap)
  - 상태 판정 임계값 설정 (날씨, 혼잡도, 영업시간 기준)
  - 데이터 수집 대상 증설 (job_execution_log 기반)

### 4.2 성능 목표 및 달성 방안

| 목표 | NFR ID | 기준 | 달성 방안 |
|------|--------|------|---------|
| 사용자 대면 API p95 | NFR-PERF-010 | 2초 이내 | API Gateway 캐싱, Circuit Breaker 빠른 실패 |
| 대안 검색 파이프라인 | NFR-PERF-020 | 정상 3초, 최대 5초 | 병렬 호출, 2초 타임아웃, 비동기 Request-Reply (Phase 2) |
| 브리핑 생성~Push | NFR-PERF-030 | 10초 이내 | 캐시 재사용, 스케줄러 기반 사전 생성 |

**성능 최적화 기법**:
- **캐시 우선**: 장소 데이터는 항상 캐시에서 조회 (5분 TTL)
- **병렬 처리**: 외부 API 병렬 호출 (Places + Weather + Directions)
- **비동기 처리**: 후처리 작업 (이벤트 발행, Push 발송)
- **조기 실패**: Circuit Breaker open 상태에서 즉시 폴백

---

## 5. 보안 고려사항

### 5.1 인증 및 인가

**인증** (Authentication):
- Federated Identity: Google/Apple OAuth 2.0 위임
- JWT Access Token (30분) + Refresh Token (7일)
- API Gateway에서 JWT 검증 (Offloading)

**인가** (Authorization):
- 구독 티어 기반 기능 제어 (Free/Trip Pass/Pro)
- 토큰 클레임에 `tier` 정보 포함
- API Gateway 및 서비스 레벨에서 검증

**보안 흐름**:
```
1. 클라이언트: 소셜 로그인 (Google/Apple)
2. AUTH 서비스: Federated Identity로 토큰 발급
3. API Gateway: JWT 검증 (공개키 기반)
4. 서비스: 토큰 클레임에서 user_id, tier 추출
5. 리소스 접근 제어: tier 기반 분기
```

### 5.2 위치정보 보호

**GDPR 준수**:
- 여행 종료 후 30일 보유, 이후 삭제
- 사용자 동의 기반 위치정보 수집
- 데이터 최소화 원칙 (필요한 정보만 수집)

**데이터 보호**:
- 위치정보는 암호화 저장 (AES-256)
- 전송 시 HTTPS 사용 (TLS 1.2+)

---

## 6. 논리아키텍처 다이어그램

Mermaid 다이어그램은 `docs/design/logical-architecture.mmd` 파일을 참고하세요.

다이어그램 구조:
- **Client Layer**: 모바일 앱 (React Native)
- **Gateway Layer**: API Gateway (JWT 검증, 라우팅, 인바운드 Rate Limiting)
- **Service Layer**: 7개 서비스 모듈 (AUTH, SCHD, PLCE, MNTR, BRIF, ALTN, PAY)
- **Data Layer**: Redis Cache, Event Bus (Spring ApplicationEvent)
- **Scheduler Layer**: 스케줄러 및 Supervisor (job_execution_log)
- **External APIs**: Google OAuth, Google Places, OpenWeatherMap, Google Directions, FCM, Apple IAP, Google Play Billing

---

## 7. 구현 가이드

### 7.1 MVP 구현 기간: 6~9주

**Phase 1: 기반 구조 (1~2주)**
- API Gateway 구현 (Spring Cloud Gateway)
- AUTH 서비스 (OAuth 2.0 + JWT)
- 데이터베이스 스키마 (user, trip, schedule, place, monitoring_target, status_badge, briefing, job_execution_log, outbox_events)

**Phase 2: 핵심 서비스 (2~4주)**
- SCHD 서비스 (일정 CRUD, 이벤트 발행)
- PLCE 서비스 (Google Places API, 캐싱, Circuit Breaker)
- MNTR 서비스 (스케줄러, 데이터 수집, 상태 판정)

**Phase 3: 고객 기능 (3~6주)**
- BRIF 서비스 (브리핑 생성, Push 발송)
- ALTN 서비스 (대안 검색, 스코어링)
- PAY 서비스 (인앱 결제, 구독 관리)

**Phase 4: 통합 및 최적화 (1~2주)**
- 통합 테스트, 성능 최적화
- 외부 API 장애 대응 테스트 (Circuit Breaker, 캐시 폴백)
- UAT 및 버그 수정

### 7.2 테스트 전략

**단위 테스트 (Unit)**: 서비스 로직, 스코어링 알고리즘
**통합 테스트 (Integration)**: 외부 API 호출, 캐시 동작, 이벤트 발행
**시스템 테스트 (System)**: 전체 플로우 (일정 등록 -> 모니터링 -> 브리핑 -> 대안)
**카오스 엔지니어링 (Chaos)**: Circuit Breaker, 캐시 폴백 검증

---

## 8. 향후 진화 방향 (Phase 2+)

### 8.1 Phase 2 (3~6개월 후)

**외부 설정 관리**:
- External Configuration Store 패턴 (DB 기반)
- 도시별 상태 판정 임계값 관리
- 외부 API 설정 중앙화

**고급 헬스체크**:
- Health Endpoint Monitoring 패턴
- 외부 API 상태 연동 헬스체크

**마이크로서비스 분리**:
- 각 서비스 독립 배포 (Kubernetes)
- 외부 메시징 (SQS/SNS 기반 Event Bus)

**성능 최적화**:
- CQRS 패턴 (읽기/쓰기 분리, 최적화된 뷰)
- Asynchronous Request-Reply (대안 검색 비동기화)

### 8.2 Phase 3 (6~12개월 후)

**Event Sourcing**: 모든 상태 변경 이벤트 저장
**분산 추적 (Distributed Tracing)**: OpenTelemetry 기반 성능 모니터링
**머신러닝**: AI 컨시어지 고도화 (맥락 맞춤 대안 추천)

---

## 9. 체크리스트

- [x] 유저스토리와 매칭 확인 (29 UFR + 10 NFR, 9 Epic)
- [x] 9개 MVP 패턴 적용 확인
- [x] 서비스 간 관계와 이벤트 흐름 명확화
- [x] 외부 API 의존 명시 (Circuit Breaker + Cache 폴백)
- [x] 스케줄링 신뢰성 설계 (job_execution_log Supervisor)
- [x] 멱등성 설계 (브리핑, 대안)
- [x] 구독 티어 기반 분기 명시
- [x] 성능 목표 및 달성 방안 제시
- [x] 보안 및 개인정보 보호 (GDPR)
- [x] 단일 진입점 (API Gateway) 구조 확인
- [x] Phase 2+ 진화 방향 제시

