# 논리 아키텍처 설계서

> 작성자: 홍길동/아키 (소프트웨어 아키텍트)
> 작성일: 2026-02-23
> 프로젝트: travel-planner — 여행 중 실시간 일정 최적화 가이드 앱
> 참조: architecture.md (패턴 선정), userstory.md (UFR), 핵심솔루션.md (S04/S05/S06)

---

## 개요

### 설계 원칙

1. **Context Map 중심**: 서비스 내부 구조는 생략하고 서비스 간 관계·통신 방식·데이터 흐름에 집중한다.
2. **패턴 충실 적용**: 선정된 10개 클라우드 디자인 패턴(Federated Identity, Gateway Routing/Offloading, Cache-Aside, Circuit Breaker, Retry, Publisher-Subscriber, Rate Limiting, External Configuration Store, Health Endpoint Monitoring)을 설계에 직접 반영한다.
3. **MVP 범위 준수 (YAGNI)**: MVP에서 꼭 필요한 관계만 설계한다. Phase 2+ 요소는 인터페이스 추상화로만 준비하고 물리적 컴포넌트를 추가하지 않는다.
4. **AI 인터페이스 추상화**: MVP에서 AI API를 사용하지 않되, 총평 생성 로직을 `BriefingTextGenerator` 인터페이스로 추상화하여 Phase 3 LLM 전환 비용을 최소화한다.
5. **이벤트 버스 교체 가능성 확보**: 인메모리 이벤트 버스(MVP) → Azure Service Bus(Phase 2) 전환 시 서비스 코드 변경 없이 교체 가능하도록 이벤트 인터페이스를 추상화한다.
6. **부분 장애 허용 설계**: 외부 API 4종(Google Places, OpenWeatherMap, Google Directions, FCM) 중 일부가 장애 상태여도 핵심 기능(일정 조회, 배지 표시, 브리핑 생성)이 동작한다.

### 핵심 컴포넌트 정의

| 컴포넌트 | 역할 코드 | 책임 |
|---------|:--------:|------|
| **모바일 클라이언트** | CLIENT | 여행자 앱. HTTPS로 API Gateway 단일 진입점 호출 |
| **API Gateway** | GW | Azure APIM 기반. Gateway Routing(서비스 라우팅), Gateway Offloading(JWT 검증·로깅·Rate Limiting 중앙화) |
| **AUTH 서비스** | AUTH | Federated Identity. Google/Apple OAuth 위임 인증, JWT Access/Refresh Token 발급 |
| **SCHD 서비스** | SCHD | 여행 일정 CRUD, 장소 추가/교체, 이동시간 재계산, 모니터링 등록/해제 이벤트 발행 |
| **PLCE 서비스** | PLCE | 키워드·반경 기반 장소 검색, Google Places API 연동, 장소 데이터 Cache-Aside |
| **MNTR 서비스** | MNTR | 15분 주기 외부 데이터 수집(Circuit Breaker+Retry), 4단계 상태 판정(External Config), 상태 변경 이벤트 발행 |
| **BRIF 서비스** | BRIF | 출발 15~30분 전 브리핑 자동 생성, FCM Push 발송, 총평 인터페이스 추상화(Phase 3 AI 대비) |
| **ALTN 서비스** | ALTN | 맥락 기반 대안 카드 3장 생성, 규칙 기반 정렬, 일정 교체 중개, Free 티어 Paywall |
| **PAY 서비스** | PAY | 구독 결제(Trip Pass, Pro), 결제 상태 관리, 환불 처리 |
| **이벤트 버스** | BUS | MVP: 인메모리 이벤트 버스. Phase 2: Azure Service Bus 교체 예정 |
| **Redis 캐시** | CACHE | 장소 데이터(PLCE), 수집 상태 Fallback(MNTR), 대안 카드(ALTN), 세션 정보(AUTH) |
| **PostgreSQL DB** | DB | 서비스별 논리 분리 스키마. 모놀리스 내 단일 인스턴스(MVP) |
| **Azure App Config** | CONFIG | MNTR 상태 판정 임계값, 수집 주기, 타임아웃 설정 외부화 |
| **AI Pipeline** | AI | Phase 2 도입 예정. LLM 기반 총평 생성. MVP에서는 BriefingTextGenerator 인터페이스로만 존재 |

