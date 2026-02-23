# AI 서비스 관점 아키텍처 패턴 평가

> 작성자: 한승우/마법사 (AI 엔지니어)
> 작성일: 2026-02-23
> 참조: 핵심솔루션.md, userstory.md, Cloud Design Patterns(개요).md

---

## AI 활용 현황 및 로드맵

### MVP 단계 AI 활용

| 기능 | 방식 | 비고 |
|------|------|------|
| 총평 생성 (BRIF) | 템플릿 기반 규칙 엔진 | AI API 미사용. "현재까지 모든 항목 정상입니다." 패턴 |
| 상태 배지 판정 (MNTR) | 규칙 기반 임계값 판정 | confidence_score 컬럼 예약 (향후 AI 입력 대비) |
| 대안 카드 정렬 (ALTN) | 합산 점수 규칙 정렬 | 거리 + 카테고리 + 영업 중 필터 |

**MVP의 핵심 AI 전략**: AI 없이 동작하되, 데이터를 쌓는다.
- 상태 판정 이력 6개월 이상 보존 (AI 학습 데이터셋 목적)
- 총평 생성 로직을 외부 호출 가능한 인터페이스로 추상화 (Phase 3 AI 전환 대비)
- 현재 위치, 선택 이력, 방문 패턴을 이벤트로 기록

### Phase 2+ AI 활용

| 기능 | 방식 | 데이터 요구사항 |
|------|------|--------------|
| 대안 카드 가중치 자동 학습 | 사용자 선택 피드백 기반 경량 ML | MVP 누적 선택 이력 |
| AI 기반 총평 생성 | LLM API 연동 (Azure OpenAI) | 수집 데이터 구조화 필요 |
| 날씨 기반 일정 자동 전환 | 규칙 + 경량 ML 앙상블 | 날씨-선택 상관 데이터 |

### Phase 3+ AI 활용

| 기능 | 방식 | 복잡도 |
|------|------|--------|
| AI 자동 일정 재조정 엔진 | LLM + RAG + 사용자 선호 프로파일 | 높음 |
| AI 현지 친구 컨시어지 | 대화형 LLM, 멀티턴 컨텍스트 | 매우 높음 |

---

## 패턴별 AI 관점 평가

| 패턴 | AI 적합성 (1-10) | 적용 시기 | 근거 |
|------|:-:|:-:|------|
| **비동기 통신** (Asynchronous Request-Reply) | **9** | Phase 2 | LLM 응답 지연 평균 2~10초. 브리핑 생성을 동기 블로킹으로 처리하면 Push 알림 타이밍이 틀어진다. MVP에서도 외부 API(Places, Weather) 병렬 호출에 즉시 필요 |
| **Circuit Breaker** | **9** | MVP | 외부 API 4종(Google Places, OpenWeatherMap, Directions, FCM) 중 어느 하나라도 장애 시 브리핑 전체가 실패하면 안 된다. 부분 데이터로도 브리핑을 생성해야 핵심 가치(안심감)가 유지된다 |
| **Cache-Aside** | **9** | MVP | UFR-BRIF-010에서 명시적으로 요구: "브리핑 생성 시 모니터링 서비스의 캐시된 데이터를 Read-Through 방식으로 조회 (온디맨드 외부 API 재호출 아님)". Google Places API 비용이 캐시 없이는 감당 불가 |
| **Rate Limiting** | **8** | MVP | Google Places API 무료 티어 한도 존재. FCM 발송 폭주 방지. Phase 2에서 LLM API 도입 시 토큰 비용 제어가 서비스 생존의 직결 요소 |
| **Retry with Backoff** | **8** | MVP | 외부 API 일시적 오류(5xx, timeout)는 재시도로 해결 가능. 단, 브리핑 트리거가 출발 15~30분 전이므로 재시도 시간창이 좁다 — 최대 3회, 지수 백오프, 총 60초 이내 제한 필요 |
| **Bulkhead** | **7** | Phase 2 | MVP 단계에서는 AI API 미사용이므로 우선순위 낮음. Phase 2에서 LLM 호출 스레드/커넥션 풀을 별도 격리하지 않으면 LLM 지연이 일반 API 응답에 영향을 준다 |
| **Priority Queue** | **7** | MVP | 브리핑 생성 작업은 출발 시간 임박도에 따라 우선순위가 다르다. 30분 전 브리핑이 지연되면 사용자 가치가 0이 된다. Azure Service Bus의 Priority Queue로 임박 순 처리 보장 |
| **Queue-Based Load Leveling** | **7** | MVP | 스케줄러가 15분 주기로 다수 사용자의 상태 수집을 트리거. 동시 다발 API 호출을 큐로 완충하지 않으면 외부 API Rate Limit에 직접 충돌한다 |
| **Scheduler Agent Supervisor** | **6** | MVP | 15분 주기 모니터링, 출발 전 브리핑 트리거 등 스케줄 기반 워크플로우의 실패/재시도/감시 패턴. MVP에서 스케줄러 단일 실패 지점 방지에 유효 |
| **Pipes and Filters** | **6** | Phase 2 | Phase 2 LLM 파이프라인: 데이터 수집 → 정규화 → 프롬프트 구성 → LLM 호출 → 후처리 → 저장 단계를 독립 필터로 분리하면 단계별 교체와 테스트가 쉬워진다 |
| **Claim Check** | **5** | Phase 2 | LLM 응답(JSON 구조체, 추천 사유 텍스트)이 메시지 큐의 크기 제한을 초과할 수 있다. 응답을 Blob Storage에 저장하고 참조키만 메시지로 전달하는 패턴. Phase 2 이상에서 검토 |
| **CQRS** | **5** | Phase 3 | AI 추천 결과는 쓰기(생성)와 읽기(조회) 패턴이 극명히 다르다. Phase 3 자동 재조정 엔진 수준에서 의미 있으며, MVP에서는 오버엔지니어링 |
| **Event Sourcing** | **4** | Phase 3 | 상태 판정 이력을 이벤트로 누적하는 요구사항(UFR-MNTR-020)은 이미 반영됨. 단 완전한 Event Sourcing 아키텍처는 Phase 3 수준의 복잡도 — MVP에서는 이력 테이블 append-only로 충분 |

