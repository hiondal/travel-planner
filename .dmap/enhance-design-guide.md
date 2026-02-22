# NPD 플러그인 설계 가이드 개선 계획

> 분석 대상: `resources/guides/design/` 하위 13개 가이드 파일 (v0.2.1)
> 분석 일시: 2026-02-23
> 분석 방법: 3개 병렬 Analyst 에이전트를 활용한 심층 분석 (v0.1.4 vs v0.2.1 비교 포함)

---

## 1. 분석 대상 파일 목록

| # | 파일명 | 크기 | 설계 단계 |
|---|--------|------|-----------|
| 1 | common-principles.md | 3.1KB | 공통 |
| 2 | architecture-patterns.md | 15.4KB | Step 1 |
| 3 | logical-architecture-design.md | 4.8KB | Step 2 |
| 4 | sequence-outer-design.md | 3.5KB | Step 3 |
| 5 | sequence-inner-design.md | 5.6KB | Step 3 |
| 6 | api-design.md | 6.0KB | Step 4 |
| 7 | class-design.md | 4.6KB | Step 5 |
| 8 | data-design.md | 5.1KB | Step 6 |
| 9 | frontend-design.md | 4.4KB | Step 7 |
| 10 | physical-architecture-design.md | 25.1KB | Step 8 |
| 11 | uiux-design.md | 2.6KB | Plan 단계 |
| 12 | uiux-prototype.md | 4.2KB | Plan 단계 |
| 13 | architecture-highlevel.md | 5.9KB | Step 10 |

---

## 2. v0.1.4 -> v0.2.1 주요 변화 요약

### 2.1 긍정적 변화 (유지해야 할 개선)

| 변화 | 상세 |
|------|------|
| **문서 구조 표준화** | 모든 가이드가 `목적 > 입력 > 출력 > 방법론 > 출력 형식 > 품질 기준 > 주의사항` 표준 포맷으로 통일 |
| **입출력 체인 명시** | 각 가이드에 입력/출력 산출물 테이블 추가로 파이프라인 연결이 명확해짐 |
| **품질 기준 체크리스트** | 모든 가이드에 완료 체크리스트가 추가되어 검증 기준이 명시됨 |
| **로컬 샘플 참조** | v0.1.4의 외부 GitHub 링크가 `{PLUGIN_DIR}/resources/samples/` 로컬 참조로 변경 |
| **이모지 제거** | 문서 톤이 전문적으로 통일됨 |
| **`{PLUGIN_DIR}` 도입** | 플러그인 디렉토리 경로 변수화로 이식성 향상 |

### 2.2 부정적 변화 (복원 또는 보완 필요)

| 변화 | 영향 |
|------|------|
| **common-principles.md 대폭 축소** (198줄 -> 101줄) | YAGNI, 점진적 구현, 마이크로서비스 설계 원칙이 소실됨. 개별 가이드로 이관되었는지 미확인 |
| **SKILL.md MUST/MUST NOT 규칙 제거** | `..>` 화살표 사용 금지 등 구체적 제약이 v0.2.1 SKILL.md에서 사라짐 |
| **UI/UX 설계서 참조 제거** | architecture-patterns, logical-architecture 입력에서 UI/UX 설계서 참조가 삭제됨. 의도적 변경인지 미확인 |

---

## 3. 발견된 이슈 분류

### 3.1 Critical 이슈 (즉시 수정 필요)

#### C-01. 전체 가이드 경로 불일치

**현상**: 모든 가이드에서 출력 테이블 경로와 본문/출력형식 경로가 불일치함.

| 가이드 | 출력 테이블 | 본문/출력형식 | 불일치 |
|--------|-----------|--------------|--------|
| api-design | `docs/design/api/` | `design/backend/api/` | `docs/` 접두사 + `backend/` 중간경로 |
| class-design | `docs/design/class/` | `design/backend/class/` | 동일 패턴 |
| data-design | `docs/design/database/` | `design/backend/database/` | 동일 패턴 |
| frontend-design | `docs/design/frontend/` | `design/frontend/` | `docs/` 접두사 |
| uiux-design | - | `design/uiux/` | SKILL.md는 `docs/plan/design/uiux/` |
| uiux-prototype | - | `design/uiux/prototype/` | SKILL.md는 `docs/plan/design/uiux/prototype/` |

