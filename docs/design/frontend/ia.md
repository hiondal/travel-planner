# 프론트엔드 정보 아키텍처 (IA) 설계서

> 작성자: 데브-프론트 (프론트엔드 개발자)
> 작성일: 2026-02-23
> 근거: UI/UX 설계서 (`docs/plan/design/uiux/uiux.md`) 2-1. 사이트맵
> 기술 스택: Flutter 3.x, go_router, flutter_riverpod

---

## 목차

1. [사이트맵 및 라우트 정의](#1-사이트맵-및-라우트-정의)
   - 1.1 페이지 계층 구조
   - 1.2 go_router 라우트 정의 표
   - 1.3 네비게이션 흐름
2. [프로젝트 구조 설계](#2-프로젝트-구조-설계)
   - 2.1 최상위 디렉토리 트리
   - 2.2 core 레이어 상세
   - 2.3 features 레이어 상세
   - 2.4 shared 레이어 상세
3. [레이어 책임 정의](#3-레이어-책임-정의)
4. [의존성 규칙](#4-의존성-규칙)

---

## 1. 사이트맵 및 라우트 정의

### 1.1 페이지 계층 구조

```
travel-planner (앱 루트)
│
├── /splash                          [SplashPage]
│   └── 앱 진입 시 토큰 유효성 확인 후 분기
│
├── /auth                            [AuthShellPage]
│   ├── /auth/login                  [LoginPage]
│   │   ├── Google 소셜 로그인        (UFR-AUTH-010)
│   │   └── Apple 소셜 로그인         (UFR-AUTH-010)
│   └── /auth/onboarding             [OnboardingPage]
│       ├── Step 1: 상태 배지 소개    (UFR-SCHD-005)
│       ├── Step 2: 출발 전 브리핑 소개
│       └── Step 3: 대안 카드 소개
│
└── /main                            [MainShellPage] ← BottomTabBar 쉘
    │
    ├── TAB 1: /schedule             [ScheduleTab]
    │   ├── /schedule                [TripListPage]        여행 목록 (메인)
    │   ├── /schedule/new            [TripCreatePage]      여행 일정 생성
    │   │   └── /schedule/new/permission  [PermissionPage] 권한 동의 (첫 일정)
    │   ├── /schedule/:tripId        [ScheduleDetailPage]  일정표 (일별 타임라인)
    │   │   ├── /schedule/:tripId/place/search
    │   │   │                        [PlaceSearchPage]     장소 검색
    │   │   │   └── /schedule/:tripId/place/search/time
    │   │   │                        [PlaceTimePickerPage] 시간 지정
    │   │   └── [StatusDetailSheet]  상태 상세 (바텀시트, 라우트 아님)
    │   │       └── → /briefing/alternative/:briefingId
    │   └── /schedule/:tripId/result [ScheduleChangeResultPage] 장소 교체 결과
    │
    ├── TAB 2: /briefing             [BriefingTab]
    │   ├── /briefing                [BriefingListPage]    브리핑 목록
    │   ├── /briefing/:briefingId    [BriefingDetailPage]  브리핑 상세
    │   │   ├── 안심 브리핑 변형      (UFR-BRIF-050 시나리오1)
    │   │   ├── 주의 브리핑 변형      (UFR-BRIF-050 시나리오2)
    │   │   └── 만료 브리핑 변형      (UFR-BRIF-050 시나리오3)
    │   ├── /briefing/alternative/:briefingId
    │   │                            [AlternativeCardPage] 대안 카드 화면
    │   │   ├── 유료 티어: 카드 선택 → 일정 반영   (UFR-ALTN-030)
    │   │   └── 기존 일정 유지 (닫기)              (UFR-ALTN-040)
    │   └── /briefing/paywall        [PaywallPage]         Paywall
    │       └── → /payment/checkout
    │
    └── TAB 3: /profile              [ProfileTab]
        ├── /profile                 [ProfilePage]         마이페이지
        ├── /profile/subscription    [SubscriptionPage]    구독 관리
        │   └── → /payment/checkout
        ├── /profile/notifications   [NotificationSettingsPage] 알림 설정
        ├── /profile/location        [LocationConsentPage] 위치정보 동의 관리
        └── /profile/onboarding      → /auth/onboarding (재실행)
│
└── /payment/checkout                [PaymentCheckoutPage] 결제 화면
    ├── 결제 성공                     [PaymentSuccessPage]
    └── 결제 실패 (인라인 처리)
```

---

### 1.2 go_router 라우트 정의 표

#### 인증/온보딩 라우트

| path | name | page | params | 비고 |
|------|------|------|--------|------|
| `/splash` | `splash` | `SplashPage` | - | 앱 진입점, 토큰 확인 후 리다이렉트 |
| `/auth/login` | `login` | `LoginPage` | - | 미인증 리다이렉트 대상 |
| `/auth/onboarding` | `onboarding` | `OnboardingPage` | `?step=1` (query) | 최초 1회 / 설정에서 재진입 |

#### TAB 1: 일정 라우트

| path | name | page | params | 비고 |
|------|------|------|--------|------|
| `/schedule` | `tripList` | `TripListPage` | - | BottomTab 루트 |
| `/schedule/new` | `tripCreate` | `TripCreatePage` | - | |
| `/schedule/new/permission` | `permission` | `PermissionPage` | - | 첫 일정 등록 시 |
| `/schedule/:tripId` | `scheduleDetail` | `ScheduleDetailPage` | `tripId` (path) | |
| `/schedule/:tripId/place/search` | `placeSearch` | `PlaceSearchPage` | `tripId` (path) | |
| `/schedule/:tripId/place/search/time` | `placeTimePicker` | `PlaceTimePickerPage` | `tripId` (path), `placeId` (query) | |
| `/schedule/:tripId/result` | `scheduleChangeResult` | `ScheduleChangeResultPage` | `tripId` (path), `alternativeId` (query) | |

#### TAB 2: 브리핑 라우트

| path | name | page | params | 비고 |
|------|------|------|--------|------|
| `/briefing` | `briefingList` | `BriefingListPage` | - | BottomTab 루트 |
| `/briefing/:briefingId` | `briefingDetail` | `BriefingDetailPage` | `briefingId` (path) | |
| `/briefing/alternative/:briefingId` | `alternativeCard` | `AlternativeCardPage` | `briefingId` (path) | |
| `/briefing/paywall` | `paywall` | `PaywallPage` | `?from=alternative` (query) | 진입 경로 추적 |

#### TAB 3: 마이페이지 라우트

| path | name | page | params | 비고 |
|------|------|------|--------|------|
| `/profile` | `profile` | `ProfilePage` | - | BottomTab 루트 |
| `/profile/subscription` | `subscription` | `SubscriptionPage` | - | |
| `/profile/notifications` | `notificationSettings` | `NotificationSettingsPage` | - | |
| `/profile/location` | `locationConsent` | `LocationConsentPage` | - | |

#### 결제 라우트

| path | name | page | params | 비고 |
|------|------|------|--------|------|
| `/payment/checkout` | `paymentCheckout` | `PaymentCheckoutPage` | `?plan=trip_pass\|pro` (query) | 전역 라우트 (탭 외부) |
| `/payment/success` | `paymentSuccess` | `PaymentSuccessPage` | `?plan=trip_pass\|pro` (query) | |

#### 바텀시트 (라우트 미사용, 오버레이 방식)

| 컴포넌트 | 진입 경로 | 비고 |
|----------|----------|------|
| `StatusDetailSheet` | 일정표 장소 카드 탭 | UFR-MNTR-040, 대안 보기 연결 |
| `PlaceDetailSheet` | 장소 검색 결과 탭 | UFR-PLCE-020, Should |
| `DatePickerSheet` | 날짜 필드 탭 | 여행 기간 내 날짜 선택 |
| `CitySelectSheet` | 도시 선택 필드 탭 | MVP 5개 도시 |

---

### 1.3 네비게이션 흐름

#### 플로우 A: 최초 진입 → 일정 등록

```
SplashPage
  ↓ (토큰 없음)
LoginPage → OnboardingPage → TripListPage(샘플 일정)
  ↓ (여행 만들기)
TripCreatePage → PermissionPage → ScheduleDetailPage(빈 일정표)
  ↓ (장소 추가)
PlaceSearchPage → PlaceTimePickerPage → ScheduleDetailPage(장소 추가됨)
```

#### 플로우 B: 배지 확인 → 대안 선택

```
ScheduleDetailPage
  ↓ (장소 카드 탭)
StatusDetailSheet (오버레이)
  ↓ (대안 보기 탭, 노랑/빨강)
AlternativeCardPage
  ↓ (이 장소로 변경)
ScheduleChangeResultPage → ScheduleDetailPage
```

#### 플로우 C: Push 알림 → 브리핑 상세

```
OS Push 알림 탭
  ↓
BriefingDetailPage
  ↓ (대안 보기, 주의 브리핑)
AlternativeCardPage
```

#### 플로우 D: Paywall → 결제

```
AlternativeCardPage (Free 티어)
  ↓
PaywallPage
  ↓ (Trip Pass / Pro 선택)
PaymentCheckoutPage → PaymentSuccessPage → 이전 화면 복귀
```

#### 딥링크 처리

| 트리거 | 딥링크 경로 | 처리 |
|--------|-----------|------|
| 안심 브리핑 Push | `/briefing/:briefingId` | BriefingDetailPage 직접 진입 |
| 주의 브리핑 Push | `/briefing/:briefingId` | BriefingDetailPage 직접 진입 |
| 상태 악화 Push | `/schedule/:tripId` | ScheduleDetailPage + StatusDetailSheet 자동 열림 |

---

## 2. 프로젝트 구조 설계

### 2.1 최상위 디렉토리 트리

```
lib/
├── main.dart                        # 앱 엔트리포인트, ProviderScope 래핑
├── app.dart                         # MaterialApp.router, go_router 주입
│
├── core/                            # 앱 전역 인프라 (feature 미포함)
│   ├── config/
│   ├── constants/
│   ├── network/
│   ├── routing/
│   ├── theme/
│   └── utils/
│
├── features/                        # 기능 단위 분리 (Vertical Slice)
│   ├── auth/
│   ├── schedule/
│   ├── briefing/
│   ├── monitoring/
│   ├── payment/
│   └── profile/
│
├── shared/                          # 복수 feature에서 공유하는 코드
│   ├── models/
│   ├── providers/
│   └── widgets/
│
└── l10n/                            # 다국어 지원
    ├── app_ko.arb
    └── app_en.arb
```

---

### 2.2 core 레이어 상세

```
core/
│
├── config/
│   ├── app_config.dart              # 환경별 설정 (dev/staging/prod)
│   ├── api_config.dart              # API 베이스 URL, 타임아웃
│   └── flavor_config.dart           # Flutter Flavor 정의
│
├── constants/
│   ├── app_colors.dart              # 디자인 토큰 색상 (--bg-primary 등)
│   ├── app_spacing.dart             # 4pt 그리드 기반 간격 상수
│   ├── app_typography.dart          # 폰트 스케일 (H1~H3, Body1~Body2, Caption)
│   ├── app_durations.dart           # 애니메이션 지속 시간 상수
│   └── app_icons.dart               # Lucide 아이콘 매핑 상수
│
├── network/
│   ├── dio_client.dart              # Dio 인스턴스 팩토리
│   ├── auth_interceptor.dart        # JWT Bearer 토큰 자동 첨부 인터셉터
│   ├── retry_interceptor.dart       # 네트워크 재시도 인터셉터
│   ├── error_interceptor.dart       # HTTP 에러 -> AppException 변환
│   └── api_exception.dart           # 앱 공통 예외 타입 정의
│
├── routing/
│   ├── app_router.dart              # GoRouter 루트 정의 (전체 라우트 트리)
│   ├── app_routes.dart              # 라우트 경로/이름 상수 (AppRoutes.splash 등)
│   ├── router_guard.dart            # 인증 상태 기반 리다이렉트 로직
│   └── tab_routes.dart              # StatefulShellRoute (BottomTabBar 쉘)
│
├── theme/
│   ├── app_theme.dart               # ThemeData (다크 테마) 조합 진입점
│   ├── color_scheme.dart            # ColorScheme 정의
│   ├── text_theme.dart              # TextTheme 정의
│   └── component_themes.dart        # BottomNavigationBar, AppBar 등 컴포넌트 테마
│
└── utils/
    ├── date_utils.dart              # 날짜 포맷, 현지 시간(IANA timezone) 변환
    ├── analytics_service.dart       # 분석 이벤트 추상화 (P18 이벤트 기록)
    ├── notification_service.dart    # FCM 토큰 관리, 딥링크 처리
    ├── secure_storage.dart          # flutter_secure_storage 래퍼 (JWT 저장)
    └── logger.dart                  # 앱 로거 (dev/prod 레벨 분리)
```

---

### 2.3 features 레이어 상세

각 feature는 **data / domain / presentation** 3계층으로 구성하며,
barrel file(`feature_name.dart`)로 외부 노출 인터페이스를 제한합니다.

#### 2.3.1 auth (인증/온보딩)

```
features/auth/
├── data/
│   ├── models/
│   │   ├── auth_token_dto.dart      # 액세스/리프레시 토큰 DTO
│   │   └── user_dto.dart            # 사용자 정보 DTO
│   ├── datasources/
│   │   └── auth_remote_datasource.dart  # POST /auth/social-login
│   └── repositories/
│       └── auth_repository_impl.dart
│
├── domain/
│   ├── entities/
│   │   └── user.dart                # 사용자 도메인 엔티티
│   └── repositories/
│       └── auth_repository.dart     # 인터페이스
│
├── presentation/
│   ├── pages/
│   │   ├── splash_page.dart
│   │   ├── login_page.dart
│   │   └── onboarding_page.dart
│   ├── widgets/
│   │   ├── social_login_button.dart  # Google / Apple 로그인 버튼
│   │   └── onboarding_step_card.dart # 온보딩 단계별 카드
│   └── providers/
│       ├── auth_provider.dart        # 인증 상태 (로그인/로그아웃)
│       └── onboarding_provider.dart  # 온보딩 완료 플래그
│
└── auth.dart                         # barrel file
```

#### 2.3.2 schedule (일정)

```
features/schedule/
├── data/
│   ├── models/
│   │   ├── trip_dto.dart             # 여행 DTO
│   │   ├── schedule_item_dto.dart    # 일정 항목 DTO
│   │   └── place_search_result_dto.dart
│   ├── datasources/
│   │   ├── schedule_remote_datasource.dart  # schedule-service API
│   │   └── place_remote_datasource.dart     # place-service API
│   └── repositories/
│       ├── trip_repository_impl.dart
│       └── schedule_repository_impl.dart
│
├── domain/
│   ├── entities/
│   │   ├── trip.dart                 # 여행 엔티티 (여행명, 기간, 도시)
│   │   ├── schedule_item.dart        # 일정 항목 (장소, 방문시간, 상태배지)
│   │   └── place.dart               # 장소 엔티티 (이름, 주소, 좌표, 평점)
│   └── repositories/
│       ├── trip_repository.dart
│       └── schedule_repository.dart
│
├── presentation/
│   ├── pages/
│   │   ├── trip_list_page.dart       # 여행 목록 (메인)
│   │   ├── trip_create_page.dart     # 여행 생성 폼
│   │   ├── permission_page.dart      # 위치/Push 권한 동의
│   │   ├── schedule_detail_page.dart # 일정표 (타임라인)
│   │   ├── place_search_page.dart    # 장소 검색
│   │   ├── place_time_picker_page.dart  # 방문 시간 지정
│   │   └── schedule_change_result_page.dart  # 교체 결과
│   ├── widgets/
│   │   ├── trip_card.dart            # 여행 목록 카드
│   │   ├── schedule_timeline.dart    # 타임라인 레이아웃
│   │   ├── schedule_item_card.dart   # 장소 카드 (배지 포함, 스와이프 삭제)
│   │   ├── travel_time_connector.dart  # 이동시간 연결선
│   │   ├── place_search_bar.dart     # 검색바 위젯
│   │   ├── place_search_result_tile.dart  # 검색 결과 리스트 아이템
│   │   ├── place_detail_sheet.dart   # 장소 상세 바텀시트 (Should)
│   │   └── city_select_sheet.dart    # 도시 선택 바텀시트
│   └── providers/
│       ├── trip_list_provider.dart   # 여행 목록 상태
│       ├── trip_create_provider.dart # 여행 생성 폼 상태
│       ├── schedule_detail_provider.dart  # 일정표 상태
│       └── place_search_provider.dart     # 장소 검색 결과
│
└── schedule.dart
```

#### 2.3.3 briefing (브리핑)

```
features/briefing/
├── data/
│   ├── models/
│   │   ├── briefing_dto.dart         # 브리핑 DTO (유형, 총평, 상세항목)
│   │   └── alternative_dto.dart      # 대안 장소 DTO
│   ├── datasources/
│   │   └── briefing_remote_datasource.dart  # briefing-service API
│   └── repositories/
│       └── briefing_repository_impl.dart
│
├── domain/
│   ├── entities/
│   │   ├── briefing.dart             # 브리핑 엔티티 (안심/주의/만료 유형)
│   │   └── alternative.dart          # 대안 장소 엔티티
│   └── repositories/
│       └── briefing_repository.dart
│
├── presentation/
│   ├── pages/
│   │   ├── briefing_list_page.dart   # 브리핑 목록
│   │   ├── briefing_detail_page.dart # 브리핑 상세 (3가지 변형)
│   │   ├── alternative_card_page.dart  # 대안 카드 화면
│   │   └── paywall_page.dart         # Paywall
│   ├── widgets/
│   │   ├── briefing_list_item.dart   # 브리핑 목록 아이템
│   │   ├── briefing_card.dart        # 브리핑 상세 카드 (유형별 변형)
│   │   ├── briefing_detail_row.dart  # 상세 항목 행 (영업상태/혼잡도/날씨/이동시간)
│   │   ├── alternative_card.dart     # 대안 카드 (사진 + 정보 + CTA)
│   │   ├── paywall_plan_card.dart    # Paywall 플랜 카드 (Trip Pass / Pro)
│   │   └── quota_exceeded_banner.dart  # 한도 초과 인라인 배너 (M4)
│   └── providers/
│       ├── briefing_list_provider.dart
│       ├── briefing_detail_provider.dart
│       └── alternative_card_provider.dart
│
└── briefing.dart
```

#### 2.3.4 monitoring (모니터링/상태배지)

```
features/monitoring/
├── data/
│   ├── models/
│   │   └── monitor_status_dto.dart   # 상태배지 DTO (초록/노랑/빨강/회색, 판정사유)
│   ├── datasources/
│   │   └── monitor_remote_datasource.dart  # monitor-service API
│   └── repositories/
│       └── monitor_repository_impl.dart
│
├── domain/
│   ├── entities/
│   │   ├── monitor_status.dart       # 상태 엔티티 (StatusLevel enum 포함)
│   │   └── status_detail.dart        # 상태 상세 (영업상태, 혼잡도, 날씨, 이동시간)
│   └── repositories/
│       └── monitor_repository.dart
│
├── presentation/
│   ├── pages/
│   │   └── (페이지 없음 - 바텀시트로만 표출)
│   ├── widgets/
│   │   └── status_detail_sheet.dart  # 상태 상세 바텀시트 (UFR-MNTR-040)
│   └── providers/
│       └── monitor_status_provider.dart  # 장소별 상태 구독
│
└── monitoring.dart
```

#### 2.3.5 payment (결제)

```
features/payment/
├── data/
│   ├── models/
│   │   ├── subscription_dto.dart     # 구독 정보 DTO (현재 플랜, 만료일)
│   │   └── payment_result_dto.dart   # 결제 결과 DTO
│   ├── datasources/
│   │   └── payment_remote_datasource.dart  # payment-service API
│   └── repositories/
│       └── payment_repository_impl.dart
│
├── domain/
│   ├── entities/
│   │   ├── subscription.dart         # 구독 엔티티 (Free/TripPass/Pro 티어)
│   │   └── plan.dart                 # 플랜 정보 (가격, 혜택)
│   └── repositories/
│       └── payment_repository.dart
│
├── presentation/
│   ├── pages/
│   │   ├── payment_checkout_page.dart  # 결제 화면 (IAP 연동)
│   │   └── payment_success_page.dart   # 결제 완료
│   ├── widgets/
│   │   └── plan_benefit_list.dart    # 플랜 혜택 목록 위젯
│   └── providers/
│       ├── subscription_provider.dart  # 현재 구독 상태
│       └── payment_provider.dart      # 결제 실행 상태
│
└── payment.dart
```

#### 2.3.6 profile (마이페이지)

```
features/profile/
├── data/
│   ├── models/
│   │   └── profile_dto.dart          # 프로필 DTO (닉네임, 이메일, 플랜)
│   ├── datasources/
│   │   └── profile_remote_datasource.dart
│   └── repositories/
│       └── profile_repository_impl.dart
│
├── domain/
│   ├── entities/
│   │   └── profile.dart              # 프로필 엔티티
│   └── repositories/
│       └── profile_repository.dart
│
├── presentation/
│   ├── pages/
│   │   ├── profile_page.dart         # 마이페이지 메인
│   │   ├── subscription_page.dart    # 구독 관리
│   │   ├── notification_settings_page.dart  # 알림 설정
│   │   └── location_consent_page.dart  # 위치정보 동의 관리
│   ├── widgets/
│   │   ├── profile_header.dart       # 프로필 이미지/닉네임/이메일/플랜
│   │   ├── settings_section.dart     # 설정 그룹 섹션 위젯
│   │   └── settings_tile.dart        # 설정 항목 타일 (> 화살표)
│   └── providers/
│       ├── profile_provider.dart     # 프로필 상태
│       └── notification_settings_provider.dart
│
└── profile.dart
```

---

### 2.4 shared 레이어 상세

```
shared/
│
├── models/
│   ├── status_level.dart             # StatusLevel enum (green/yellow/red/gray)
│   ├── subscription_tier.dart        # SubscriptionTier enum (free/tripPass/pro)
│   ├── pagination.dart               # 페이지네이션 공통 모델
│   └── result.dart                   # Result<T, E> 타입 (성공/실패 래퍼)
│
├── providers/
│   ├── app_user_provider.dart        # 전역 사용자 상태 (인증 여부, 구독 티어)
│   ├── connectivity_provider.dart    # 네트워크 연결 상태
│   └── locale_provider.dart          # 로케일/언어 설정 상태
│
└── widgets/
    ├── status_badge.dart             # 상태 배지 (4가지 레벨, 2가지 크기)
    ├── briefing_card_compact.dart    # 브리핑 컴팩트 카드 (목록용)
    ├── app_bottom_sheet.dart         # 공통 바텀시트 래퍼 (핸들바, 딤 처리)
    ├── app_button.dart               # 공통 버튼 (Primary/Secondary/Text/Danger/Premium)
    ├── app_text_field.dart           # 공통 텍스트 필드
    ├── skeleton_loader.dart          # 스켈레톤 UI 로딩 컴포넌트
    ├── empty_state_view.dart         # 빈 상태 뷰 (아이콘+타이틀+설명+CTA)
    ├── error_view.dart               # 에러 뷰 (재시도 버튼 포함)
    ├── app_toast.dart                # 토스트/스낵바 표시 유틸리티
    └── unread_badge.dart             # 미읽음 빨강 도트 배지
```

---

## 3. 레이어 책임 정의

### feature 내부 3계층

| 계층 | 디렉토리 | 책임 | 의존 가능 대상 |
|------|----------|------|--------------|
| **presentation** | `presentation/` | UI 렌더링, 사용자 입력 처리, Riverpod Provider로 상태 구독 | domain, shared |
| **domain** | `domain/` | 비즈니스 규칙, 엔티티 정의, Repository 인터페이스 선언 | (없음, 순수 Dart) |
| **data** | `data/` | API 통신, DTO↔엔티티 변환, Repository 구현 | domain, core/network |

### 레이어 간 역할

| 레이어 | 책임 요약 |
|--------|----------|
| `core/` | 앱 전역 인프라. feature에 대한 지식 없음 |
| `features/` | 기능 단위 수직 분리. 타 feature 직접 참조 금지 |
| `shared/` | 복수 feature가 공유하는 순수 UI 컴포넌트 및 공용 모델 |
| `l10n/` | ARB 기반 다국어 리소스. UI 레이어에서만 참조 |

---

## 4. 의존성 규칙

### 허용 의존 방향

```
presentation  →  domain  ←  data
     ↓              ↓
   shared          core
```

- `presentation`은 `domain`의 엔티티와 Repository 인터페이스를 통해 데이터 접근
- `data`는 `domain`의 Repository 인터페이스를 구현
- `presentation`, `data` 모두 `core`의 인프라(network, utils)를 사용
- `presentation`은 `shared/widgets`와 `shared/models`를 사용
- **feature 간 직접 의존 금지**: 공유 데이터는 `shared/` 또는 `core/`를 경유

### Riverpod Provider 배치 원칙

| Provider 유형 | 배치 위치 | 비고 |
|--------------|----------|------|
| 특정 화면 전용 상태 | `features/{feature}/presentation/providers/` | 해당 화면 파괴 시 자동 해제 |
| 복수 feature 공유 상태 | `shared/providers/` | 앱 생명주기 동안 유지 |
| 전역 인증/구독 상태 | `shared/providers/app_user_provider.dart` | 라우터 가드에서도 참조 |

### barrel file 노출 규칙

각 feature의 `{feature}.dart` barrel file은 **외부에서 필요한 인터페이스만** re-export합니다.

- 노출: domain 엔티티, presentation pages (라우터 등록용), feature 공개 Provider
- 비노출: data 계층 내부 구현, 화면 전용 위젯의 세부 구현

---

> 이 설계서는 구현 코드를 포함하지 않으며, Flutter 프로젝트 착수 시 참조하는 구조적 명세입니다.
> 세부 구현 패턴(Riverpod 코드젠 사용 여부, freezed 적용 범위 등)은 별도 코딩 컨벤션 문서에서 정의합니다.