---

## 서비스 아키텍처

### 서비스별 책임

#### API Gateway (Azure APIM)

**책임**: 단일 진입점. 모든 클라이언트 요청의 관문.

**핵심 기능**:
- **Gateway Routing**: 요청 경로 기반 7개 서비스로 라우팅
- **Gateway Offloading**: JWT 검증, 구독 티어 확인, 요청 로깅, Rate Limiting을 게이트웨이 집중 처리 — 각 서비스의 인증 코드 제거
- **Rate Limiting**: 구독 티어별 API 호출 제한. Free 티어 브리핑 1일 1회(P19) 게이트웨이 레벨 선제 차단
- **Health Check 집계**: 하위 서비스 /health 응답 집계

#### AUTH 서비스

**책임**: 사용자 인증 전담. Federated Identity 패턴으로 인증을 외부 OAuth 제공자에 위임.

**핵심 기능**:
- Google OAuth 2.0 / Apple Sign In 위임 인증 — UFR-AUTH-010
- JWT Access Token(30분) + Refresh Token 발급
- 구독 티어 정보를 JWT 클레임에 포함
- 위치정보 수집 동의 이력 저장 (GDPR/위치정보법)

**데이터 저장**: PostgreSQL (users, auth_sessions, consent_records 스키마)

#### SCHD 서비스

**책임**: 여행 일정의 원천 데이터 관리. 일정 변경 이벤트의 유일한 발행자.

**핵심 기능**:
- 여행 일정 CRUD — UFR-SCHD-010
- 장소 검색 중개(PLCE 호출) — UFR-SCHD-020
- 장소 추가/교체 시 방문 시간 현지 시간(IANA) 저장, 이동시간 재계산 — UFR-SCHD-030, UFR-SCHD-040
- 모니터링 등록/해제 이벤트 발행(`ScheduleItemAdded`, `ScheduleItemReplaced`, `ScheduleItemRemoved`) — P14

**이벤트 발행**: `ScheduleItemAdded` → BUS → MNTR 구독
**동기 호출**: PLCE (장소 검색), ALTN (대안 선택 시 일정 교체 수신)

#### PLCE 서비스

**책임**: 장소 데이터의 단일 소유자. 외부 Google Places API 의존성 격리.

**핵심 기능**:
- 키워드 기반 장소 검색 — UFR-PLCE-010, UFR-SCHD-020
- 반경 기반 주변 장소 검색 — UFR-PLCE-020 (ALTN 서비스 활용)
- Google Places API 연동 — Circuit Breaker + Retry 적용
- 장소 데이터 Cache-Aside: 캐시 HIT 시 DB/외부 API 미호출

**통신**: 동기(SCHD, ALTN의 장소 검색 요청 수신), 외부 API Circuit Breaker+Retry

#### MNTR 서비스

**책임**: 실시간 상태 파이프라인 관리. 가장 복잡한 외부 의존성을 가진 서비스.

**핵심 기능**:
- 이벤트 구독(`ScheduleItemAdded`, `ScheduleItemReplaced`) → 모니터링 대상 등록/해제 — P14
- 스케줄러 주도 15분 주기 외부 데이터 수집 (병렬 3종 API) — UFR-MNTR-010
  - Google Places API (영업 상태) — Circuit Breaker: 5회/1분 실패 시 OPEN
  - OpenWeatherMap API (날씨) — Circuit Breaker: 3회/1분 실패 시 OPEN
  - Google Directions API (이동시간) — Circuit Breaker: 3회/1분 실패 시 OPEN