---

## AI 서비스 특화 권장 사항

### MVP 단계

**1. 외부 API 호출 캐시 계층 필수 구축**

브리핑 생성 경로: 스케줄러 트리거 → 모니터링 캐시 조회 → 템플릿 엔진 → FCM 발송

```
[Scheduler] → [Monitoring Cache (Redis/Azure Cache)]
                    ↑ miss only
             [Google Places API]
             [OpenWeatherMap API]
             [Google Directions API]
```

- 캐시 TTL: Places 영업 상태 5분, 날씨 10분, 혼잡도 15분
- 캐시 미스 시에만 외부 API 호출 (Read-Through)
- 브리핑 생성은 캐시 데이터만 사용, 실시간 API 재호출 금지

**2. Circuit Breaker 적용 대상 및 Fallback 전략**

| API | Fallback | 브리핑 영향 |
|-----|----------|-----------|
| Google Places (영업 상태) | 마지막 캐시값 + "정보 업데이트 지연" 표시 | 회색 배지로 표시 |
| OpenWeatherMap | 마지막 캐시값 사용 | 날씨 항목 "확인 불가" |
| Google Directions (이동시간) | 직선거리 기반 추정값 | 이동시간 "약 N분 (추정)" |
| FCM | 인앱 알림으로 대체 | Push → 인앱 fallback |

Fallback이 있는 한, 브리핑은 부분 데이터로도 반드시 생성한다.
"정보를 못 가져와서 브리핑 없음"은 핵심 가치 훼손이다.

**3. 총평 인터페이스 추상화 (Phase 3 AI 전환 대비)**

```
interface BriefingTextGenerator {
  generate(context: BriefingContext): Promise<BriefingText>
}

// MVP: 규칙 기반 구현
class RuleBasedGenerator implements BriefingTextGenerator { ... }

// Phase 2: LLM 기반 구현
class LLMGenerator implements BriefingTextGenerator { ... }
```

인터페이스 뒤에 구현을 숨겨두면, Phase 2에서 LLM으로 전환할 때 브리핑 서비스 코드를 건드리지 않아도 된다.

**4. AI 학습 데이터 파이프라인 준비**

MVP에서 수집해야 할 데이터:
- 상태 판정 이력 (6개월 이상 append-only 보존)
- 대안 카드 노출 3건 중 사용자 선택 1건 (선택/미선택 레이블)
- 브리핑 열람 여부 및 열람-발송 간격
- Push 알림 수신-탭 간격

이 4가지가 Phase 2 ML 모델의 학습 데이터셋이다. MVP에서 스키마를 잘 잡아야 한다.

### 확장 단계 (Phase 2)

**1. LLM API 비용 제어 3층 구조**

```
Layer 1 - 캐시: 동일 맥락(장소 + 날씨 + 혼잡도 조합) 응답 캐싱
           TTL: 브리핑 유형별 30분
           예상 캐시 히트율: 40~60% (출퇴근/식사 시간대 패턴 반복)

Layer 2 - Rate Limiting: 사용자 티어별 LLM 호출 한도
           Free: 일 1회 (규칙 기반으로 대체)
           Pro: 무제한

Layer 3 - Bulkhead: LLM 호출 전용 커넥션 풀 분리
           일반 API 풀과 격리 → LLM 지연이 Places API 조회에 영향 없음
```

**2. 비동기 LLM 처리 패턴**

LLM 호출은 동기 블로킹으로 처리하지 않는다.

```
[브리핑 트리거]
      ↓
[Azure Service Bus 큐에 작업 등록]
      ↓
[LLM Worker (별도 프로세스)]
      ↓
[결과 저장 → FCM 발송]
```

출발 30분 전 트리거 → 비동기 처리 → 출발 15분 전 전달 목표.
LLM 응답 지연(최대 10초)이 Push 알림 발송 타이밍에 영향을 주지 않는 구조.

