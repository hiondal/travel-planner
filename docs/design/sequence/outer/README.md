# 외부 시퀀스 다이어그램 (Outer Sequence Diagrams)

> 작성자: 아키 (소프트웨어 아키텍트)
> 작성일: 2026-02-23
> 관련 문서: `docs/design/logical-architecture.md`, `docs/plan/design/userstory.md`

## 개요

외부 시퀀스 다이어그램은 프론트엔드에서 시작하여 API Gateway를 거쳐 각 마이크로서비스, 외부 API, 인프라 컴포넌트(캐시, 이벤트 버스)까지 포함한 **End-to-End 흐름**을 나타냅니다.

설계 원칙:
- 유저스토리(UFR)와 매칭하여 불필요한 추가 설계 제외
- 논리 아키텍처에 정의한 참여자만 사용
- 마이크로서비스 내부 처리 흐름은 제외
- 동기 통신: 실선 화살표 (`->`, `-->`)
- 비동기 통신: `->>` 또는 `-->>` (Event Bus 경유)
- 한글 설명으로 작성

## 4개 설계 파일

### 1. 01-소셜로그인.puml
**관련 유저스토리:** UFR-AUTH-010
**참여자:** 여행자, 프론트엔드, API Gateway, AUTH 서비스, Google OAuth, Apple Sign In

**흐름:**
- Google 로그인 분기: OAuth 인증 → Authorization Code 검증 → JWT 발급
- Apple 로그인 분기: Sign In with Apple → Identity Token 검증 → JWT 발급
- 실패 분기: 인증 실패 또는 네트워크 오류 처리
- 체크리스트:
  - Google/Apple OAuth 2.0 지원
  - JWT Access Token(30분) + Refresh Token(7일) 발급
  - 닉네임, 프로필 이미지 자동 설정
  - 에러 메시지: 인증 실패 또는 네트워크 오류 안내

---

### 2. 02-온보딩.puml
**관련 유저스토리:** UFR-SCHD-005
**참여자:** 여행자, 프론트엔드, API Gateway, SCHD 서비스

**흐름:**
- 온보딩 미완료 경로:
  - 3단계 온보딩 표시 (상태 배지 → 출발 전 브리핑 → 대안 카드)
  - 샘플 여행 일정 생성 (trip_id 발급)
  - 샘플 장소 3개 자동 추가 + 모니터링 대상 등록
  - 온보딩 상태 저장 (로컬 저장소)
- 온보딩 완료 경로:
  - 메인 화면으로 이동 (온보딩 스킵)
- 체크리스트:
  - 3단계 온보딩은 클라이언트에서 처리
  - 샘플 일정은 MonitoringTargetRegistered 이벤트 발행
  - 온보딩 상태는 로컬 저장소에 저장 (서버 필수 아님)

---

### 3. 03-여행일정등록.puml
**관련 유저스토리:** UFR-SCHD-010, UFR-SCHD-020, UFR-SCHD-030, UFR-SCHD-010 시나리오2 (권한 동의)
**참여자:** 여행자, 프론트엔드, API Gateway, SCHD 서비스, PLCE 서비스, MNTR 서비스, Redis Cache, Event Bus, Google Places API

**흐름:**
1. 첫 여행 등록 시 권한 동의 (위치정보, Push 알림)
2. 여행 생성 (trip_id 발급)
3. 장소 검색:
   - 검색어 입력 → Google Places API 호출
   - 캐시 확인 (5분 TTL)
   - 검색 결과 반환
4. 장소 선택 및 방문 일시 입력:
   - 영업시간 내 검증
   - 일정 생성 (schedule_id 발급)
5. 모니터링 대상 등록:
   - MonitoringTargetRegistered 이벤트 발행 (Event Bus)
   - MNTR 서비스 수신 → 모니터링 대상 등록
- 체크리스트:
  - 권한 동의는 클라이언트에서 처리 (동의 결과만 서버 전송)
  - 장소 데이터는 캐시로 관리 (Cache-Aside 패턴)
  - 이벤트는 비동기로 발행하여 느슨한 결합 유지
  - MNTR이 이벤트 구독하여 모니터링 시작

---

### 4. 04-일정표조회.puml
**관련 유저스토리:** UFR-SCHD-050 (배지 포함 일정표 조회)
**참여자:** 여행자, 프론트엔드, API Gateway, SCHD 서비스, MNTR 서비스, Redis Cache