- 4단계 상태 판정: 초록/노랑/빨강/회색 — UFR-MNTR-020
  - 임계값은 Azure App Configuration에서 동적 로드 (External Configuration Store)
  - 3회 연속 캐시 미스 시 회색 전환
- 상태 변경 감지 → 이벤트 발행(`PlaceStatusChanged`) → BRIF 구독 — P2
- 배지 조회 API: 캐시 우선 읽기 (Cache-Aside) — C5 읽기/쓰기 불균형 대응

**이벤트 구독**: `ScheduleItemAdded` (BUS)
**이벤트 발행**: `PlaceStatusChanged` (BUS)
**외부 API Fallback 전략**:
- Google Places 장애: 마지막 캐시값 + 회색 배지
- OpenWeatherMap 장애: 마지막 캐시값 사용
- Google Directions 장애: 직선거리 기반 추정값

#### BRIF 서비스

**책임**: 출발 전 브리핑 생성과 Push 알림 발송. 총평 생성 로직의 Phase 3 AI 전환 준비.

**핵심 기능**:
- 이벤트 구독(`PlaceStatusChanged`) + 스케줄러 트리거 → 브리핑 생성 조건 판단 — UFR-BRIF-010
- 출발 15~30분 전 브리핑 자동 생성: MNTR 캐시 데이터 조회(온디맨드 외부 API 재호출 금지) — UFR-BRIF-010
- 안심/주의 브리핑 분기 — UFR-BRIF-020
- FCM Push 발송 — Circuit Breaker(10회/1분 실패 시 OPEN) + 인앱 알림 Fallback
- 브리핑 멱등성 보장: 장소ID + 출발시간 해시 기반 중복 방지 — P21
- 구독 티어 분기: Free 1일 1회 (API Gateway Rate Limiting에서 선제 차단, 서비스 레이어 이중 방어) — P19

**총평 생성 인터페이스 추상화** (Phase 3 AI 전환 대비):
```
BriefingTextGenerator (interface)
  ├─ RuleBasedGenerator  → MVP: 템플릿 기반 규칙 엔진 (현재 구현체)
  └─ LLMGenerator        → Phase 3: Azure OpenAI 연동 (미래 구현체)
```

**이벤트 구독**: `PlaceStatusChanged` (BUS)
**외부 통신**: FCM — Circuit Breaker + Retry

#### ALTN 서비스

**책임**: 맥락 맞춤 대안 카드 생성 및 일정 교체 중개.

**핵심 기능**:
- 맥락 기반 대안 장소 검색: PLCE 호출 (동일 카테고리, 반경 1~3km, 영업 중) — UFR-ALTN-010
- 대안 카드 3장 생성: 거리>평점>혼잡도 합산 점수 규칙 정렬 — UFR-ALTN-020
- 대안 카드 Cache-Aside: 동일 맥락(장소+상태 조합) 결과 캐싱
- 일정 교체 중개: SCHD 동기 API 호출 → 모니터링 대상 변경 이벤트 연쇄 발행 — UFR-ALTN-030
- Free 티어 Paywall: 대안 카드 탭 시 구독 유도 — P22

**통신**: 동기(PLCE 장소 검색, SCHD 일정 교체)
**분산 트랜잭션(C6)**: MVP는 모놀리스 단일 DB 트랜잭션으로 처리. Phase 2(서비스 분리 후) Saga 패턴 도입.

#### PAY 서비스

**책임**: 구독 결제 및 상태 관리.

**핵심 기능**:
- 구독 플랜 결제(Trip Pass, Pro) — UFR-PAY-010
- 결제 게이트웨이 연동: Circuit Breaker + Retry
- 구독 상태 변경 시 AUTH 서비스 JWT 클레임 갱신 트리거

---

### 서비스 간 통신 전략

#### 동기 통신 (REST/HTTPS)