**3. Prompt Engineering 가이드라인**

- 시스템 프롬프트에 페르소나 고정: "당신은 여행 가이드입니다. 50자 이내로 총평을 작성하세요."
- 구조화된 JSON 출력 강제 (function calling / structured output)
- 온도(temperature) 0.3 이하: 창의성보다 일관성 우선
- 최대 토큰 100 이하: 총평 텍스트는 짧을수록 좋다

### 고도화 단계 (Phase 3+)

**1. AI 자동 재조정 엔진 아키텍처**

단순 LLM 호출이 아닌, 도구 호출(Tool Use) 패턴으로 설계:

```
[LLM Agent]
    ├─ tool: search_places(category, location, radius)
    ├─ tool: get_travel_time(origin, destination)
    ├─ tool: check_schedule_conflict(schedule, new_place)
    └─ tool: update_schedule(schedule_id, changes)
```

LLM이 직접 일정을 수정하지 않는다. 도구를 통해 시스템이 수정하고, LLM은 추론만 담당한다.
이 구조가 감사 로그, 롤백, 사용자 확인 단계를 유지하면서 AI 자동화를 가능하게 한다.

**2. RAG 기반 개인화**

- 사용자 과거 방문 장소, 선택 패턴, 선호 카테고리를 벡터 DB에 저장
- 대안 카드 생성 시 개인화 컨텍스트를 RAG로 주입
- Azure AI Search (벡터 검색) + Azure Cosmos DB 조합 권장

---

## 외부 API 연동 아키텍처 권장

### API별 장애 대응 전략

| API | 역할 | Circuit Breaker 임계값 | Fallback | 데이터 스탈 허용 TTL |
|-----|------|:--------------------:|----------|:------------------:|
| Google Places API | 영업 상태, 장소 검색 | 5회/1분 실패 | 캐시값 + 회색 배지 | 5분 |
| OpenWeatherMap API | 날씨 데이터 | 3회/1분 실패 | 캐시값 | 10분 |
| Google Directions API | 이동시간 | 3회/1분 실패 | 직선거리 추정 | 30분 |
| FCM | Push 발송 | 10회/1분 실패 | 인앱 알림 | N/A |
| Azure OpenAI (Phase 2) | 총평 생성 | 3회/30초 실패 | 규칙 기반 fallback | N/A |

**핵심 원칙**: 외부 API가 모두 죽어도 앱의 기본 흐름(일정 조회, 배지 표시)은 동작해야 한다.

### 비용 최적화 전략

**Google Places API 비용 절감**

```
호출 우선순위:
1순위: 모니터링 캐시 (Redis) — 비용 0
2순위: 일정 등록 시 선행 조회 결과 — 비용 0
3순위: Google Places API 실시간 호출 — 비용 발생

15분 주기 수집 × 사용자 수 × 일정 장소 수 = 일일 API 호출량
Free Tier 한도(월 200달러 크레딧) 초과 시점을 사용자 수 기준으로 사전 계산 필요
예상 손익분기: 동시 활성 여행자 약 500명 초과 시 유료 전환
```

**캐시 키 설계 (장소 + 시간대 기반)**

```
cache_key = f"place:{place_id}:status:{floor(timestamp/300)}"
# 5분 단위 슬롯으로 키를 고정 → 동일 시간대 요청은 캐시 공유
```

**LLM 비용 추정 및 한도 설정 (Phase 2)**

```
총평 생성 1회 = 약 200 input token + 50 output token
GPT-4o-mini 기준: 약 $0.00015/회
일 10,000 브리핑 기준: 일 $1.5, 월 $45

LLM 비용은 구독 수익으로 커버하는 구조로 설계
Free Tier → 규칙 기반 (LLM 호출 없음)
Pro Tier → LLM 기반 (비용 구독료로 회수)
```

**FCM 비용**

FCM 자체는 무료. 단, 대량 발송 시 Apple APNs 연동 오류율 모니터링 필요.

---

## 마법사의 총평

MVP에서 AI를 쓰지 않는 건 옳은 판단이다. 데이터 없이 AI를 붙이면 그냥 비싸고 느린 규칙 엔진이 된다.

지금 해야 할 일은 세 가지다.

**첫째**, Cache-Aside와 Circuit Breaker를 MVP부터 제대로 만든다. 이건 AI와 무관하게 외부 API 4개를 붙이는 순간 필수다. 여기서 타협하면 나중에 LLM을 붙일 때 더 힘들어진다.

**둘째**, 총평 생성 인터페이스를 추상화한다. 구현 2주면 충분하다. 이걸 안 하면 Phase 2에서 LLM 전환 비용이 3배가 된다.

**셋째**, 데이터를 쌓는다. 상태 판정 이력, 대안 선택 이력, 열람 패턴. 이 데이터가 Phase 2 ML 모델의 원재료다. MVP에서 스키마를 틀리면 나중에 마이그레이션 비용이 나온다.

AI는 데이터가 있을 때 비로소 가치가 있다. 지금은 데이터를 만드는 시간이다.