**영향**: AI 에이전트가 파일 생성 시 어느 경로를 사용해야 하는지 판단 불가. 다음 단계 입력 경로와 맞지 않아 파이프라인 체인이 끊어짐.

**수정 방안**: `docs/design/{category}/`를 정규 경로로 확정하고, 모든 가이드의 출력 테이블과 본문 경로를 일괄 통일.

#### C-02. 내부 시퀀스 입력 테이블에 외부 시퀀스 누락

**현상**: `sequence-inner-design.md` 입력 테이블에 `docs/design/sequence/outer/*.puml`이 누락됨.

**근거**: 작성 원칙에 "외부 시퀀스 설계서에서 설계한 플로우와 일치해야 함"을 요구하고, 준비 단계에서 "외부 시퀀스설계서 분석 및 이해"를 명시하면서 입력 테이블에는 경로가 없음.

**수정 방안**: 입력 테이블에 외부 시퀀스 설계서 행을 추가.

```markdown
| 외부 시퀀스 설계서 | `docs/design/sequence/outer/*.puml` | 외부 플로우 일치 확인 |
```

#### C-03. 물리 아키텍처(Step 8) -> HighLevel 아키텍처(Step 10) 순환 의존

**현상**: `physical-architecture-design.md`가 "HighLevel아키텍처정의서에 선정한 제품으로 구성"을 요구하지만, HighLevel은 Step 10에서 작성되므로 Step 8 시점에 아직 존재하지 않음.

**영향**: 에이전트가 실행 시 존재하지 않는 산출물을 참조하게 되어 실행 불가.

**수정 방안**: Step 8에서 실제로 참조해야 하는 입력을 `docs/design/architecture.md` (Step 1 산출물)의 기술스택 섹션으로 변경. HighLevel 아키텍처 정의서와의 "일치 확인"은 Step 10 완료 후 검증 단계로 이동.

#### C-04. 화살표 표기법 의미 불일치

**현상**: 동일한 PlantUML 문법이 가이드별로 다른 의미로 사용됨.

| 표기법 | 논리 아키텍처 | 시퀀스 다이어그램 |
|--------|-------------|-----------------|
| `-->` | 선택적 의존성 | 비동기 응답 |
| `->` | 동기 의존성 | 동기 호출 |
| `->>` | 비동기 의존성 | (미사용) |

**수정 방안**: `common-principles.md`에 다이어그램 유형별 화살표 의미 표준표를 추가.

#### C-05. uiux-design.md에 출력 형식 템플릿 및 예시 부재

**현상**: 13개 가이드 중 유일하게 출력 형식 템플릿과 예시가 모두 없음. "Markdown 형식"이라는 1줄만 존재.

**영향**: 에이전트마다 다른 포맷으로 UI/UX 설계서를 생성하여 품질 편차가 극심함.

**수정 방안**: 마크다운 템플릿 골격(화면 목록, 화면별 상세, UI 구성요소 테이블, 플로우 다이어그램)과 최소 1개 예시를 추가.

---

### 3.2 High 이슈 (다음 릴리즈에서 수정)

#### H-01. `{PLUGIN_DIR}` 변수 해석 메커니즘 미문서화

**현상**: 모든 가이드에서 `{PLUGIN_DIR}` 변수를 사용하지만, 런타임 치환 메커니즘이 개별 가이드 문서에 설명되지 않음. SKILL.md에 해석 규칙이 있으나 가이드만 단독으로 참조하는 에이전트는 해석 불가.

**수정 방안**: `common-principles.md` 상단에 변수 해석 규칙과 절대 경로 예시를 추가.

#### H-02. `{CLOUD}` 플레이스홀더 결정 소스 미정의

**현상**: `architecture-highlevel.md`, `physical-architecture-design.md`에서 `{CLOUD}` 변수를 사용하지만, 값의 출처(CLAUDE.md? 별도 설정? 사용자 입력?)가 명시되지 않음.

**수정 방안**: SKILL.md 또는 `common-principles.md`에 `{CLOUD}` 결정 메커니즘 명시. 예: "CLAUDE.md의 기술스택 섹션에서 클라우드 제공자를 읽어 바인딩."

#### H-03. 시나리오/플로우 명명 규칙 혼동

**현상**: `sequence-inner-design.md`에서 "케밥-케이스 사용, 한글로 작성 (예: 사용자 등록)"이라 했으나, "사용자 등록"은 케밥-케이스가 아님. 한글에 케밥-케이스를 적용하는 방법이 불명확.

**수정 방안**: 구체적 파일명 예시를 3개 이상 제공.
```
auth-소셜로그인.puml
schd-여행일정생성.puml
mntr-외부데이터수집.puml
```

#### H-04. 아키텍처 패턴 참조 경로 미정의 (class-design)

**현상**: `class-design.md`에서 `{설계 아키텍처 패턴}`을 참조하라고 하면서 출처 문서 경로가 없음.

**수정 방안**: `docs/design/architecture.md`의 구체적 섹션을 참조 경로로 명시.

#### H-05. 외부 시퀀스의 "핵심 비즈니스 플로우" 정의 부재

**현상**: 유저스토리와 플로우의 관계(1:1, N:1, 1:N)가 미정의. 에이전트가 파일 분리 방법을 결정할 수 없음.

**수정 방안**: 플로우 분류 기준을 추가. 예: "하나의 완전한 사용자 시나리오(1개 이상의 유저스토리 그룹)를 1 플로우로 구성. 이 때 하나의 플로우에 포함되는 UFR은 동일한 사용자 세션 내에서 연속적으로 수행되는 것들."

#### H-06. architecture-highlevel.md 출력 형식 템플릿 섹션 누락

**현상**: 방법론에서 16개 섹션의 작성 가이드를 제공하지만, 출력 형식 템플릿에는 섹션 1, 4, 5, 6, 7, 13, 16만 포함. 2, 3, 8~12, 14, 15 섹션이 누락.

**수정 방안**: 누락된 9개 섹션을 출력 형식 템플릿에 추가.

#### H-07. DBMS 종류 미명시 (data-design)

**현상**: `.psql` 확장자로 PostgreSQL을 암시하지만 명시적 선언이 없음.

**수정 방안**: 가이드 방법론 섹션에 "기본 DBMS: PostgreSQL. 프로젝트별 다른 DBMS 사용 시 스키마 문법을 해당 DBMS에 맞게 조정." 명시.

#### H-08. 외부 GitHub URL의 내부 번들링 필요 (physical-architecture)

**현상**: 7개 이상의 외부 GitHub URL이 예시로 참조됨. 네트워크 불가 환경에서 에이전트가 핵심 참조를 잃음.

**수정 방안**: 핵심 예시를 `resources/samples/` 디렉토리에 번들하거나, 접근 실패 시 fallback 지침 추가.

#### H-09. 테스트 시나리오 포맷 미정의 (api-design)

**현상**: "테스트 시나리오 포함"을 요구하면서 구체적 포맷(OpenAPI 확장 필드? 별도 파일?)이 없음.

**수정 방안**: 테스트 시나리오 형식 예시를 제공. 예: `x-test-cases` 확장 필드 형식 또는 별도 `.test.yaml` 파일 형식.

#### H-10. 패턴 평가 채점 기준선(baseline) 미정의 (architecture-patterns)

**현상**: "응답시간 50%+ 개선 예상"(성능 효과 9-10점)에서 기준선이 없음. "기존 대비"의 "기존"이 미정의.

**수정 방안**: "산업 평균 대비" 또는 "동일 아키텍처 패턴 미적용 시 대비" 등 기준선을 명확히 정의.

---

### 3.3 Medium 이슈 (개선 권장)

| ID | 이슈 | 영향 가이드 |
|----|------|------------|
| M-01 | "스쿼드 팀원 리뷰" 단계의 AI 에이전트 맥락에서 실행 방법 미정의 | 전체 |
| M-02 | 에러/예외 흐름 표현 가이드 부재 (alt/else/opt 프래그먼트) | sequence-outer, sequence-inner |
| M-03 | "프론트엔드에서 할 수 있는 것"의 판단 기준 미정의 | sequence-inner |
| M-04 | UI 프레임워크 선택 기준 부재 (5개 옵션 나열만) | frontend-design |
| M-05 | 프로토타입 형식 정의 부재 (HTML/SVG/PNG/Figma?) | frontend-design |
| M-06 | 캐시 대상 선정 기준 미정의 (조회 빈도? 변경 빈도?) | data-design |
| M-07 | TTL 정책 가이드라인 부재 | data-design |
| M-08 | ERD PlantUML 문법/유형 미정의 | data-design |
| M-09 | Clean 아키텍처 용어 매핑표 부재 | class-design |
| M-10 | 공통 컴포넌트(common-base) 결정 기준 미정의 | class-design |
| M-11 | Playwright MCP 테스트 시나리오 구체화 필요 | uiux-prototype |
| M-12 | 프로젝트 프로파일 판단 기준 구체화 필요 | architecture-patterns |
| M-13 | 의존성 분석 예시의 도메인 특화 (범용성 부족) | common-principles |
| M-14 | servers URL의 `{org}` 값 결정 기준 미정의 | api-design |
| M-15 | v0.1.4 유용 원칙(YAGNI, 점진적 구현) 복원 검토 | common-principles |
| M-16 | 가이드 소속 스킬 불일치 (uiux 가이드가 design 디렉토리에 위치) | uiux-design, uiux-prototype |
| M-17 | 운영환경 가이드에 Azure 특화 용어 혼재 (클라우드 중립성 부족) | physical-architecture |
| M-18 | 번호 체계 불일치 (대분류 연번 vs 섹션 번호 혼재) | physical-architecture |

### 3.4 Low 이슈 (향후 개선)

| ID | 이슈 | 영향 가이드 |
|----|------|------------|
| L-01 | 소규모 프로젝트(서비스 2개 이하) 대응 가이드 부재 | 전체 |
| L-02 | 다이어그램 복잡도 상한(참여자/메시지 수 제한) 미정의 | sequence-outer, sequence-inner |
| L-03 | 산출물 수량 범위(플로우/시나리오 수 권장) 미정의 | sequence-outer, sequence-inner |
| L-04 | 패턴 평가 후 최종 선정 수 권장 범위 미정의 | architecture-patterns |
| L-05 | 공통 에러 응답 표준 (RFC 7807) 가이드 부재 | api-design |
| L-06 | 인덱스 설계 가이드 부재 | data-design |
| L-07 | 프론트엔드 상태 관리 전략 가이드 부재 | frontend-design |
| L-08 | 프론트엔드 테스트 방법론 가이드 부재 | frontend-design |
| L-09 | Redis database 14개 초과 서비스 시 전환 기준 미정의 | data-design |
| L-10 | 한글 파일명의 OS/Git 호환성 검증 미수행 | sequence-inner |

---

## 4. 개선 실행 계획

### Phase 1: Critical 수정 (즉시)

| 작업 | 대상 파일 | 예상 작업량 |
|------|----------|-----------|
| 경로 통일 (C-01) | 전체 13개 파일 | 일괄 치환 |
| 외부 시퀀스 입력 추가 (C-02) | sequence-inner-design.md | 1줄 추가 |
| 순환 의존 해소 (C-03) | physical-architecture-design.md | 참조 경로 변경 |
| 화살표 표준표 추가 (C-04) | common-principles.md | 테이블 1개 추가 |
| 출력 형식 템플릿 추가 (C-05) | uiux-design.md | 마크다운 골격 + 예시 추가 |

### Phase 2: High 수정 (다음 릴리즈)

| 작업 | 대상 파일 | 예상 작업량 |
|------|----------|-----------|
| `{PLUGIN_DIR}` 해석 문서화 (H-01) | common-principles.md | 섹션 추가 |
| `{CLOUD}` 결정 메커니즘 (H-02) | common-principles.md 또는 SKILL.md | 섹션 추가 |
| 명명 규칙 구체화 (H-03) | sequence-inner-design.md | 예시 추가 |
| 아키텍처 패턴 참조 경로 (H-04) | class-design.md | 1줄 수정 |
| 플로우 분류 기준 (H-05) | sequence-outer-design.md | 가이드 추가 |
| 템플릿 섹션 보완 (H-06) | architecture-highlevel.md | 9개 섹션 추가 |
| DBMS 명시 (H-07) | data-design.md | 1줄 추가 |
| 예시 내부 번들링 (H-08) | physical-architecture-design.md + samples/ | 파일 추가 |
| 테스트 시나리오 포맷 (H-09) | api-design.md | 예시 추가 |
| 채점 기준선 정의 (H-10) | architecture-patterns.md | 문구 수정 |

### Phase 3: Medium 수정 (점진적 개선)

- M-01 ~ M-18 이슈를 우선순위에 따라 순차 적용
- 각 가이드 파일 수정 시 관련 Medium 이슈를 함께 반영

### Phase 4: Low 수정 (백로그)

- L-01 ~ L-10 이슈는 사용자 피드백 기반으로 필요 시 반영

---

## 5. 미해결 질문 (Open Questions)

다음 질문들은 플러그인 관리자(또는 PO)의 판단이 필요한 사항입니다:

| # | 질문 | 영향 범위 |
|---|------|----------|
| Q-01 | v0.2.1에서 UI/UX 설계서 참조가 architecture-patterns, logical-architecture 입력에서 제거된 것은 의도적 변경인가? | architecture-patterns, logical-architecture |
| Q-02 | `{PLUGIN_DIR}` 변수는 NPD 런타임이 자동 치환하는가, 에이전트가 수동 경로 조합을 해야 하는가? | 전체 |
| Q-03 | Docker가 없는 환경에서 PlantUML/Mermaid 검증의 fallback 절차는? | common-principles |
| Q-04 | "스쿼드 팀원 리뷰"는 AI 에이전트 맥락에서 자동 리뷰인가, 사람 승인 대기인가? | 전체 |
| Q-05 | v0.1.4 SKILL.md의 MUST/MUST NOT 규칙 제거는 의도적인가, 각 가이드로 이관된 것인가? | SKILL.md |
| Q-06 | 정규 파일 경로 체계가 `docs/design/` 인지 `design/backend/` 인지 확정 필요 | 전체 |
| Q-07 | `{CLOUD}` 값은 CLAUDE.md에서 오는가, 별도 프로젝트 설정에서 오는가? | physical-architecture, architecture-highlevel |
| Q-08 | uiux-design.md, uiux-prototype.md의 소속 스킬이 `/npd:plan`인가 `/npd:design`인가? | uiux-design, uiux-prototype |
| Q-09 | 프로토타입과 API 명세서 간 충돌 시 어느 쪽이 우선인가? | frontend-design |
| Q-10 | 패턴 평가 매트릭스에서 최소 채택 임계값(총점 기준)이 있는가? | architecture-patterns |

---

## 6. 가이드별 품질 평가 요약

| 가이드 | 구조 | 명확성 | 예시 | 실행가능성 | 연계성 | 종합 |
|--------|------|--------|------|-----------|--------|------|
| common-principles | B | B | C | B | B | **B** |
| architecture-patterns | A | A | A | A | A | **A** |
| logical-architecture | B | B | C | B | B | **B** |
| sequence-outer | B | B | C | B | B | **B** |
| sequence-inner | B | B | C | B | C | **B-** |
| api-design | A | B | B | A | B | **B+** |
| class-design | B | B | C | B | C | **B-** |
| data-design | A | A | B | B | B | **B+** |
| frontend-design | B | C | C | C | B | **C+** |
| physical-architecture | A | A | B | A | B | **A-** |
| uiux-design | B | D | D | D | C | **D+** |
| uiux-prototype | A | B | B | B | B | **B+** |
| architecture-highlevel | B | B | C | C | B | **B-** |

**등급 기준**: A(우수) B(양호) C(보통) D(미흡) F(불가)

### 핵심 발견

1. **최우수 가이드**: `architecture-patterns.md` - 정량 평가 체계, 구체적 예시, 프로파일별 가중치 등이 체계적
2. **최약 가이드**: `uiux-design.md` - 출력 형식 템플릿/예시 부재로 AI 에이전트 자율 실행이 사실상 불가
3. **가장 큰 가이드**: `physical-architecture-design.md` (25.1KB) - 90+ 항목으로 가장 상세하나 단일 에이전트 컨텍스트 초과 위험

---

## 7. 전체 가이드 체계 입출력 체인

```
[Plan 단계]
  userstory.md ─────────────────────────────────────────────┐
  이벤트 스토밍(es/*.puml) ────────────────────────────────┐ │
  핵심 솔루션(핵심솔루션.md) ─────────────────────────────┐ │ │
  Cloud Design Patterns 개요 ────────────────────────────┐ │ │ │
                                                          │ │ │ │
[Step 1] architecture-patterns.md ←───────────────────────┘─┘─┘─┘
    출력: docs/design/architecture.md                      │
                                                           │
[Step 2] logical-architecture-design.md ←──────────────────┘
    출력: docs/design/logical-architecture.md              │
           docs/design/logical-architecture.mmd            │
                                                           │
[Step 3] sequence-outer-design.md ←────────────────────────┘
    출력: docs/design/sequence/outer/*.puml                │
                                                           │
[Step 3] sequence-inner-design.md ←────────────────────────┘
    출력: docs/design/sequence/inner/*.puml                │
                                                           │
[Step 4] api-design.md ←───────────────────────────────────┘
    출력: docs/design/api/*.yaml                           │
                                                           │
[Step 5] class-design.md ←─────────────────────────────────┘
    출력: docs/design/class/*.puml                         │
                                                           │
[Step 6] data-design.md ←──────────────────────────────────┘
    출력: docs/design/database/*.puml, *.psql              │
                                                           │
[Step 7] frontend-design.md ←──── (API 설계서) ────────────┘
    출력: docs/design/frontend/*.md

[Step 8] physical-architecture-design.md ←── (패턴+논리+데이터)
    출력: docs/design/physical/*.md, *.mmd

[Step 10] architecture-highlevel.md ←── (전체 설계 산출물)
    출력: docs/design/high-level-architecture.md
```

---

## 8. 권장 우선순위별 실행 로드맵

```
Week 1 (즉시)
├── C-01: 전체 경로 통일 (일괄 sed/replace)
├── C-02: 내부 시퀀스 입력 테이블 보완
├── C-03: 순환 의존 해소
├── C-04: 화살표 표준표 추가
└── C-05: uiux-design.md 템플릿 추가

Week 2-3 (High 이슈)
├── H-01 ~ H-03: 변수 해석/명명 규칙 정리
├── H-04 ~ H-05: 참조 경로/분류 기준
├── H-06 ~ H-07: 템플릿 보완/DBMS 명시
└── H-08 ~ H-10: 예시 번들링/포맷 정의

Week 4+ (Medium/Low)
├── M-01 ~ M-18: 점진적 개선
└── L-01 ~ L-10: 백로그 관리
```

---

## 부록 A: 버전별 주요 차이 매트릭스

| 가이드 | 라인수 변화 | 주요 추가 | 주요 삭제 |
|--------|-----------|----------|----------|
| common-principles | 198 -> 101 (-49%) | Mermaid 검사, .temp 규칙 | 핵심원칙 6개, 도메인별 표준 |
| architecture-patterns | 169 -> 327 (+93%) | 스크리닝, rubric, 조합검증 | UI/UX 참조 |
| logical-architecture | 65 -> 137 (+111%) | 출력 형식, 품질 기준 | UI/UX 참조, 외부 링크 |
| sequence-outer | 51 -> 90 (+76%) | 출력 형식, 품질 기준 | @analyze 명령 |
| sequence-inner | 79 -> 131 (+66%) | 출력 형식, 품질 기준 | - |
| api-design | - -> 6.0KB | 입출력 테이블, 품질 기준 | - |
| class-design | - -> 4.6KB | 패키지구조도, 이벤트스토밍 입력 | - |
| data-design | - -> 5.1KB | 출력 형식, 품질 기준 | - |
| frontend-design | - -> 4.4KB | 입출력 테이블, 품질 기준 | (내용 동일) |
| physical-architecture | - -> 25.1KB | 입출력 테이블, 품질 기준 | (내용 동일) |
| uiux-design | - -> 2.6KB | 품질 기준 | - |
| uiux-prototype | - -> 4.2KB | 품질 기준 | - |
| architecture-highlevel | - -> 5.9KB | 템플릿, 품질 기준 | 참고자료 직접 나열 |