| 호출 방향 | 통신 방식 | 적용 패턴 | 설명 |
|---------|---------|---------|------|
| CLIENT → GW | HTTPS | Rate Limiting | 단일 진입점. 모든 요청의 관문 |
| GW → 각 서비스 | HTTP (내부망) | Gateway Routing | 경로 기반 라우팅 |
| SCHD → PLCE | HTTP | - | 장소 검색 요청 (UFR-SCHD-020) |
| ALTN → PLCE | HTTP | Cache-Aside | 주변 장소 검색 (대안 카드 생성) |
| ALTN → SCHD | HTTP | - | 일정 교체 중개 (UFR-ALTN-030) |
| BRIF → MNTR | HTTP | Cache-Aside | 캐시된 상태 데이터 조회 (실시간 재호출 금지) |
| MNTR → CONFIG | HTTP | External Configuration Store | 임계값 동적 로드 |
| PAY → 결제GW | HTTPS | Circuit Breaker + Retry | 결제 게이트웨이 외부 호출 |
| PLCE → Google Places | HTTPS | Circuit Breaker + Retry | 장소 검색/상세 조회 |
| MNTR → Google Places | HTTPS | Circuit Breaker + Retry | 영업 상태 수집 |
| MNTR → OpenWeatherMap | HTTPS | Circuit Breaker + Retry | 날씨 데이터 수집 |
| MNTR → Google Directions | HTTPS | Circuit Breaker + Retry | 이동시간 수집 |
| BRIF → FCM | HTTPS | Circuit Breaker + Retry | Push 알림 발송 |
| AUTH → Google/Apple OAuth | HTTPS | Federated Identity | OAuth 위임 인증 |

#### 비동기 통신 (이벤트 버스)

| 이벤트 | 발행자 | 구독자 | 설명 |
|-------|-------|-------|------|
| `ScheduleItemAdded` | SCHD | MNTR | 일정 장소 추가 → 모니터링 등록 |
| `ScheduleItemReplaced` | SCHD | MNTR | 일정 장소 교체 → 모니터링 대상 변경 |
| `ScheduleItemRemoved` | SCHD | MNTR | 일정 장소 삭제 → 모니터링 해제 |
| `PlaceStatusChanged` | MNTR | BRIF | 상태 변경 감지 → 브리핑 트리거 |

**MVP 이벤트 버스**: 인메모리 (모놀리스 내부)
**Phase 2 전환**: Azure Service Bus (동시 모니터링 장소 1,000개 초과 또는 서비스 분리 시)
**충돌 대응 (Publisher-Subscriber vs 강한 일관성)**: 일정 교체(C6)는 동기 API 호출(ALTN→SCHD)로 처리. 모니터링 대상 변경만 비동기 이벤트로 발행.

#### 캐시 우선 읽기 (Cache-Aside)

| 캐시 대상 | 서비스 | TTL | Fallback |
|---------|-------|:---:|---------|
| 장소 데이터 (영업 상태, 상세) | PLCE | 5분 | Google Places API 실시간 호출 |
| 모니터링 수집 상태 | MNTR | 10분 | 회색 배지 전환 (3회 캐시 미스) |
| 날씨 데이터 | MNTR | 10분 | 마지막 캐시값 |
| 이동시간 | MNTR | 30분 | 직선거리 추정값 |
| 대안 카드 결과 | ALTN | 5분 | PLCE 재검색 |
| 세션 정보 | AUTH | 30분 | DB 조회 |
| 배지 상태 (읽기 전용) | MNTR | 10분 | DB 직접 조회 |

**캐시 무효화**: `PlaceStatusChanged` 이벤트 수신 시 해당 장소 캐시 즉시 무효화.
**배지 상태 캐시 TTL**: 수집 주기(15분)보다 짧은 10분으로 설정하여 Cache-Aside와 실시간 배지 요구 충돌 방지.

