# 내부 시퀀스 설계서 (Inner Sequence Diagrams)

> 작성자: 아키 (소프트웨어 아키텍트)
> 작성일: 2026-02-23
> 근거: 외부 시퀀스 + 논리 아키텍처 + 유저스토리

## 개요

마이크로서비스 내부의 레이어별 처리 흐름을 PlantUML로 설계한 5개 시퀀스 다이어그램입니다.
각 서비스-시나리오별로 분리하여 작성되었으며, 외부 시퀀스 설계서와 일치합니다.

## 설계 대상

### 1. BRIF 서비스 (2개)

#### 1-1. `brif-브리핑생성.puml`
- **관련 UFR**: UFR-BRIF-010, 020, 030
- **시나리오**: 출발 15~30분 전 브리핑 자동 생성
- **주요 처리 흐름**:
  1. 스케줄러 트리거
  2. 멱등성 키 확인 (user_id, place_id, DATE(scheduled_departure_time))
  3. 구독 티어 확인 (Free: 1회/일, Trip Pass/Pro: 무제한)
  4. MNTR 캐시에서 최신 상태 조회
  5. BriefingGenerator로 상태 기반 브리핑 생성 (안심/주의)
  6. BriefingRepository에 저장 (멱등성 제약)
  7. Redis 캐시 저장 (TTL 30분)
  8. FCM Push 발송
  9. BriefingGenerated 이벤트 발행

- **참여자**: 
  - 내부: BriefingController, BriefingService, BriefingGenerator, BriefingRepository
  - 외부(E): AUTH, MNTR, FCM

#### 1-2. `brif-브리핑조회.puml`
- **관련 UFR**: UFR-BRIF-050
- **시나리오**: 사용자가 브리핑 열람
- **주요 처리 흐름**:
  1. Redis 캐시 확인 (TTL 30분)
  2. 캐시 미스 시 DB 조회
  3. 결과를 캐시에 저장
  4. 만료 여부 판정 (예정 시간 경과 여부)
  5. 분석 이벤트 기록 (BriefingViewed)

- **참여자**:
  - 내부: BriefingController, BriefingService, BriefingRepository
  - 인프라: Redis Cache, Event Bus

---

### 2. ALTN 서비스 (2개)

#### 2-1. `altn-대안장소검색.puml`
- **관련 UFR**: UFR-ALTN-010, 020, 050
- **시나리오**: 주의/위험 상태 시 대안 장소 검색 파이프라인
- **주요 처리 흐름**:
  1. 구독 티어 확인 (Free: Paywall, Trip Pass/Pro: 검색 진행)
  2. PLCE 호출: 반경 1km 장소 검색
  3. MNTR 호출: 후보 장소 상태 확인 (배지)
  4. ScoringService: 스코어링 계산
     - score = 0.5×거리 + 0.3×평점 + 0.2×혼잡도
     - 상태 필터링 (초록 최우선, 노랑 주의, 회색 미확인, 빨강 제외)
  5. 반경 내 결과 < 3개 시 반경 확장 (2km → 3km)
  6. 구독 티어별 카드 수 제한 (Trip Pass: 3, Pro: 5)
  7. AlternativeRepository에 저장
  8. Redis 캐시 저장 (TTL 10분)

- **성능 목표**: 정상 3초, 최대 5초
- **참여자**:
  - 내부: AlternativeController, AlternativeService, ScoringService, AlternativeRepository
  - 외부(E): AUTH, PLCE, MNTR

#### 2-2. `altn-대안카드선택.puml`
- **관련 UFR**: UFR-ALTN-030, 040
- **시나리오 1**: 대안 카드 선택 (채택)
  1. 카드 정보 조회
  2. 채택 기록 저장 (alternative_adoption_log)
  3. AlternativeAdopted 이벤트 발행
     - 구독자: SCHD (일정 교체) → MNTR (모니터링 변경)
  4. 캐시 무효화