**흐름:**
1. 일정표 조회 요청 (JWT 검증)
2. SCHD 서비스에서 일정 목록 조회
3. MNTR 서비스에서 배지 상태 조회:
   - 캐시 히트 → 캐시 데이터 반환 (5분 TTL)
   - 캐시 미스 → DB 조회 → 캐시 저장
4. 일정표 UI 렌더링:
   - 시간순 정렬
   - 각 장소명 옆 배지 표시 (색상 + 아이콘)
   - 색약 사용자 지원 (색상만 의존 X)
   - 배지 상태: 초록(안심), 노랑(주의), 빨강(위험), 회색(미확인)
- 체크리스트:
  - 배지 데이터는 캐시 기반 조회 (5분 TTL)
  - 색상 + 아이콘으로 WCAG 2.1 접근성 지원
  - 회색 배지에 "데이터 미확인" 라벨 표시

---

## 설계 검증 체크리스트

### 1. 유저스토리 매칭
- [x] UFR-AUTH-010 (소셜 로그인) → 01-소셜로그인.puml
- [x] UFR-SCHD-005 (온보딩) → 02-온보딩.puml
- [x] UFR-SCHD-010/020/030 (여행일정등록) → 03-여행일정등록.puml
- [x] UFR-SCHD-050 (일정표조회) → 04-일정표조회.puml

### 2. 논리 아키텍처 정의 확인
- [x] API Gateway: 단일 진입점, JWT 검증
- [x] AUTH 서비스: 소셜 로그인, JWT 발급
- [x] SCHD 서비스: 여행/일정/장소 CRUD
- [x] PLCE 서비스: 장소 검색, 캐싱
- [x] MNTR 서비스: 모니터링 대상 관리, 배지 상태 조회
- [x] Redis Cache: 장소 데이터, 배지 상태 캐싱
- [x] Event Bus: 비동기 이벤트 통신 (MonitoringTargetRegistered)

### 3. 통신 방식
- [x] 동기: 실선 화살표 (`->`, `-->`)
- [x] 비동기: `->>` (Event Bus 경유)
- [x] 응답 표시: `-->` (점선)

### 4. 플로우 완성도
- [x] End-to-End 흐름: 프론트엔드 → 게이트웨이 → 서비스 → 외부 API → 결과 반환
- [x] 에러 처리: 실패 케이스 포함 (로그인 실패, 권한 거부 등)
- [x] 캐시 동작: Cache-Aside 패턴 표현
- [x] 이벤트 발행: Event Bus 거쳐 구독 서비스 처리

### 5. PlantUML 문법
- [x] 모든 파일이 PlantUML 문법 검사 통과
- [x] `!theme mono` 사용
- [x] 한글 설명 포함
- [x] `@startuml` / `@enduml` 포함

---

## 추가 설계 파일 (참고용)

다음 파일들은 전체 흐름을 이해하기 위한 추가 설계 파일입니다:
- 05-외부데이터수집.puml: UFR-MNTR-010
- 06-상태배지조회.puml: UFR-MNTR-030
- 07-출발전브리핑.puml: UFR-BRIF-010/040/050
- 08-상태변경알림.puml: UFR-MNTR-050
- 09-대안장소검색.puml: UFR-ALTN-010
- 10-대안카드선택및일정반영.puml: UFR-ALTN-030, UFR-SCHD-040
- 11-구독결제.puml: UFR-PAY-010

---

## PlantUML 렌더링 방법

온라인 PlantUML 에디터에서 확인:
```
https://www.plantuml.com/plantuml/uml/
```

로컬 렌더링 (Docker 필요):
```bash
docker run --rm -v $(pwd):/docs plantuml/plantuml:latest -o /docs docs/design/sequence/outer/01-소셜로그인.puml
```

---

## 다음 단계

1. 내부 시퀀스 설계 (Inner Sequence)
   - 각 서비스 내부 처리 흐름
   - 데이터베이스 상호작용
   - 이벤트 핸들러 처리 로직

2. API 설계서 작성
   - 각 서비스별 REST API 명세
   - Request/Response 스키마

3. 클래스 다이어그램 설계
   - 각 서비스의 도메인 모델
   - 엔티티 관계도

---