---

## AI 서비스 아키텍처

### MVP 단계 — 인터페이스 추상화만 포함

MVP에서는 AI API를 사용하지 않는다. 그러나 총평 생성 로직은 외부 호출 가능한 인터페이스로 추상화하여 Phase 3 LLM 전환 비용을 최소화한다.

**책임**: 브리핑 총평 텍스트 생성 (현재: 규칙 기반 템플릿)

**핵심 기능**:
- 총평 생성 인터페이스 추상화 (`BriefingTextGenerator`) — UFR-BRIF-010
- MVP 구현체: `RuleBasedGenerator` (템플릿 기반 규칙 엔진)
  - "현재까지 모든 항목 정상입니다." 패턴
  - 안심/주의 분기 로직

**AI 기술 요소** (Phase 3 도입 예정):
- LLM API: Azure OpenAI (GPT-4o-mini)
- AI 프레임워크: 직접 구현 (Phase 2), LangChain 검토 (Phase 3)
- RAG: 미적용 (Phase 3 검토 — 사용자 선호 프로파일 기반 개인화)
- Function Calling: Phase 3 AI 자동 재조정 엔진에서 도입

**통신 방식**: BRIF 서비스 내부 호출 (MVP). Phase 3에서 별도 AI Pipeline 서비스로 분리.

**폴백 전략**:
- Phase 3 LLM 장애 시: `RuleBasedGenerator`로 자동 Fallback
- Circuit Breaker(3회/30초 실패 시 OPEN) 적용 예정

**다이어그램 표현**: 논리 아키텍처 다이어그램에 "Phase 2 도입 예정"으로 점선 표현.

### AI 학습 데이터 파이프라인 준비 (MVP 단계 수집)

Phase 2 ML 모델의 원재료를 MVP 단계부터 수집한다.

| 수집 데이터 | 서비스 | 저장 방식 | 목적 |
|----------|-------|---------|------|
| 상태 판정 이력 | MNTR | append-only 이력 테이블 (6개월 이상 보존) | 상태 판정 AI 학습 |
| 대안 카드 선택 이력 | ALTN | 노출 3건 중 선택 1건 레이블링 | 대안 추천 ML |
| 브리핑 열람 여부 | BRIF | 이벤트 로그 | 브리핑 품질 측정 |
| Push 알림 수신-탭 간격 | BRIF | 이벤트 로그 | 알림 타이밍 최적화 |

---

## 주요 사용자 플로우

### Flow 1: S05 상태 배지 조회 (UFR-MNTR-010, UFR-SCHD-030)

```
여행자 → GW → MNTR → Redis 캐시
                          ↓ HIT: 캐시 반환 (10분 TTL)
                          ↓ MISS: DB 직접 조회
                            ↓ 캐시 저장 후 반환
```

읽기 트래픽이 쓰기 대비 압도적으로 높음(C5). 배지 조회는 매 앱 오픈 시 발생하므로 캐시 우선 전략 필수.

### Flow 2: S04 출발 전 브리핑 생성 (UFR-BRIF-010, UFR-BRIF-020)

```
Scheduler(15분 전) → BRIF
    ↓ MNTR 캐시 데이터 조회 (실시간 재호출 금지)
    ↓ 안심/주의 분기 판단
    ↓ BriefingTextGenerator.generate() → RuleBasedGenerator (MVP)
    ↓ 멱등성 키 확인 (장소ID + 출발시간 해시)
    ↓ FCM Circuit Breaker 상태 확인
        ↓ CLOSED: FCM Push 발송
        ↓ OPEN: 인앱 알림 Fallback
```

BRIF 서비스는 MNTR 캐시 데이터만 사용한다. 온디맨드 외부 API 재호출 금지.

### Flow 3: S06 대안 카드 검색 및 일정 교체 (UFR-ALTN-010, UFR-ALTN-020, UFR-ALTN-030)