- **시나리오 2**: 대안 미선택 (닫기)
  1. 미채택 기록 저장 (alternative_dismissal_log)
  2. 분석 데이터 수집 (카드 수, 미선택 사유, 경과 시간)
  3. 캐시 무효화
  4. 일정은 변경 없음

- **참여자**:
  - 내부: AlternativeController, AlternativeService, AlternativeRepository
  - 인프라: Redis Cache, Event Bus

---

### 3. PAY 서비스 (1개)

#### 3-1. `pay-구독구매.puml`
- **관련 UFR**: UFR-PAY-010
- **시나리오**: 인앱 결제 구독 구매
- **주요 처리 흐름**:
  1. 플랫폼별 영수증 검증 분기
     - iOS: Apple IAP verifyReceipt() API
     - Android: Google Play Billing verifyPurchase() API
  2. 검증 성공/실패 분기
  3. 구독 상태 DB 저장 (subscription 테이블)
  4. product_id → tier 변환 (TRIP_PASS / PRO)
  5. AUTH 서비스 호출: 토큰 재발급 (tier 클레임 업데이트)
  6. 캐시 갱신
  7. 활성화된 기능 목록 반환

- **구독 상태**: ACTIVE, PAUSED, CANCELLED, EXPIRED
- **활성화 기능**:
  - Trip Pass: 대안 카드 3개, 무제한 브리핑
  - Pro: 대안 카드 5개, 무제한 브리핑, 우선 조회

- **참여자**:
  - 내부: PaymentController, SubscriptionService, PlatformVerifier, SubscriptionRepository
  - 외부(E): AUTH, Apple IAP, Google Play Billing

---

## 설계 원칙

### 1. 유저스토리 매칭
- 각 다이어그램은 대응하는 UFR과 매칭
- 불필요한 추가 설계 금지
- 외부 시퀀스와 일치 검증

### 2. 마이크로서비스 내부 표현
- **API 레이어**: Controller (스케줄러/사용자 요청 수신)
- **비즈니스 레이어**: Service (핵심 로직)
- **데이터 레이어**: Repository (CRUD, 영속성)
- **인프라 레이어**: Cache, Event Bus, External API

### 3. 통신 패턴
- **동기**: `->` (즉시 응답 필요)
- **비동기**: `->>` (후처리, 이벤트)
- **외부 시스템**: "(E)" 표기

### 4. 처리 구간 표시
- `activate`/`deactivate`: 각 참여자의 활성/비활성 구간
- 멱등성, 캐시, 이벤트 처리 명시

### 5. 표현 요소
- 요청/응답: 한글 설명
- Repository CRUD: 한글 설명 (SQL 제외)
- 분기: alt/else로 선택 경로 표현
- 노트: 주요 로직, 성능, 스키마 설명

---

## 통합 검증 결과

### PlantUML 문법 검증
✓ brif-브리핑생성.puml: Syntax OK
✓ brif-브리핑조회.puml: Syntax OK
✓ altn-대안장소검색.puml: Syntax OK
✓ altn-대안카드선택.puml: Syntax OK
✓ pay-구독구매.puml: Syntax OK

### 외부 시퀀스와의 일관성
- 모든 다이어그램이 외부 시퀀스 설계서의 플로우를 따름
- 서비스 간 이벤트 발행/구독 명시
- 캐시 및 DB 처리 상세히 표현

---

## 관련 문서

- 외부 시퀀스: `docs/design/sequence/outer/`
  - 07-출발전브리핑.puml
  - 09-대안장소검색.puml
  - 10-대안카드선택및일정반영.puml
  - 11-구독결제.puml

- 논리 아키텍처: `docs/design/logical-architecture.md`
- 유저스토리: `docs/plan/design/userstory.md`

---

## 다음 단계

1. ✓ 내부 시퀀스 설계 완료 (5개)
2. 다른 서비스 내부 시퀀스 설계 (AUTH, SCHD, PLCE, MNTR) - 기존 파일 참조
3. API 스펙 정의 (OpenAPI 3.0)
4. 클래스/도메인 모델 설계
5. 데이터베이스 스키마 최종화

---

