# 프론트엔드 UI/UX 설계서

> 작성자: 데브-프론트 (프론트엔드 개발자)
> 작성일: 2026-02-23
> 근거: uiux.md (도그냥), style-guide.md (도그냥), 아키텍처 설계서
> 플랫폼: Flutter 3.x (Dart) — 크로스플랫폼 모바일 앱
> 앱명: travel-planner (여행 중 실시간 일정 최적화 가이드 앱)

---

## 1. UI 프레임워크 선택

### 1-1. 선택: Flutter 3.x + Material Design 3 (Material You)

| 항목 | 내용 |
|------|------|
| 프레임워크 | Flutter 3.x (Dart) |
| 디자인 시스템 | Material Design 3 (Material You) |
| 렌더링 엔진 | Impeller (iOS/Android 공통) |
| 최소 지원 OS | iOS 16.0 이상 / Android API 26 (Android 8.0) 이상 |

### 1-2. 선정 이유

| # | 이유 | 설명 |
|:-:|------|------|
| 1 | **다크 테마 기본 지원** | Material 3의 `ColorScheme.dark()` + `ThemeData` 기반으로 디자인 토큰(`--bg-primary` #0A0A0A 등)을 Flutter `ThemeExtension`으로 정의. 라이트/다크 전환 없이 다크 단일 테마로 운영 |
| 2 | **커스텀 위젯 자유도** | Skia/Impeller 엔진 위에서 픽셀 단위 커스텀 렌더링 가능. 상태 배지 바운스 애니메이션, 그라디언트 오버레이 카드 등 설계서의 모든 UI 표현 가능 |
| 3 | **60fps 렌더링** | `AnimationController` + `Tween` 기반 선언형 애니메이션으로 배지 상태 변경 스프링 애니메이션(400ms), 바텀시트 슬라이드(350ms) 모두 60fps 유지 |
| 4 | **단일 코드베이스** | iOS/Android 동시 배포. Google Sign-In, Apple Sign-In, Firebase Messaging 모두 Flutter 공식 플러그인 제공 |
| 5 | **Material 3 컴포넌트** | `NavigationBar`, `BottomSheet`, `SearchBar` 등 설계서 요구 컴포넌트가 Material 3에 내장. 커스터마이즈 비용 절감 |

### 1-3. 주요 패키지

| 카테고리 | 패키지 | 버전 | 용도 |
|----------|--------|------|------|
| 상태 관리 | `flutter_riverpod` | ^2.x | Provider 패턴, 비동기 상태(AsyncValue), 코드 생성(riverpod_generator) |
| 라우팅 | `go_router` | ^14.x | 선언형 라우팅, 딥링크, ShellRoute 기반 탭 네비게이션 |
| HTTP | `dio` | ^5.x | REST API 클라이언트, 인터셉터(토큰 자동 갱신, 에러 핸들링) |
| Push 알림 | `firebase_messaging` | ^15.x | FCM 기반 Push 수신, 포그라운드/백그라운드 핸들러 |
| Google 로그인 | `google_sign_in` | ^6.x | OAuth 2.0, ID 토큰 획득 |
| Apple 로그인 | `sign_in_with_apple` | ^6.x | Sign in with Apple, iOS/Android 지원 |
| 이미지 캐싱 | `cached_network_image` | ^3.x | 장소 사진 캐싱, 플레이스홀더/에러 위젯 |
| 위치 정보 | `geolocator` | ^13.x | GPS 좌표 획득, 권한 관리 |
| 인앱 결제 | `in_app_purchase` | ^3.x | Apple IAP / Google Play Billing 추상화 |
| 로컬 저장소 | `flutter_secure_storage` | ^9.x | JWT 토큰 보안 저장 |
| 코드 생성 | `freezed` + `json_serializable` | ^2.x | 불변 모델 클래스, JSON 직렬화 |
| 날짜/시간 | `intl` | ^0.19.x | 현지화 날짜 포맷, 타임존(IANA) 처리 |
| 드래그 앤 드롭 | `reorderables` | ^0.6.x | 일정 순서 변경 드래그 앤 드롭 |

---

## 2. 화면 목록 정의

### 2-1. 전체 화면 목록

| 화면 ID | 화면명 | 라우트 경로 | 진입 방식 | 관련 UFR |
|---------|--------|------------|----------|---------|
| SCR-000 | 스플래시 | `/splash` | 앱 시작 | - |
| SCR-001 | 소셜 로그인 | `/login` | 인증 미완료 시 자동 리다이렉트 | UFR-AUTH-010 |
| SCR-002 | 온보딩 가이드 (1단계) | `/onboarding` | 최초 로그인 후 | UFR-SCHD-005 |
| SCR-010 | 여행 목록 | `/trips` | TAB1 기본 화면 | UFR-SCHD-050 |
| SCR-011 | 여행 생성 | `/trips/new` | 여행 목록 FAB 탭 | UFR-SCHD-010 |
| SCR-012 | 권한 동의 | `/trips/new/permissions` | 여행 생성 후 최초 | UFR-SCHD-010, NFR-SEC-020 |
| SCR-013 | 일정표 (일별 타임라인) | `/trips/:tripId/schedule` | 여행 목록 탭 | UFR-SCHD-050, UFR-MNTR-030 |
| SCR-014 | 장소 검색 | `/trips/:tripId/places/search` | 장소 추가 버튼 탭 | UFR-SCHD-020, UFR-PLCE-010 |
| SCR-015 | 장소 시간 지정 | `/trips/:tripId/places/schedule` | 장소 검색 결과 탭 | UFR-SCHD-030 |
| SCR-016 | 장소 교체 결과 | `/trips/:tripId/schedule/replaced` | 대안 카드 선택 후 | UFR-SCHD-040, UFR-ALTN-030 |
| SCR-017 | 장소 상세 (바텀시트) | - (바텀시트) | 검색 결과 탭 | UFR-PLCE-020 |
| SCR-018 | 상태 상세 (바텀시트) | - (바텀시트) | 일정표 배지 탭 | UFR-MNTR-040 |
| SCR-020 | 브리핑 목록 | `/briefings` | TAB2 기본 화면 | UFR-BRIF-060 |
| SCR-021 | 브리핑 상세 | `/briefings/:briefingId` | 브리핑 목록 탭 / Push 딥링크 | UFR-BRIF-050 |
| SCR-022 | 대안 카드 화면 | `/briefings/:briefingId/alternatives` | 브리핑 상세 "대안 보기" | UFR-ALTN-020 |
| SCR-023 | Paywall | `/paywall` | 무료 티어 대안 카드 진입 | UFR-ALTN-050, UFR-PAY-010 |
| SCR-030 | 마이페이지 | `/my` | TAB3 기본 화면 | UFR-AUTH-010 |
| SCR-031 | 구독 관리 | `/my/subscription` | 마이페이지 구독 관리 탭 | UFR-PAY-010 |
| SCR-032 | 알림 설정 | `/my/notifications` | 마이페이지 알림 설정 탭 | UFR-MNTR-050 |
| SCR-033 | 위치정보 동의 관리 | `/my/location-consent` | 마이페이지 위치정보 탭 | NFR-SEC-020 |

### 2-2. 바텀시트 목록 (라우트 미사용, 오버레이)

| 바텀시트 ID | 이름 | 호출 화면 | 내용 |
|------------|------|----------|------|
| BS-001 | 장소 상세 | SCR-014 (장소 검색) | 장소 사진, 영업시간, 미니맵, "일정에 추가" CTA |
| BS-002 | 상태 상세 | SCR-013 (일정표) | 종합 배지, 4개 항목 개별 상태, 판정 사유, "대안 보기" CTA |
| BS-003 | 날짜 피커 | SCR-011 (여행 생성), SCR-015 (시간 지정) | 캘린더 그리드, 여행 기간 범위 강조 |
| BS-004 | 도시 선택 | SCR-011 (여행 생성) | MVP 지원 도시 5개 목록, 선택 체크 |
| BS-005 | 삭제 확인 | SCR-013 (일정표) | 장소 삭제 확인 다이얼로그 |

---

## 3. 화면 간 사용자 플로우

### 3-1. 플로우 A: 첫 진입 플로우

```
[앱 시작]
    |
    v
[SCR-000: 스플래시]
    | 1.5초 후 인증 상태 확인
    |
    +-- (미인증) ---------> [SCR-001: 소셜 로그인]
    |                           | Google / Apple 인증 성공
    |                           v
    |                       [SCR-002: 온보딩 가이드]
    |                           | 3단계 완료 또는 건너뛰기
    |                           v
    |                       [SCR-011: 여행 생성]
    |                           | 여행명/기간/도시 입력
    |                           v
    |                       [SCR-012: 권한 동의]
    |                           | 위치정보 동의 → OS Push 권한
    |                           v
    |                       [SCR-013: 일정표 (빈 상태)]
    |
    +-- (인증 완료) ------> [SCR-010: 여행 목록]
                                | 여행 선택
                                v
                            [SCR-013: 일정표]
```

**트리거 조건**: 첫 설치 후 앱 진입. 온보딩 완료 플래그(`SharedPreferences: onboarding_completed`)가 없을 때 온보딩 표시.

### 3-2. 플로우 B: 일정 관리 플로우

```
[SCR-010: 여행 목록]
    | 여행 카드 탭
    v
[SCR-013: 일정표]
    |
    +-- (장소 추가 버튼) --> [SCR-014: 장소 검색]
    |                           | 검색어 입력 → 결과 리스트
    |                           |
    |                           +-- (장소 탭) -----> [BS-001: 장소 상세]
    |                           |                       | "일정에 추가" CTA
    |                           |                       v
    |                           |                   [SCR-015: 시간 지정]
    |                           |                       | 날짜/시간 선택
    |                           |                       v
    |                           |                   [SCR-013: 일정표 (갱신)]
    |                           |
    |                           +-- (직접 탭) -----> [SCR-015: 시간 지정]
    |
    +-- (장소 카드 좌 스와이프) --> [BS-005: 삭제 확인]
    |                                   | 확인
    |                                   v
    |                               [SCR-013: 일정표 (갱신, 토스트)]
    |
    +-- (장소 카드 길게 누르기) --> [드래그 앤 드롭 모드]
                                        | 순서 변경 후 드롭
                                        v
                                    [SCR-013: 일정표 (갱신)]
```

### 3-3. 플로우 C: 브리핑 확인 플로우

```
[Push 알림 수신 (OS 레벨)]
    | 탭
    v
[SCR-021: 브리핑 상세]  <------------- [SCR-020: 브리핑 목록 → 탭]
    |
    +-- (안심 브리핑) ------> "안심 확인 완료" 버튼 → [SCR-020: 브리핑 목록]
    |
    +-- (주의 브리핑) ------> "대안 보기" 버튼
    |                               |
    |                               +-- (유료 티어) -----> [SCR-022: 대안 카드]
    |                               |                           | 카드 선택
    |                               |                           v
    |                               |                       [SCR-016: 교체 결과]
    |                               |                           v
    |                               |                       [SCR-013: 일정표]
    |                               |
    |                               +-- (무료 티어) -----> [SCR-023: Paywall]
    |                                                           | 결제
    |                                                           v
    |                                                       [SCR-022: 대안 카드]
    |
    +-- (만료 브리핑) ------> "최신 상태 조회" 버튼 → API 호출 → 상태 갱신
```

### 3-4. 플로우 D: 상태 배지 플로우

```
[SCR-013: 일정표]
    | 장소 카드 탭 (배지 포함)
    v
[BS-002: 상태 상세]
    |
    +-- (초록 배지) ------> "확인" 닫기 → [SCR-013: 일정표]
    |
    +-- (노랑/빨강 배지) --> "대안 보기" CTA 버튼
    |                               |
    |                               v
    |                           [SCR-022: 대안 카드] 또는 [SCR-023: Paywall]
    |
    +-- (회색 배지) ------> "데이터 미확인" 안내 → 닫기 → [SCR-013: 일정표]
    |
[Push 알림: 상태 악화]
    | 탭 (딥링크)
    v
[BS-002: 상태 상세] (해당 장소)
```

**Push 딥링크**: `travelplanner://status/{tripId}/{placeId}` → `go_router`의 `redirect`로 처리, 로그인 미완료 시 로그인 후 복귀.

### 3-5. 플로우 E: 구독 플로우

```
[SCR-022: 대안 카드] 또는 [SCR-020: 브리핑 목록 한도 초과 안내]
    | 무료 티어 제한 감지
    v
[SCR-023: Paywall]
    |
    +-- (Trip Pass 선택) --> [인앱 결제 (Apple IAP / Google Play)]
    |                               |
    |                               +-- (결제 성공) --> 활성화 완료 토스트 → [SCR-022: 대안 카드]
    |                               +-- (결제 실패) --> 에러 다이얼로그 → [SCR-023: Paywall]
    |
    +-- (Pro 선택) -------> [인앱 결제 (구독)]
    |                               | (동일 흐름)
    |
    +-- (마이페이지 진입) --> [SCR-031: 구독 관리]
                                    |
                                    v
                                [SCR-023: Paywall] (업그레이드 경로)
```

---

## 4. 화면별 상세 설계

### 4-1. SCR-000: 스플래시 화면

#### 4-1-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-000-01 | 브랜드 표시 | 앱 로고 및 서비스명 표시 |
| F-000-02 | 인증 상태 확인 | `flutter_secure_storage`에서 JWT 토큰 조회, 유효성 검증 |
| F-000-03 | 온보딩 플래그 확인 | `SharedPreferences`에서 온보딩 완료 여부 조회 |
| F-000-04 | 자동 라우팅 | 인증/온보딩 상태에 따라 적절한 화면으로 자동 이동 |

#### 4-1-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 배경 | `Scaffold` + `Container` | `color: --bg-primary (#0A0A0A)` | - |
| 앱 로고 | `SvgPicture` | `width: 80dp, height: 80dp` | assets/logo.svg |
| 서비스명 | `Text` | `--font-h1 (22px, Bold), color: --text-primary` | "travel-planner" |
| 슬로건 | `Text` | `--font-body2 (14px), color: --text-secondary` | "여행 중 실시간 일정 최적화 가이드" |
| 로딩 인디케이터 | `CircularProgressIndicator` | `color: --accent-red, strokeWidth: 2` | 인증 확인 중 표시 |

#### 4-1-3. 인터랙션

| 상황 | 동작 | 애니메이션 |
|------|------|-----------|
| 앱 시작 | 로고/텍스트 표시 후 인증 확인 | 페이드 인 (300ms, `FadeTransition`) |
| 인증 완료 + 온보딩 완료 | `/trips`로 이동 | 페이드 아웃 → 여행 목록 슬라이드 인 |
| 인증 완료 + 온보딩 미완료 | `/onboarding`으로 이동 | 페이드 아웃 |
| 미인증 | `/login`으로 이동 | 페이드 아웃 |
| 네트워크 오류 | 에러 토스트 + 재시도 버튼 표시 | - |

---

### 4-2. SCR-001: 소셜 로그인

#### 4-2-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-001-01 | Google 로그인 | `google_sign_in` 패키지로 OAuth 2.0 플로우 실행, ID 토큰을 백엔드 전송 |
| F-001-02 | Apple 로그인 | `sign_in_with_apple` 패키지, iOS/Android 모두 지원, Identity Token 전송 |
| F-001-03 | 로그인 상태 피드백 | 버튼 로딩 상태, 성공/실패 토스트 |
| F-001-04 | 약관 링크 | 이용약관, 개인정보처리방침 웹뷰 이동 |

#### 4-2-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 배경 | `Scaffold` | `backgroundColor: --bg-primary` | - |
| 로고 영역 | `Column` (로고+서비스명+슬로건) | `MainAxisAlignment.center` | - |
| Google 로그인 버튼 | `ElevatedButton` | `height: 52dp, radius: 8dp, bgColor: #FFFFFF, textColor: #000000` | `authState.isLoading` → 스피너 |
| Apple 로그인 버튼 | `ElevatedButton` | `height: 52dp, radius: 8dp, bgColor: #000000, textColor: #FFFFFF` | `authState.isLoading` → 스피너 |
| 약관 안내 | `RichText` | `--font-caption (12px), color: --text-secondary` | 링크 탭 시 웹뷰 |
| 버튼 간격 | `SizedBox` | `height: 12dp` | - |

#### 4-2-3. 인터랙션

| 이벤트 | 동작 | 피드백 |
|--------|------|--------|
| Google 버튼 탭 | 버튼 스케일 0.97 → 1.0 (150ms), Google 인증 시트 표시 | 버튼 내 텍스트 → `CircularProgressIndicator` |
| Apple 버튼 탭 | 버튼 스케일 0.97 → 1.0 (150ms), Apple 인증 시트 표시 | 버튼 내 텍스트 → `CircularProgressIndicator` |
| 인증 성공 | 온보딩 상태에 따라 라우팅 | 페이드 아웃 전환 |
| 인증 실패 | 토스트 메시지 | "로그인에 실패했습니다. 다시 시도해주세요" (3초) |
| 네트워크 오류 | 토스트 메시지 | "인터넷 연결을 확인해주세요" (3초) |

---

### 4-3. SCR-002: 온보딩 가이드 (3단계)

#### 4-3-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-002-01 | 단계별 콘텐츠 표시 | 1단계: 상태 배지, 2단계: 출발 전 브리핑, 3단계: 대안 카드 소개 |
| F-002-02 | 건너뛰기 | 온보딩 완료 플래그 저장 후 여행 생성으로 이동 |
| F-002-03 | 스와이프 네비게이션 | 좌우 스와이프로 단계 이동 |
| F-002-04 | 인디케이터 | 현재 단계 도트 인디케이터 |
| F-002-05 | 완료 처리 | 3단계 완료 후 `SharedPreferences` 플래그 저장 |

#### 4-3-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 페이지 뷰 | `PageView` | `physics: BouncingScrollPhysics` | `currentPage` (0~2) |
| 건너뛰기 버튼 | `TextButton` | `color: --text-secondary, --font-body2` | - |
| 일러스트 | `SvgPicture` / `Image.asset` | `height: 280dp` | 각 단계별 일러스트 |
| 타이틀 | `Text` | `--font-display (28px, Bold), color: --text-primary` | 단계별 타이틀 |
| 설명 | `Text` | `--font-body1 (15px), color: --text-secondary, textAlign: center` | 단계별 설명 |
| 인디케이터 | `Row` of `AnimatedContainer` | `width: 8dp(비활성)/24dp(활성), height: 8dp, radius: 4dp` | `currentPage` |
| 다음/시작 버튼 | `ElevatedButton` | `height: 52dp, bgColor: --accent-red, radius: 8dp, fullWidth` | 마지막 단계: "시작하기" |

#### 4-3-3. 인터랙션

| 이벤트 | 동작 | 애니메이션 |
|--------|------|-----------|
| 좌우 스와이프 | 다음/이전 페이지 전환 | `PageView` 기본 슬라이드 (250ms) |
| 인디케이터 도트 | 탭 시 해당 페이지로 이동 | 도트 크기 애니메이션 (150ms) |
| "다음" 버튼 | 다음 페이지 이동 | 슬라이드 |
| "시작하기" 버튼 | 온보딩 완료 저장, `/trips/new` 이동 | 페이드 아웃 |
| "건너뛰기" 버튼 | 온보딩 완료 저장, `/trips/new` 이동 | 즉시 전환 |

**단계별 콘텐츠**:

| 단계 | 타이틀 | 설명 | 일러스트 |
|------|--------|------|---------|
| 1단계 | "장소 상태를 한눈에" | "초록은 안심, 노랑은 주의, 빨강은 위험. 여행지 상태를 실시간으로 확인하세요." | 상태 배지 4종 + 타임라인 |
| 2단계 | "출발 전 미리 알려드려요" | "방문 15분 전, AI가 날씨·혼잡도·영업시간을 확인해 알림을 보내드립니다." | Push 알림 + 브리핑 카드 |
| 3단계 | "대안을 탭 한 번으로" | "문제가 감지되면 주변 대안 장소 3곳을 추천해드려요. 탭 한 번으로 일정을 바꾸세요." | 대안 카드 3장 + 체크 |

---

### 4-4. SCR-010: 여행 목록

#### 4-4-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-010-01 | 여행 카드 리스트 | 생성된 여행 목록 표시, 여행명/기간/도시 |
| F-010-02 | 여행 생성 진입 | FAB(Floating Action Button) 탭으로 SCR-011 이동 |
| F-010-03 | 여행 선택 | 카드 탭 시 해당 여행의 일정표(SCR-013) 이동 |
| F-010-04 | 빈 상태 | 등록된 여행 없을 때 Empty State 표시 |

#### 4-4-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 앱 바 | `SliverAppBar` | `title: "내 여행", backgroundColor: --bg-primary, floating: true` | - |
| 여행 리스트 | `SliverList` of `TripCard` | `itemExtent: null (가변 높이)` | `tripListProvider` |
| 여행 카드 | `Card` | `bgColor: --bg-card (#1A1A1A), radius: 12dp, padding: 16dp` | `TripModel` |
| - 여행명 | `Text` | `--font-h2 (18px, SemiBold), color: --text-primary` | `trip.name` |
| - 기간 | `Text` | `--font-caption (12px), color: --text-secondary` | `trip.startDate ~ trip.endDate` |
| - 도시 | `Chip` | `bgColor: --bg-input, textColor: --text-secondary` | `trip.city` |
| - 진행 상태 | `Chip` | D-Day 또는 진행중/완료 뱃지 | `trip.status` |
| FAB | `FloatingActionButton` | `bgColor: --accent-red, icon: Icons.add (24dp)` | - |
| 빈 상태 | `EmptyStateWidget` | 아이콘: `calendar-days` 48dp, 타이틀/설명/버튼 | `trips.isEmpty` |
| 로딩 | `SliverList` of `TripCardSkeleton` | 스켈레톤 카드 3개 | `tripListProvider.isLoading` |

#### 4-4-3. 인터랙션

| 이벤트 | 동작 | 피드백 |
|--------|------|--------|
| 카드 탭 | 해당 여행 일정표 이동 (push) | 탭 리플 이펙트, 슬라이드 전환 |
| FAB 탭 | 여행 생성 화면 이동 | 스케일 0.97 → 1.0 (150ms), push 전환 |
| 풀 투 리프레시 | 여행 목록 갱신 | `RefreshIndicator` 스피너 |

---

### 4-5. SCR-011: 여행 생성

#### 4-5-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-011-01 | 여행명 입력 | 텍스트 필드, 1~50자, 글자 수 카운터 |
| F-011-02 | 기간 선택 | 시작일/종료일 날짜 피커(BS-003), 시작일 >= 오늘 검증 |
| F-011-03 | 도시 선택 | 드롭다운 바텀시트(BS-004), MVP 5개 도시 |
| F-011-04 | 유효성 검증 | 모든 필드 입력 완료 시 CTA 활성화 |
| F-011-05 | 여행 생성 API 호출 | 성공 시 권한 동의(SCR-012) 또는 일정표(SCR-013) 이동 |

#### 4-5-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 앱 바 | `AppBar` | `leading: 뒤로가기, title: "새 여행 만들기"` | - |
| 여행명 필드 | `TextFormField` | `bgColor: --bg-input, radius: 8dp, height: 48dp, maxLength: 50` | `tripNameController` |
| 글자 수 카운터 | `Text` | `--font-caption, color: --text-secondary` | `tripNameController.text.length / 50` |
| 시작일 버튼 | `InkWell` + `Container` | `bgColor: --bg-input, radius: 8dp, height: 48dp` | `tripForm.startDate` |
| 종료일 버튼 | `InkWell` + `Container` | `bgColor: --bg-input, radius: 8dp, height: 48dp` | `tripForm.endDate` |
| 도시 선택 버튼 | `InkWell` + `Container` | `bgColor: --bg-input, radius: 8dp, height: 48dp, trailingIcon: chevron-down` | `tripForm.city` |
| 지원 도시 안내 | `Text` | `--font-caption, color: --text-disabled` | "도쿄, 오사카, 교토, 방콕, 싱가포르" |
| CTA 버튼 | `ElevatedButton` | `height: 52dp, bgColor: --accent-red (활성) / opacity40% (비활성), radius: 8dp` | `tripForm.isValid` |

#### 4-5-3. 인터랙션

| 이벤트 | 동작 | 피드백 |
|--------|------|--------|
| 여행명 입력 | 실시간 글자 수 업데이트, 50자 초과 시 에러 표시 | 에러 보더 `--status-red` |
| 시작일/종료일 탭 | BS-003 날짜 피커 바텀시트 표시 | 바텀시트 슬라이드 업 (350ms) |
| 도시 탭 | BS-004 도시 선택 바텀시트 표시 | 바텀시트 슬라이드 업 (350ms) |
| CTA 탭 (유효) | 로딩 스피너, API 호출 | 버튼 내 `CircularProgressIndicator` |
| 생성 성공 | 최초 여행 → SCR-012 / 이후 여행 → SCR-013 | push 전환 |
| 생성 실패 | 에러 토스트 | "여행 생성에 실패했습니다" |

---

### 4-6. SCR-012: 권한 동의

#### 4-6-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-012-01 | 위치정보 수집 고지 | 수집 항목/목적/보유 기간 명시 (NFR-SEC-020) |
| F-012-02 | 위치정보 없이 이용 가능한 기능 안내 | 거부 시 제한 범위 명확히 안내 |
| F-012-03 | 동의 처리 | `geolocator` 권한 요청, 동의 결과 서버 전송 |
| F-012-04 | Push 권한 요청 | 위치 동의 후 OS 네이티브 Push 권한 다이얼로그 |

#### 4-6-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 배경 | `Scaffold` | `backgroundColor: --bg-primary` | - |
| 위치 아이콘 | `Icon` | `map-pin, 48dp, color: --accent-blue` | - |
| 타이틀 | `Text` | `--font-h1 (22px, Bold)` | "위치정보 수집 동의" |
| 고지 카드 | `Container` | `bgColor: --bg-card, radius: 12dp, padding: 16dp` | - |
| - 수집 항목/목적/보유 기간 | `Column` of `Row` | `--font-body2` | 정적 텍스트 |
| 이용 가능 기능 안내 | `Text` + `Column` of `Row` | `--font-body2, color: --text-secondary` | 정적 텍스트 |
| CTA 버튼 | `ElevatedButton` | `height: 52dp, bgColor: --accent-red, fullWidth` | "동의하고 계속" |
| 거부 버튼 | `TextButton` | `color: --text-secondary` | "위치정보 없이 계속하기" |

#### 4-6-3. 인터랙션

| 이벤트 | 동작 | 피드백 |
|--------|------|--------|
| "동의하고 계속" 탭 | `geolocator.requestPermission()` 호출, 동의 일시 서버 전송 | OS 권한 다이얼로그 → Push 권한 다이얼로그 순서 |
| "위치정보 없이 계속하기" 탭 | 거부 결과 서버 전송, SCR-013 이동 | 기능 제한 안내 토스트 |
| 권한 거부 후 재요청 | 시스템 설정 앱으로 이동 안내 | 다이얼로그 "설정에서 변경하기" |

---

### 4-7. SCR-013: 일정표 (일별 타임라인)

#### 4-7-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-013-01 | 일별 타임라인 표시 | 시간순 장소 카드 + 이동시간 연결선 |
| F-013-02 | 상태 배지 표시 | 각 장소에 4색 상태 배지 (초록/노랑/빨강/회색) |
| F-013-03 | 날짜 이동 | 상단 날짜 셀렉터로 여행 기간 내 날짜 이동 |
| F-013-04 | 여행 전환 | 상단 여행명 드롭다운으로 다른 여행 전환 |
| F-013-05 | 장소 삭제 | 좌 스와이프 → 삭제 버튼, BS-005 확인 |
| F-013-06 | 장소 순서 변경 | 길게 누르기 → 드래그 앤 드롭 (Could) |
| F-013-07 | 배지 탭 | BS-002 상태 상세 바텀시트 표시 |
| F-013-08 | 풀 투 리프레시 | 배지 데이터 갱신 |

#### 4-7-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 앱 바 | `SliverAppBar` | `pinned: true, backgroundColor: --bg-primary` | - |
| 여행명 드롭다운 | `PopupMenuButton` | `--font-h2 (18px, SemiBold), trailing: chevron-down` | `selectedTripProvider` |
| 날짜 셀렉터 | `Row` of `IconButton` + `Text` + `IconButton` | 이전/다음 날짜 화살표 | `selectedDateProvider` |
| 날짜 헤더 | `Text` | `--font-h3 (16px, SemiBold), padding: 16dp` | "3월 16일 (월) — 2일차" |
| 타임라인 리스트 | `ReorderableListView` | `physics: BouncingScrollPhysics` | `scheduledPlacesProvider` |
| 장소 카드 | `Dismissible` + `SchedulePlaceCard` | `bgColor: --bg-card, radius: 12dp` | `PlaceScheduleModel` |
| - 상태 배지 | `StatusBadgeWidget` | `size: 24dp, shape: circle` | `place.statusCode` |
| - 장소명 | `Text` | `--font-h3 (16px, SemiBold)` | `place.name` |
| - 보조 정보 | `Text` | `--font-caption (12px), color: --text-secondary` | `place.subInfo` (날씨/혼잡도/영업상태) |
| 이동시간 연결선 | `TimelineDivider` (커스텀 위젯) | `color: --bg-input, dashed: true` | `place.travelTime` |
| 시간 레이블 | `Text` | `--font-caption, color: --text-secondary, width: 48dp` | `place.visitTime` |
| 스와이프 삭제 배경 | `Container` | `bgColor: --status-red, icon: trash-2 (white)` | - |
| 장소 추가 버튼 | `ElevatedButton` | `height: 52dp, bgColor: --bg-card, textColor: --accent-red, fullWidth, icon: plus` | - |
| 빈 상태 | `EmptyStateWidget` | "아직 장소가 없습니다", CTA: "장소 추가" | `places.isEmpty` |
| 로딩 | 스켈레톤 카드 3개 | 펄스 애니메이션 | `scheduleProvider.isLoading` |

#### 4-7-3. 인터랙션

| 이벤트 | 동작 | 애니메이션 / 피드백 |
|--------|------|-------------------|
| 장소 카드 탭 | BS-002 상태 상세 바텀시트 표시 | 탭 리플 + 바텀시트 슬라이드 업 (350ms) |
| 좌 스와이프 | 빨강 배경 + 삭제 아이콘 노출, BS-005 확인 | `Dismissible` 스와이프 애니메이션 |
| 삭제 확인 | 카드 제거 + 토스트 | 리스트 아이템 높이 0 collapse (250ms) + "장소가 삭제되었습니다" |
| 길게 누르기 | 드래그 모드 활성화 | 카드 그림자 강조 + opacity 0.7 |
| 드래그 드롭 | 순서 재정렬, API 업데이트 | 드롭 위치 프리뷰 |
| 배지 상태 변경 | 배지 색상 + 아이콘 변경 | 스케일 바운스 0.8→1.2→1.0 (400ms, spring) |
| 날짜 변경 | 해당 날짜 일정 로딩 | 페이드 인/아웃 (150ms) |
| 풀 투 리프레시 | 배지 데이터 갱신 | `RefreshIndicator` |
| "장소 추가" 버튼 탭 | SCR-014 장소 검색 이동 | push 전환 |

---

### 4-8. SCR-014: 장소 검색

#### 4-8-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-014-01 | 키워드 검색 | 2글자 이상 입력 시 자동 검색, 현재 여행 도시 내 |
| F-014-02 | 검색 결과 리스트 | 최대 10건, 장소명/주소/평점/카테고리 |
| F-014-03 | 장소 선택 | 탭 시 BS-001 장소 상세 또는 SCR-015 시간 지정 |
| F-014-04 | 스켈레톤 로딩 | API 응답 대기 중 표시 |
| F-014-05 | 빈 결과 안내 | "검색 결과가 없습니다" |

#### 4-8-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 검색바 | `SearchBar` (Material 3) | `bgColor: --bg-input, radius: 12dp, height: 44dp, leadingIcon: search` | `searchController` |
| 클리어 버튼 | `IconButton` | `icon: x (20dp), color: --text-disabled` | `query.isNotEmpty` |
| 결과 건수 | `Text` | `--font-caption, color: --text-secondary` | "검색 결과 (N건)" |
| 결과 리스트 | `ListView.builder` | `itemCount: results.length` | `placeSearchProvider` |
| 장소 리스트 아이템 | `ListTile` | `minHeight: 64dp, bgColor: --bg-card, radius: 12dp` | `PlaceSearchResult` |
| - 장소명 | `Text` | `--font-h3 (16px, SemiBold)` | `place.name` |
| - 주소 | `Text` | `--font-caption (12px), color: --text-secondary` | `place.address` |
| - 평점/카테고리 | `Row` | `icon: star (amber), --font-caption` | `place.rating, place.category` |
| 로딩 스켈레톤 | `ListView.builder` of `Skeleton` | 5개, 펄스 애니메이션 | `isLoading` |
| 빈 상태 | `EmptyStateWidget` | "검색 결과가 없습니다" | `results.isEmpty && !isLoading` |

#### 4-8-3. 인터랙션

| 이벤트 | 동작 | 피드백 |
|--------|------|--------|
| 2글자 입력 | 500ms 디바운스 후 검색 API 호출 | 스켈레톤 표시 → 결과 페이드 인 |
| 클리어 버튼 탭 | 검색어 초기화, 결과 초기화 | - |
| 장소 아이템 탭 | BS-001 장소 상세 바텀시트 표시 | 탭 리플 + 바텀시트 슬라이드 업 |
| 뒤로가기 | 일정표(SCR-013)로 복귀 | pop 전환 |

---

### 4-9. SCR-015: 장소 시간 지정

#### 4-9-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-015-01 | 선택된 장소 요약 표시 | 장소명, 영업시간, 카테고리, 평점 |
| F-015-02 | 날짜 선택 | 여행 기간 내 날짜만 선택 가능 |
| F-015-03 | 시간 선택 | 30분 단위 드럼(Wheel) 피커, 현지 시간(IANA timezone) 기준 |
| F-015-04 | 영업시간 외 경고 | 선택 시간이 영업시간 외일 때 경고 카드 표시 (강제 차단 아님) |
| F-015-05 | 일정 추가 API 호출 | 성공 시 일정표 복귀, 모니터링 대상 자동 등록 |

#### 4-9-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 앱 바 | `AppBar` | `leading: 뒤로가기, title: "장소 추가"` | - |
| 장소 요약 카드 | `Container` | `bgColor: --bg-card, radius: 12dp, padding: 16dp` | `selectedPlace` |
| - 장소명 | `Text` | `--font-h2 (18px, SemiBold)` | `place.name` |
| - 영업시간 | `Text` | `--font-caption, color: --text-secondary` | `place.openingHours` |
| 날짜 선택 버튼 | `InkWell` + `Container` | `bgColor: --bg-input, radius: 8dp` | `selectedDate` |
| 시간 피커 | `CupertinoTimerPicker` 또는 커스텀 `WheelPicker` | `30분 단위` | `selectedTime` |
| 영업시간 외 경고 | `AnimatedSwitcher` + `Container` | `bgColor: rgba(255,59,48,0.1), radius: 8dp, icon: alert-circle` | `isOutsideHours` |
| CTA 버튼 | `ElevatedButton` | `height: 52dp, bgColor: --accent-red, fullWidth` | "일정에 추가" |

#### 4-9-3. 인터랙션

| 이벤트 | 동작 | 피드백 |
|--------|------|--------|
| 날짜 버튼 탭 | BS-003 날짜 피커 표시 | 바텀시트 슬라이드 업 |
| 시간 변경 | 영업시간 검증, 외부 시간 시 경고 표시 | `AnimatedSwitcher` 페이드 인/아웃 |
| "일정에 추가" 탭 | API 호출, 로딩 | 버튼 스피너 |
| 추가 성공 | 일정표 복귀 | pop + "일정에 추가되었습니다" 토스트 |

---

### 4-10. SCR-016: 장소 교체 결과

#### 4-10-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-016-01 | 변경 요약 표시 | 원래 장소 → 대안 장소, 이동시간 변화 |
| F-016-02 | 후속 일정 영향 안내 | 재계산된 이동시간 표시 |
| F-016-03 | 확인 후 일정표 이동 | 확인 버튼 탭 시 업데이트된 일정표 표시 |

#### 4-10-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 성공 카드 | `Container` | `bgColor: --bg-card, radius: 12dp, padding: 20dp` | `replacementResult` |
| 성공 아이콘 | `Icon` | `check-circle, 32dp, color: --status-green` | - |
| 변경 요약 | `Column` | 원래 장소명 → (화살표 아이콘) → 대안 장소명 | `replacementResult` |
| 이동시간 변화 | `Row` | "도보 15분 → 도보 5분" (strikethrough + 새 값) | `replacementResult.travelTimeChange` |
| 후속 일정 안내 | `Text` | `--font-body2, color: --text-secondary` | "후속 일정 이동시간 재계산 완료" |
| 확인 버튼 | `ElevatedButton` | `height: 52dp, bgColor: --accent-red` | - |

#### 4-10-3. 인터랙션

| 이벤트 | 동작 | 피드백 |
|--------|------|--------|
| 화면 진입 | 성공 카드 + 업데이트된 일정표 표시 | 카드 스케일 인 (250ms) |
| "확인" 버튼 탭 | 카드 닫힘, 일정표 포커스 | - |

---

### 4-11. SCR-020: 브리핑 목록

#### 4-11-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-020-01 | 브리핑 리스트 | 오늘 날짜 기준, 최신순 정렬 |
| F-020-02 | 유형 구분 표시 | 안심(초록), 주의(노랑/빨강) 배지 시각 구분 |
| F-020-03 | 한도 초과 안내 | Free 티어 한도 초과 시 인라인 업그레이드 카드 |
| F-020-04 | 브리핑 상세 이동 | 카드 탭 시 SCR-021 이동 |
| F-020-05 | 빈 상태 | "오늘의 브리핑이 없습니다" |

#### 4-11-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 앱 바 | `SliverAppBar` | `title: "브리핑", pinned: true` | - |
| 날짜 헤더 | `Text` | `--font-caption, color: --text-secondary, padding: 16dp` | 오늘 날짜 |
| 브리핑 리스트 | `SliverList` | `ListView.builder` | `briefingListProvider` |
| 브리핑 카드 | `BriefingCard` (커스텀 위젯) | `bgColor: --bg-card, radius: 12dp, padding: 16dp` | `BriefingModel` |
| - 유형 배지 | `StatusBadgeWidget` | `size: 24dp` | `briefing.statusCode` |
| - 생성 시각 | `Text` | `--font-caption, color: --text-secondary` | `briefing.createdAt` |
| - 장소명 | `Text` | `--font-h3 (16px, SemiBold)` | `briefing.placeName` |
| - 총평 | `Text` | `--font-body2 (14px), color: --text-secondary, maxLines: 2` | `briefing.summary` |
| 한도 초과 카드 | `Container` | `bgColor: --bg-card, leftBorder: 4dp solid --accent-purple` | `isLimitExceeded` |
| 빈 상태 | `EmptyStateWidget` | 아이콘: bell, "오늘의 브리핑이 없습니다" | `briefings.isEmpty` |

#### 4-11-3. 인터랙션

| 이벤트 | 동작 | 피드백 |
|--------|------|--------|
| 카드 탭 | SCR-021 브리핑 상세 이동 | push 전환 |
| 한도 초과 카드 "업그레이드" 탭 | SCR-023 Paywall 이동 | push 전환 |
| 풀 투 리프레시 | 브리핑 목록 갱신 | `RefreshIndicator` |
| 탭 진입 | 미읽음 배지 카운트 초기화 | 하단 탭 바 브리핑 탭 배지 제거 |

---

### 4-12. SCR-021: 브리핑 상세

#### 4-12-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-021-01 | 브리핑 카드 표시 | 유형(안심/주의) 배지, 출발 시간, 장소명, 총평 |
| F-021-02 | 상세 정보 리스트 | 영업상태/혼잡도/날씨/이동시간 개별 상태 |
| F-021-03 | CTA 분기 | 안심: "안심 확인 완료", 주의: "대안 보기", 만료: "최신 상태 조회" |
| F-021-04 | 만료 브리핑 안내 | 방문 시간 경과 시 만료 안내 배너 표시 |
| F-021-05 | 분석 이벤트 | 브리핑 열람 시 "브리핑 열람됨" + 수신-열람 간격 기록 |

#### 4-12-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 앱 바 | `AppBar` | `leading: 뒤로가기, title: "브리핑 상세"` | - |
| 브리핑 유형 배지 | `StatusBadgeWidget` (Large 40dp) | 좌측 상단 | `briefing.statusCode` |
| 출발 예정 시간 | `Text` | `--font-caption, color: --text-secondary` | `briefing.departureTime` |
| 장소명 | `Text` | `--font-h1 (22px, Bold)` | `briefing.placeName` |
| 총평 | `Text` | `--font-body1 (15px), color: --text-secondary` | `briefing.summary` |
| 만료 배너 | `Container` | `bgColor: rgba(255,59,48,0.1), icon: alert-circle` | `briefing.isExpired` |
| 상세 정보 리스트 | `Column` of `StatusDetailRow` | - | `briefing.details` |
| - 항목명 | `Text` | `--font-body2, width: 80dp` | (영업상태/혼잡도/날씨/이동시간) |
| - 값 | `Text` | `--font-body2, color: --text-primary` | `detail.value` |
| - 개별 배지 | `StatusBadgeWidget` | `size: 20dp` | `detail.statusCode` |
| 생성 시각 | `Text` | `--font-caption, color: --text-disabled` | `briefing.createdAt` |
| CTA 버튼 | `ElevatedButton` | `height: 52dp, fullWidth, bgColor: 유형별 분기` | `briefing.type` |

#### 4-12-3. 인터랙션

| 이벤트 | 동작 | 피드백 |
|--------|------|--------|
| "안심 확인 완료" 탭 | 브리핑 읽음 처리, SCR-020 복귀 | pop + 브리핑 카드 읽음 스타일 변경 |
| "대안 보기" 탭 | 유료 티어 확인 후 SCR-022 또는 SCR-023 이동 | push 전환 |
| "최신 상태 조회" 탭 | 상태 재조회 API 호출 | 버튼 스피너 → 결과 업데이트 |
| 딥링크 진입 | Push 알림 탭으로 직접 진입 | 로그인 확인 후 표시 |

---

### 4-13. SCR-022: 대안 카드 화면

#### 4-13-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-022-01 | 대안 카드 3장 표시 | 장소 사진, 상태 배지, 거리/이동시간/평점/혼잡도, 추천 이유 |
| F-022-02 | 이동시간 병기 | 500m 이상 시 도보/대중교통 이동시간 모두 표시 |
| F-022-03 | 주의 레이블 | 노랑 상태 대안에 "주의 필요" 레이블 표시 |
| F-022-04 | 장소로 변경 | "이 장소로 변경" CTA → UFR-ALTN-030 실행 |
| F-022-05 | 기존 일정 유지 | 세컨더리 버튼 → 기존 일정 유지 (UFR-ALTN-040) |
| F-022-06 | 분석 이벤트 | 카드 노출 시 거리/평점/혼잡도 스냅샷, 채택 시 선택 순위 + 경과 시간 기록 |

#### 4-13-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 앱 바 | `AppBar` | `leading: 뒤로가기, title: "대안 추천", actions: [닫기]` | - |
| 헤더 | `Text` | `--font-body1, color: --text-secondary` | "[장소명] 대신 추천 대안 3곳" |
| 카드 리스트 | `ListView.builder` | `scrollDirection: vertical, itemCount: 3` | `alternativesProvider` |
| 대안 카드 | `AlternativeCard` (커스텀) | `bgColor: --bg-card, radius: 12dp` | `AlternativeModel` |
| - 장소 사진 | `CachedNetworkImage` | `height: 160dp, fit: BoxFit.cover` | `alternative.photoUrl` |
| - 그라디언트 오버레이 | `Container` + `LinearGradient` | `transparent → rgba(0,0,0,0.7)` | - |
| - 상태 배지 | `StatusBadgeWidget` | `size: 24dp, position: top-left (Positioned)` | `alternative.statusCode` |
| - 주의 레이블 | `Chip` | `bgColor: rgba(255,214,10,0.2), textColor: --status-yellow` | `alternative.isWarning` |
| - 장소명 | `Text` | `--font-h2 (18px, SemiBold)` | `alternative.name` |
| - 거리/이동시간 | `Text` | `--font-body2, color: --text-secondary` | `alternative.distance, travelTime` |
| - 평점 | `Row` (star 아이콘 + Text) | `color: --accent-amber` | `alternative.rating` |
| - 추천 이유 | `Text` | `--font-caption (12px), color: --text-secondary` | `alternative.reason` |
| - "이 장소로 변경" | `ElevatedButton` | `height: 48dp, bgColor: --accent-red, fullWidth` | - |
| "기존 일정 유지" | `OutlinedButton` | `height: 48dp, fullWidth, borderColor: --bg-input` | - |

#### 4-13-3. 인터랙션

| 이벤트 | 동작 | 애니메이션 / 피드백 |
|--------|------|-------------------|
| "이 장소로 변경" 탭 | API 호출 (UFR-ALTN-030) | 버튼 스피너 → SCR-016 교체 결과 이동 |
| "기존 일정 유지" 탭 | 화면 닫힘, 일정 유지 (UFR-ALTN-040) | pop + "기존 일정을 유지합니다" 토스트 |
| 닫기 버튼 | 화면 닫힘 | pop |
| 카드 스크롤 | 자연스러운 세로 스크롤 | BouncingScrollPhysics |

---

### 4-14. SCR-023: Paywall

#### 4-14-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-023-01 | 대안 카드 미리보기 | 블러 처리된 대안 카드 이미지 표시 |
| F-023-02 | 플랜 비교 | Trip Pass(건당) vs Pro(월 구독) 혜택 비교 |
| F-023-03 | 인앱 결제 | `in_app_purchase` 패키지, Apple IAP / Google Play |
| F-023-04 | 현재 플랜 표시 | Free 플랜 현황 |
| F-023-05 | 분석 이벤트 | Paywall 노출 이벤트 기록 |

#### 4-14-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 앱 바 | `AppBar` | `actions: [닫기]` | - |
| 미리보기 이미지 | `ImageFiltered` (BackdropFilter) | `sigmaX: 10, sigmaY: 10, height: 180dp` | 대안 카드 예시 이미지 |
| 가치 제안 제목 | `Text` | `--font-h1 (22px, Bold)` | "대안 추천은 유료 기능입니다" |
| 부제 | `Text` | `--font-body2, color: --text-secondary` | - |
| Trip Pass 카드 | `Container` | `gradient: --accent-purple 135deg, radius: 16dp, padding: 20dp` | `productProvider` |
| - 플랜명/가격 | `Text` | `--font-h2 / --font-h1, color: white` | `product.title / product.price` |
| - 혜택 리스트 | `Column` of `Row` (dot + Text) | `--font-body2, color: white` | `product.features` |
| - CTA | `ElevatedButton` | `height: 52dp, bgColor: white, textColor: --accent-purple` | "Trip Pass 시작" |
| Pro 카드 | `Container` | `bgColor: --bg-card, border: 1dp solid --accent-purple, radius: 16dp` | `productProvider` |
| - CTA (세컨더리) | `OutlinedButton` | `height: 48dp, borderColor: --accent-purple, textColor: --accent-purple` | "Pro 시작" |
| Free 플랜 안내 | `Text` | `--font-caption, color: --text-secondary, textAlign: center` | "Free 플랜: 1일 1회 브리핑" |
| 구매 복원 | `TextButton` | `--font-caption, color: --text-secondary` | "구매 복원" |

#### 4-14-3. 인터랙션

| 이벤트 | 동작 | 피드백 |
|--------|------|--------|
| "Trip Pass 시작" 탭 | Apple IAP / Google Play 결제 시트 표시 | OS 네이티브 결제 UI |
| "Pro 시작" 탭 | 구독 결제 시트 표시 | OS 네이티브 결제 UI |
| 결제 성공 | 구독 활성화, 대안 카드 화면으로 복귀 | "구독이 활성화되었습니다" 토스트 |
| 결제 실패 | 에러 다이얼로그 | "결제에 실패했습니다. 다시 시도해주세요" |
| "구매 복원" 탭 | `in_app_purchase.restorePurchases()` | 복원 완료 토스트 |

---

### 4-15. SCR-030: 마이페이지

#### 4-15-1. 상세 기능

| 기능 ID | 기능명 | 설명 |
|---------|--------|------|
| F-030-01 | 프로필 표시 | 소셜 계정 프로필 이미지, 닉네임, 이메일, 현재 플랜 |
| F-030-02 | 구독 관리 진입 | SCR-031 이동 |
| F-030-03 | 알림 설정 진입 | SCR-032 이동 |
| F-030-04 | 위치정보 동의 관리 | SCR-033 이동 |
| F-030-05 | 온보딩 다시 보기 | 온보딩 완료 플래그 초기화 후 SCR-002 이동 |
| F-030-06 | 로그아웃 | 로컬 토큰 삭제, SCR-001 이동 |
| F-030-07 | 회원 탈퇴 | 확인 다이얼로그 → 데이터 삭제 → SCR-001 이동 |

#### 4-15-2. UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 앱 바 | `SliverAppBar` | `title: "마이페이지", pinned: true` | - |
| 프로필 섹션 | `Container` | `bgColor: --bg-card, radius: 12dp, padding: 16dp` | `userProvider` |
| - 프로필 이미지 | `CircleAvatar` + `CachedNetworkImage` | `radius: 32dp` | `user.photoUrl` |
| - 닉네임 | `Text` | `--font-h2 (18px, SemiBold)` | `user.displayName` |
| - 이메일 | `Text` | `--font-caption, color: --text-secondary` | `user.email` |
| - 플랜 뱃지 | `Chip` | `bgColor: --accent-purple (유료) / --bg-input (Free)` | `user.subscriptionTier` |
| 메뉴 리스트 | `Column` of `SettingsMenuTile` | `bgColor: --bg-card, radius: 12dp` | - |
| 메뉴 아이템 | `ListTile` | `minHeight: 56dp, trailing: chevron-right` | - |
| 로그아웃 버튼 | `ListTile` | `textColor: --text-primary` | - |
| 탈퇴 버튼 | `ListTile` | `textColor: --status-red` | - |
| 앱 버전 | `ListTile` | `trailing: "v1.0.0", color: --text-disabled` | `packageInfo.version` |

#### 4-15-3. 인터랙션

| 이벤트 | 동작 | 피드백 |
|--------|------|--------|
| 구독 관리 탭 | SCR-031 push 전환 | - |
| 로그아웃 탭 | 즉시 실행, 토큰 삭제, SCR-001 이동 | 확인 다이얼로그 (선택사항) |
| 회원 탈퇴 탭 | 확인 다이얼로그 표시 | "탈퇴 시 모든 데이터가 삭제됩니다. 계속하시겠습니까?" |
| 탈퇴 확인 | API 호출, 토큰 삭제, SCR-001 이동 | 처리 중 로딩 |

---

### 4-16. BS-001: 장소 상세 바텀시트

#### 4-16-1. 상세 기능 / UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 바텀시트 | `DraggableScrollableSheet` | `initialSize: 0.6, maxSize: 0.9, snap: [0.6, 0.9]` | - |
| 핸들 바 | `Container` | `width: 36dp, height: 4dp, color: --text-disabled, top: 12dp` | - |
| 장소명 + 닫기 | `Row` | `--font-h2, trailingIcon: X` | `place.name` |
| 장소 사진 | `CachedNetworkImage` | `height: 180dp, radius: 12dp, fit: cover` | `place.photoUrl` |
| 카테고리/평점 | `Row` | Chip + Star 아이콘 | `place.category, place.rating` |
| 영업시간 | `Text` | `--font-body2` | `place.openingHours` |
| 미니맵 | `GoogleMap` (플러터 위젯) | `height: 120dp, disabled interaction` | `place.location` |
| CTA 버튼 | `ElevatedButton` | `height: 52dp, bgColor: --accent-red, fullWidth` | "일정에 추가" |

#### 4-16-2. 인터랙션

| 이벤트 | 동작 |
|--------|------|
| 핸들 바 드래그 | 절반(60%) ↔ 전체(90%) 높이 전환 |
| 외부 탭 / 하향 스와이프 | 바텀시트 닫기 |
| "일정에 추가" 탭 | SCR-015 시간 지정 화면 이동, 바텀시트 닫기 |

---

### 4-17. BS-002: 상태 상세 바텀시트

#### 4-17-1. 상세 기능 / UI 구성요소

| 위젯 | 유형 | 속성 | 데이터 바인딩 |
|------|------|------|-------------|
| 바텀시트 | `BottomSheet` | `backgroundColor: --bg-card, topRadius: 20dp` | - |
| 핸들 바 | `Container` | `width: 36dp, height: 4dp` | - |
| 장소명 + 닫기 | `Row` | `--font-h2, trailingIcon: X` | `place.name` |
| 종합 배지 (Large) | `StatusBadgeWidget` | `size: 40dp` | `place.overallStatus` |
| 종합 상태 레이블 | `Text` | `--font-h3` | "안심" / "주의" / "위험" / "데이터 미확인" |
| 상세 리스트 | `Column` of `StatusDetailRow` | 구분선 포함 | `place.statusDetails` |
| - 영업상태 | 항목 행 | `leading: storefront 아이콘` | `details.businessStatus` |
| - 혼잡도 | 항목 행 | `leading: people 아이콘` | `details.crowdedness` |
| - 날씨 | 항목 행 | `leading: cloud 아이콘` | `details.weather` |
| - 이동시간 | 항목 행 (2줄) | 도보 + 대중교통 (500m 이상) | `details.travelTime` |
| 판정 사유 | `Text` | `--font-body2, color: --text-secondary, padding: 12dp` | `place.statusReason` |
| "대안 보기" CTA | `ElevatedButton` | `height: 52dp, bgColor: --accent-red` | `place.isWarning` → 표시/숨김 |

#### 4-17-2. 인터랙션

| 이벤트 | 동작 |
|--------|------|
| "대안 보기" 탭 | 바텀시트 닫기, SCR-022 또는 SCR-023 이동 |
| 초록 배지 상태 | "대안 보기" 버튼 비표시 |
| 외부 탭 / 하향 스와이프 | 바텀시트 닫기 |

---

## 5. 화면 간 전환 및 네비게이션

### 5-1. go_router 라우팅 테이블

```
/splash                                → SCR-000 (SplashScreen)
/login                                 → SCR-001 (LoginScreen)
/onboarding                            → SCR-002 (OnboardingScreen)

/ (ShellRoute: BottomNavigationBar)
  /trips                               → SCR-010 (TripListScreen)       [TAB 0]
  /trips/new                           → SCR-011 (TripCreateScreen)
  /trips/new/permissions               → SCR-012 (PermissionsScreen)
  /trips/:tripId/schedule              → SCR-013 (ScheduleScreen)
    ?date=YYYY-MM-DD                   → 특정 날짜 포커스
  /trips/:tripId/places/search         → SCR-014 (PlaceSearchScreen)
  /trips/:tripId/places/schedule       → SCR-015 (PlaceTimeScreen)
    ?placeId=:placeId
  /trips/:tripId/schedule/replaced     → SCR-016 (ReplacedResultScreen)

  /briefings                           → SCR-020 (BriefingListScreen)   [TAB 1]
  /briefings/:briefingId               → SCR-021 (BriefingDetailScreen)
  /briefings/:briefingId/alternatives  → SCR-022 (AlternativeCardScreen)

  /paywall                             → SCR-023 (PaywallScreen)
    ?source=briefing|badge|limit       → 진입 출처

  /my                                  → SCR-030 (MyPageScreen)          [TAB 2]
  /my/subscription                     → SCR-031 (SubscriptionScreen)
  /my/notifications                    → SCR-032 (NotificationSettingsScreen)
  /my/location-consent                 → SCR-033 (LocationConsentScreen)
```

**ShellRoute 구성**: `go_router`의 `ShellRoute`를 사용해 하단 탭 바를 유지하면서 중첩 네비게이션 지원.

**리다이렉트 규칙**:

| 조건 | 리다이렉트 목적지 |
|------|----------------|
| 미인증 + 보호된 경로 접근 | `/login?redirect={originalPath}` |
| 인증 완료 + `/login` 접근 | `/trips` |
| 온보딩 미완료 + 인증 완료 | `/onboarding` |

### 5-2. 딥링크 지원

| 딥링크 URI | 처리 화면 | 조건 |
|------------|----------|------|
| `travelplanner://briefings/{briefingId}` | SCR-021 브리핑 상세 | Push 알림 (브리핑 수신) |
| `travelplanner://status/{tripId}/{placeId}` | SCR-013 일정표 + BS-002 상태 상세 자동 열기 | Push 알림 (상태 악화) |
| `travelplanner://paywall?source=limit` | SCR-023 Paywall | 한도 초과 안내 |

**딥링크 처리 흐름**:
1. `go_router`의 `redirect` 콜백에서 인증 상태 확인
2. 미인증 시 로그인 화면 이동 후 `state.extra`에 원래 경로 저장
3. 로그인 완료 후 원래 딥링크 경로로 자동 이동

### 5-3. 바텀시트 계층 구조

```
SCR-013 (일정표)
    └── BS-002 (상태 상세) — 1단계
            └── SCR-022 (대안 카드) — push 전환 (바텀시트 닫힘)

SCR-014 (장소 검색)
    └── BS-001 (장소 상세) — 1단계
            └── SCR-015 (시간 지정) — push 전환 (바텀시트 닫힘)
```

> 바텀시트 위에 바텀시트 중첩은 허용하지 않는다. 추가 화면 전환이 필요한 경우 기존 바텀시트를 닫고 push 전환을 사용한다.

---

## 6. 반응형 설계 전략

### 6-1. 브레이크포인트 정의

| 단계 | 범위 | 대상 디바이스 | 컬럼 | 좌우 패딩 |
|------|------|------------|------|:--------:|
| Mobile S | 320dp ~ 374dp | iPhone SE, 소형 Android | 1 | 16dp |
| **Mobile M (기준)** | **375dp ~ 428dp** | **iPhone 13/14/15, Galaxy S 시리즈** | **1** | **16dp** |
| Mobile L | 429dp ~ 599dp | iPhone Plus/Max, 대형 Android | 1 | 20dp |
| Tablet | 600dp ~ 839dp | iPad Mini, 소형 태블릿 | 2 | 24dp |
| Large Tablet | 840dp 이상 | iPad Air/Pro, 폴더블 펼침 | 3 | 32dp |

### 6-2. Flutter 적응형 UI 구현 전략

**LayoutBuilder 기반 분기**:

| 조건 | 레이아웃 변화 |
|------|-------------|
| `constraints.maxWidth < 600dp` | 1열 레이아웃, 풀 와이드 카드, 하단 탭 바 |
| `600dp <= constraints.maxWidth < 840dp` | 2열 그리드 카드, 하단 탭 바 |
| `constraints.maxWidth >= 840dp` | 3열 그리드, 사이드 네비게이션 레일(NavigationRail) |

**MediaQuery 활용**:

| 항목 | 처리 방식 |
|------|---------|
| 세이프 에어리어 | `MediaQuery.of(context).padding` — 상단 StatusBar, 하단 HomeIndicator 여백 자동 적용 |
| 텍스트 스케일 | `MediaQuery.of(context).textScaleFactor` — 최소 1.0, 최대 1.3으로 클램핑 |
| 가로/세로 모드 | `MediaQuery.of(context).orientation` — 가로 모드 시 콘텐츠 영역 확장, 탭 바 유지 |
| 폴더블 힌지 | `MediaQuery.of(context).displayFeatures` — 힌지 영역 콘텐츠 배치 회피 |

**일정표 타임라인 반응형**:

| 모바일 (< 600dp) | 태블릿 (>= 600dp) |
|-----------------|-----------------|
| 풀 와이드 단일 컬럼 타임라인 | 2열: 좌측 날짜 네비게이션 + 우측 타임라인 |
| 하단 "장소 추가" 버튼 고정 | 우측 패널에 장소 검색 내장 |

**대안 카드 반응형**:

| 모바일 | 태블릿 |
|--------|--------|
| 세로 스크롤 풀 와이드 카드 | 2열 그리드 카드 |

---

## 7. 접근성 보장 방안

### 7-1. Semantics 위젯 적용

| 컴포넌트 | Semantics 속성 | 값 |
|---------|--------------|-----|
| 상태 배지 (초록) | `label`, `hint` | "상태: 정상", "모든 항목 양호" |
| 상태 배지 (노랑) | `label`, `hint` | "상태: 주의", `place.statusReason` |
| 상태 배지 (빨강) | `label`, `hint` | "상태: 위험", `place.statusReason` |
| 상태 배지 (회색) | `label` | "상태: 데이터 미확인" |
| 브리핑 카드 | `label` | "[장소명] 브리핑. [유형]. [총평]" |
| 대안 카드 | `label` | "[장소명] 대안. [거리]. [평점]. [추천이유]" |
| 하단 탭 바 | `label`, `selected` | 탭명 + 활성화 여부 |
| 미읽음 배지 | `label` | "브리핑 탭, 미읽음 [N]건" |
| 스와이프 삭제 | `button`, `label` | "[장소명] 삭제" |
| 장소 추가 FAB | `label`, `hint` | "장소 추가", "새 장소를 일정에 추가합니다" |

### 7-2. 색상 + 아이콘 조합 (색약 지원)

| 상태 | 색상 | 아이콘 (Lucide) | 텍스트 레이블 | 색약 대비 방식 |
|------|------|----------------|------------|-------------|
| 정상 (초록) | `#34C759` | `check-circle` | "정상" | 체크 아이콘으로 색상 없이도 구분 |
| 주의 (노랑) | `#FFD60A` | `alert-triangle` | "주의" | 삼각형 느낌표로 색상 없이도 구분 |
| 위험 (빨강) | `#FF3B30` | `x-circle` | "위험" | X 아이콘으로 색상 없이도 구분 |
| 미확인 (회색) | `#8E8E93` | `help-circle` | "미확인" | 물음표로 색상 없이도 구분 |

> WCAG 1.4.1 (색상 사용): 색상만으로 정보를 전달하지 않으며, 반드시 아이콘 + 텍스트 레이블을 함께 제공한다.

### 7-3. WCAG 2.1 AA 준수 체크리스트

| # | 기준 | 적용 내용 | 준수 방법 |
|:-:|------|---------|---------|
| 1 | 1.1.1 텍스트 대안 | 장소 사진 | `Semantics(label: "[장소명] 사진")` |
| 2 | 1.3.1 정보 및 관계 | 브리핑 카드 구조 | `Semantics(header: true)` for 섹션 타이틀 |
| 3 | 1.4.1 색상 사용 | 상태 배지 | 색상 + 아이콘 + 텍스트 조합 |
| 4 | 1.4.3 명도 대비 | 모든 텍스트 | `--text-primary` 18.3:1, `--text-secondary` 5.1:1 |
| 5 | 1.4.11 비텍스트 대비 | UI 컴포넌트 | 모든 상태 배지 >= 4.1:1 on `--bg-card` |
| 6 | 2.1.1 키보드 | 외부 키보드 | `Focus`, `FocusTraversalGroup`으로 탭 순서 관리 |
| 7 | 2.4.3 포커스 순서 | 폼 화면 | 논리적 탭 순서: 여행명 → 시작일 → 종료일 → 도시 → CTA |
| 8 | 2.4.7 포커스 표시 | 포커스 인디케이터 | `FocusDecoration`: `2dp solid --accent-blue, offset 2dp` |
| 9 | 3.3.1 오류 인식 | 폼 입력 에러 | `--status-red` 보더 + 에러 텍스트 (색상+텍스트 조합) |
| 10 | 4.1.3 상태 메시지 | 배지 상태 변경 | `SemanticsService.announce()` for 실시간 상태 변경 |

### 7-4. 터치 타겟 최소 크기

모든 탭 가능 요소에 `MinimumSizeButton` 또는 `SizedBox(width: 48, height: 48)` wrapping 적용.

| 컴포넌트 | 실제 시각 크기 | 터치 타겟 크기 |
|---------|-------------|-------------|
| 상태 배지 (인라인) | 24dp | 48dp x 48dp (패딩 확장) |
| 탭 바 아이콘 | 24dp | 전체 탭 영역 (최소 44dp) |
| 뒤로가기 버튼 | 24dp | 44dp x 44dp |
| 스와이프 삭제 아이콘 | 24dp | 72dp x 72dp 영역 |
| 바텀시트 핸들 | 36dp x 4dp | 36dp x 20dp (패딩 확장) |

---

## 8. 성능 최적화 방안

### 8-1. 렌더링 최적화

| 기법 | 적용 위치 | 설명 |
|------|---------|------|
| `ListView.builder` / `SliverList` | 일정표, 브리핑 목록, 장소 검색 결과 | 화면 밖 아이템은 렌더링하지 않는 가상화 리스트 |
| `const` 위젯 | 정적 UI 컴포넌트 (앱 바 타이틀, 아이콘, 고정 텍스트) | 불필요한 위젯 재빌드 방지 |
| `RepaintBoundary` | 상태 배지, 미읽음 뱃지 | 주변 위젯 변경 시 해당 영역만 재페인트 |
| `AutomaticKeepAliveClientMixin` | 각 탭 화면 | 탭 전환 시 스크롤 위치 및 상태 유지 |
| `Sliver` 계열 위젯 | 스크롤 가능 화면 | `SliverAppBar` + `SliverList` 조합으로 스크롤 성능 향상 |

### 8-2. 이미지 최적화

| 기법 | 패키지 | 설명 |
|------|--------|------|
| 이미지 캐싱 | `cached_network_image` | 디스크 캐시 + 메모리 캐시. 네트워크 재요청 최소화 |
| 스켈레톤 플레이스홀더 | `cached_network_image`의 `placeholder` | 이미지 로딩 중 스켈레톤 UI 표시 |
| 에러 위젯 | `cached_network_image`의 `errorWidget` | 이미지 로드 실패 시 아이콘 대체 표시 |
| WebP 포맷 | 백엔드 이미지 서빙 | PNG/JPEG 대비 25~35% 용량 절감 |
| 적응형 해상도 | `ResizeImage` 래퍼 | 화면 크기에 맞는 해상도만 디코딩 (메모리 절약) |

### 8-3. 상태 관리 최적화 (flutter_riverpod)

| 기법 | 설명 | 적용 사례 |
|------|------|---------|
| `select` 기반 세밀한 구독 | Provider 전체가 아닌 필요한 필드만 감시 | `ref.watch(scheduleProvider.select((s) => s.places))` |
| `AsyncNotifier` / `FutureProvider` | 비동기 상태 캐싱, 자동 로딩/에러 상태 관리 | API 호출 상태 |
| `family` 파라미터 | 동일한 Provider를 tripId별로 인스턴스 분리 | `scheduleProvider(tripId)` |
| `autoDispose` | 화면 이탈 시 Provider 자동 해제 | 장소 검색 결과 (`placeSearchProvider.autoDispose`) |
| `keepAlive` | 탭 화면 전환 시 상태 유지 | `tripListProvider`, `briefingListProvider` |
| `Riverpod` 코드 생성 | `@riverpod` 어노테이션으로 타입 안전 Provider 생성 | riverpod_generator 사용 |

### 8-4. 네트워크 최적화

| 기법 | 설명 |
|------|------|
| Dio 인터셉터 | 토큰 자동 갱신 (401 응답 시 refresh token으로 재시도), 공통 에러 핸들링 |
| 요청 디바운싱 | 장소 검색: 500ms 디바운스 후 API 호출 |
| 페이지네이션 | 브리핑 목록: `cursor` 기반 무한 스크롤 |
| 오프라인 캐싱 | 일정표 데이터를 `flutter_secure_storage` 또는 `hive`에 캐싱, 네트워크 없을 때 캐시 표시 |
| 배지 데이터 폴링 | 15분 주기 백그라운드 갱신 (`firebase_messaging` + 서버 Push 방식 우선, 폴링은 폴백) |

### 8-5. 애니메이션 성능

| 규칙 | 설명 |
|------|------|
| `AnimationController` 해제 | `dispose()`에서 반드시 `controller.dispose()` 호출 |
| `addPostFrameCallback` | 빌드 완료 후 애니메이션 시작 (초기 렌더링 블로킹 방지) |
| 복잡한 애니메이션 격리 | `RepaintBoundary`로 감싸 GPU 합성 레이어 분리 |
| `InteractiveViewer` 비사용 | 지도 미니맵은 정적 이미지로 대체 (가능한 경우) |
| 60fps 목표 | `flutter_driver`로 프레임 타이밍 측정, 16ms 초과 프레임 제거 |

---

## 9. 컴포넌트-디자인 토큰 매핑 (Flutter ThemeExtension)

### 9-1. ThemeExtension 토큰 정의

Flutter `ThemeExtension`으로 style-guide.md의 CSS 변수를 Dart 타입으로 변환.

| CSS 토큰 | Dart 이름 | 타입 | 값 |
|----------|----------|------|-----|
| `--bg-primary` | `AppColors.bgPrimary` | `Color` | `Color(0xFF0A0A0A)` |
| `--bg-card` | `AppColors.bgCard` | `Color` | `Color(0xFF1A1A1A)` |
| `--bg-input` | `AppColors.bgInput` | `Color` | `Color(0xFF242424)` |
| `--bg-hover` | `AppColors.bgHover` | `Color` | `Color(0xFF2A2A2A)` |
| `--text-primary` | `AppColors.textPrimary` | `Color` | `Color(0xFFF0F0F0)` |
| `--text-secondary` | `AppColors.textSecondary` | `Color` | `Color(0xFF8A8A8A)` |
| `--text-disabled` | `AppColors.textDisabled` | `Color` | `Color(0xFF555555)` |
| `--status-green` | `AppColors.statusGreen` | `Color` | `Color(0xFF34C759)` |
| `--status-yellow` | `AppColors.statusYellow` | `Color` | `Color(0xFFFFD60A)` |
| `--status-red` | `AppColors.statusRed` | `Color` | `Color(0xFFFF3B30)` |
| `--status-gray` | `AppColors.statusGray` | `Color` | `Color(0xFF8E8E93)` |
| `--accent-red` | `AppColors.accentRed` | `Color` | `Color(0xFFFF2D2D)` |
| `--accent-amber` | `AppColors.accentAmber` | `Color` | `Color(0xFFFFB830)` |
| `--accent-purple` | `AppColors.accentPurple` | `Color` | `Color(0xFF7B3FE0)` |
| `--accent-blue` | `AppColors.accentBlue` | `Color` | `Color(0xFF0A84FF)` |
| `--anim-fast` | `AppDurations.fast` | `Duration` | `Duration(milliseconds: 150)` |
| `--anim-normal` | `AppDurations.normal` | `Duration` | `Duration(milliseconds: 250)` |
| `--anim-slow` | `AppDurations.slow` | `Duration` | `Duration(milliseconds: 350)` |
| `--anim-spring` | `AppDurations.spring` | `Duration` | `Duration(milliseconds: 400)` |
| `--space-base` | `AppSpacing.base` | `double` | `16.0` |
| `--radius-sm` | `AppRadius.sm` | `double` | `8.0` |
| `--radius-md` | `AppRadius.md` | `double` | `12.0` |
| `--radius-lg` | `AppRadius.lg` | `double` | `16.0` |

### 9-2. 커스텀 위젯 카탈로그

| 위젯명 | 설명 | 주요 Props |
|--------|------|-----------|
| `StatusBadgeWidget` | 상태 배지 (4종) | `statusCode: StatusCode, size: BadgeSize` |
| `SchedulePlaceCard` | 일정표 장소 카드 | `place: PlaceScheduleModel, onTap: VoidCallback` |
| `AlternativeCard` | 대안 카드 (미디어+정보) | `alternative: AlternativeModel, onSelect: VoidCallback` |
| `BriefingCard` | 브리핑 목록 카드 | `briefing: BriefingModel, onTap: VoidCallback` |
| `TimelineDivider` | 이동시간 연결선 | `travelTime: String, isDashed: bool` |
| `StatusDetailRow` | 상태 상세 항목 행 | `label: String, value: String, statusCode: StatusCode` |
| `SkeletonWidget` | 스켈레톤 로딩 | `width: double, height: double, radius: double` |
| `EmptyStateWidget` | 빈 상태 | `icon: IconData, title: String, description: String, cta: Widget?` |
| `AppBottomSheet` | 공통 바텀시트 래퍼 | `child: Widget, initialSize: double, maxSize: double` |
| `PlanCard` | Paywall 플랜 카드 | `plan: SubscriptionPlan, isHighlighted: bool, onTap: VoidCallback` |

---

## 10. 검증 매트릭스

### 10-1. 화면-요구사항 추적표

| 화면 | 관련 UFR | 우선순위 | Flutter 위젯 구현 특이사항 |
|------|---------|:-------:|--------------------------|
| SCR-000 스플래시 | - | M | `SplashScreen` → `go_router` redirect |
| SCR-001 소셜 로그인 | UFR-AUTH-010 | M | `google_sign_in`, `sign_in_with_apple` |
| SCR-002 온보딩 | UFR-SCHD-005 | M | `PageView` + 인디케이터 도트 |
| SCR-010 여행 목록 | UFR-SCHD-050 | M | `SliverList` + FAB |
| SCR-011 여행 생성 | UFR-SCHD-010 | M | `TextFormField` + 날짜 피커 바텀시트 |
| SCR-012 권한 동의 | UFR-SCHD-010, NFR-SEC-020 | M | `geolocator`, Push 권한 순서 처리 |
| SCR-013 일정표 | UFR-SCHD-050, UFR-MNTR-030 | M | `ReorderableListView`, `Dismissible`, 배지 바운스 |
| SCR-014 장소 검색 | UFR-SCHD-020, UFR-PLCE-010 | M | `SearchBar` + 디바운싱 |
| SCR-015 시간 지정 | UFR-SCHD-030 | M | 드럼 피커, IANA 타임존 (`intl`) |
| SCR-016 교체 결과 | UFR-SCHD-040 | M | 결과 카드 스케일 인 |
| BS-001 장소 상세 | UFR-PLCE-020 | S | `DraggableScrollableSheet` |
| BS-002 상태 상세 | UFR-MNTR-040 | M | `BottomSheet` + CTA 조건부 표시 |
| SCR-020 브리핑 목록 | UFR-BRIF-060 | C | `SliverList` + 한도 초과 인라인 카드 |
| SCR-021 브리핑 상세 | UFR-BRIF-050 | M | 딥링크 진입, CTA 유형 분기 |
| SCR-022 대안 카드 | UFR-ALTN-020, UFR-ALTN-030 | M | `CachedNetworkImage` + 그라디언트 오버레이 |
| SCR-023 Paywall | UFR-ALTN-050, UFR-PAY-010 | M | `in_app_purchase`, 블러 이미지 |
| SCR-030 마이페이지 | UFR-AUTH-010, UFR-PAY-010 | M | `SettingsMenuTile` 리스트 |
| SCR-031 구독 관리 | UFR-PAY-010 | M | 구독 상태 표시 |
| SCR-032 알림 설정 | UFR-MNTR-050 | M | `Switch` 토글 |
| SCR-033 위치정보 관리 | NFR-SEC-020 | M | 동의 현황 + 철회 플로우 |

### 10-2. 설계 원칙 준수 검증

| 원칙 | 검증 항목 | 적용 화면 |
|------|---------|---------|
| 한눈에 파악 | 상태 배지 4색+아이콘 조합, 일정표 타임라인 | SCR-013, BS-002 |
| 즉시 행동 | 대안 카드 탭 1회 일정 반영, 배지 탭 → 대안 보기 | SCR-022, BS-002 |
| 맥락 우선 | 노랑/빨강 배지만 "대안 보기" CTA 표시, 만료 브리핑 분기 | BS-002, SCR-021 |
| 신뢰 투명성 | 회색 배지 "데이터 미확인", 브리핑 생성 시각 표시 | SCR-013, SCR-021 |
| 미디어 몰입 | 대안 카드 풀 블리드 장소 사진 + 그라디언트 오버레이 | SCR-022 |

---

*설계 기준: uiux.md (도그냥, 2026-02-22), style-guide.md (도그냥, 2026-02-22)*
*플랫폼: Flutter 3.x (Dart), Material Design 3, 다크 테마 단일*
*검토 예정: 아키 (아키텍처 정합성), 가디언 (QA 체크리스트)*