```
여행자 탭 → GW → ALTN
    ↓ Free 티어 Paywall 확인
    ↓ 대안 카드 캐시 확인 (5분 TTL)
        ↓ HIT: 캐시 반환
        ↓ MISS: PLCE 주변 장소 검색 (동일 카테고리, 반경 1~3km, 영업 중)
              ↓ 거리>평점>혼잡도 합산 점수 정렬 → 3장 선택
              ↓ 캐시 저장 (5분 TTL)
    ↓ 여행자 카드 선택 → ALTN → SCHD (동기 호출, 단일 DB 트랜잭션)
              ↓ 일정 교체 완료 → ScheduleItemReplaced 이벤트 → BUS → MNTR 모니터링 대상 변경
```

C6(분산 트랜잭션): MVP 모놀리스에서는 ALTN→SCHD 동기 호출 + 단일 DB 트랜잭션으로 처리. Phase 2 서비스 분리 후 Saga 도입.

### Flow 4: 모니터링 데이터 수집 파이프라인 (UFR-MNTR-010)

```
Scheduler(15분 주기) → MNTR
    ↓ 모니터링 대상 목록 조회 (DB)
    ↓ 외부 API 병렬 3종 호출 (Circuit Breaker + Retry)
        ├─ Google Places API (Circuit Breaker: 5회/1분 → Fallback: 캐시값)
        ├─ OpenWeatherMap API (Circuit Breaker: 3회/1분 → Fallback: 캐시값)
        └─ Google Directions API (Circuit Breaker: 3회/1분 → Fallback: 직선거리)
    ↓ 수집 결과 캐시 저장 (Redis)
    ↓ 4단계 상태 판정 (임계값: Azure App Configuration)
    ↓ 상태 변경 감지 → PlaceStatusChanged 이벤트 → BUS → BRIF
```

---

## 데이터 흐름 및 캐싱 전략

### 데이터 저장 원칙

- **PostgreSQL (Azure Database for PostgreSQL)**: 서비스별 논리 스키마 분리. MVP는 단일 인스턴스, 모놀리스 내 공유.
- **Redis (Azure Cache for Redis)**: 읽기 최적화 전용. 서비스별 캐시 키 네임스페이스 분리.
- **Azure App Configuration**: MNTR 임계값, 수집 주기, 타임아웃 설정. 재배포 없이 운영 중 변경.

### 데이터 소유권 (서비스별)

| 서비스 | DB 스키마 | 설명 |
|-------|---------|------|
| AUTH | users, auth_sessions, consent_records | 사용자 인증 및 동의 이력 |
| SCHD | trips, schedule_items, travel_routes | 여행 일정 원천 데이터 |
| PLCE | places, place_details | 장소 마스터 데이터 |
| MNTR | monitoring_targets, status_history, status_current | 모니터링 대상 및 상태 이력 |
| BRIF | briefings, briefing_logs | 브리핑 생성 이력 및 멱등성 키 |
| ALTN | alternative_cards, selection_logs | 대안 카드 생성 및 선택 이력 |
| PAY | subscriptions, payment_records, refunds | 결제 및 구독 정보 |

### 캐시 키 설계

```
PLCE: place:{place_id}:status:{floor(timestamp/300)}   # 5분 슬롯 단위 — 동일 시간대 공유
MNTR: mntr:{place_id}:weather:{floor(timestamp/600)}   # 10분 슬롯
MNTR: mntr:{place_id}:badge                            # 배지 상태 현재값
ALTN: altn:{place_id}:{category}:{radius}              # 대안 카드 검색 결과
AUTH: sess:{user_id}:token                             # 세션 정보
```

### 캐시 무효화 트리거

- `PlaceStatusChanged` 이벤트 수신 → `mntr:{place_id}:badge` 즉시 무효화
- 장소 교체(`ScheduleItemReplaced`) → 이전 장소 대안 카드 캐시 무효화
- 수동 무효화: 운영자 Azure App Configuration 설정 변경 시

---

## 확장성 및 성능 고려사항

| 도전과제 | 대응 방안 | 적용 패턴 |
|---------|---------|---------|
| C1 외부 API 장애 | 3중 방어: Circuit Breaker(차단) + Retry(재시도) + Cache-Aside(Fallback) | Circuit Breaker, Retry, Cache-Aside |
| C2 15분 주기 대량 수집 | 병렬 3종 API 호출, 수집 타임아웃 2초, 스케줄러 내부 호출 Rate Limit 예외 처리 | External Configuration Store |
| C3 비동기 이벤트 연동 | 인메모리 이벤트 버스(MVP) → Azure Service Bus(Phase 2) 인터페이스 추상화 | Publisher-Subscriber |
| C5 읽기/쓰기 불균형 | 배지 조회 Cache-Aside (TTL 10분), 캐시 무효화 이벤트 연동 | Cache-Aside |
| C6 분산 트랜잭션 | MVP: 단일 DB 트랜잭션. Phase 2: Saga 패턴 도입 | (Phase 2: Saga) |
| 동시 모니터링 1,000개+ | Phase 2: MNTR 서비스 독립 분리, Azure Container Apps 스케일링 | (Phase 2 배포 전략) |

**Phase 2 전환 조건**:
- 동시 모니터링 장소 1,000개 초과
- 배지 조회 응답 2초 초과 지속
- 인메모리 이벤트 버스 안정성 이슈 발생

---

## 보안 고려사항

| 보안 요구사항 | 대응 방안 | 적용 위치 |
|-----------|---------|---------|
| 인증/인가 | Federated Identity(Google/Apple OAuth), JWT(30분 만료) | AUTH, GW |
| 구독 티어 접근 제어 | JWT 클레임 내 tier 정보, Gateway Offloading 검증 | GW, BRIF, ALTN |
| API 남용 방지 | Rate Limiting (구독 티어별 차등 적용) | GW |
| GDPR/위치정보법 | 동의 이력 저장(consent_records), 위치정보 수집 목적 명시 | AUTH, SCHD |
| 서비스 간 통신 | 내부망 HTTP (모놀리스 내부), JWT 재검증 불필요 (Gateway Offloading 통과 후) | 내부 서비스 |
| 외부 API 키 관리 | Azure Key Vault 연동 (External Configuration Store와 통합) | 전체 서비스 |
| 결제 정보 보안 | PCI DSS 준수 결제 게이트웨이 위임, 카드 정보 직접 저장 금지 | PAY |

---

## 논리 아키텍처 다이어그램

다이어그램 파일: `docs/design/logical-architecture.mmd`

주요 표현 요소:
- 클라이언트 → API Gateway: 단일 연결선 (HTTPS)
- 서비스 간 동기 통신: 실선 화살표
- 비동기 이벤트: 점선 화살표
- Cache-Aside 관계: 점선 (데이터 흐름)
- AI Pipeline (Phase 2): 점선 박스로 "Phase 2 도입 예정" 표현
- 외부 시스템: 별도 subgraph

---

*설계 원칙 준수 체크리스트*
- [x] 유저스토리(UFR)와 매칭 확인
- [x] 선정된 10개 클라우드 디자인 패턴 반영
- [x] 서비스 내부 구조 생략, 서비스 간 관계 집중
- [x] 클라이언트에서 API Gateway로 단일 연결선
- [x] AI 서비스 인터페이스 추상화 포함 (Phase 3 대비)
- [x] AI 서비스 폴백 전략 명시 (RuleBasedGenerator Fallback)
- [x] 물리적 배포 구조(컨테이너 배치 등) 이 단계에서 결정하지 않음
- [x] 캐시 무효화 전략 명시
- [x] 충돌 대응 방안(Publisher-Subscriber vs 강한 일관성) 명시
