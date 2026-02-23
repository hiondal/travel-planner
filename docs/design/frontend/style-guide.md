# 프론트엔드 스타일 가이드 (Flutter)

> 작성자: 데브-프론트 (프론트엔드 개발자)
> 작성일: 2026-02-23
> 기반 문서: `docs/plan/design/uiux/style-guide.md` (도그냥, 2026-02-22)
> 적용 대상: travel-planner — 여행 중 실시간 일정 최적화 가이드 앱
> 플랫폼: Flutter 3.x + Material Design 3

---

## 목차

1. [브랜드 아이덴티티](#1-브랜드-아이덴티티)
2. [디자인 원칙](#2-디자인-원칙)
3. [컬러 시스템](#3-컬러-시스템)
4. [타이포그래피](#4-타이포그래피)
5. [간격 시스템](#5-간격-시스템)
6. [컴포넌트 스타일](#6-컴포넌트-스타일)
7. [반응형 브레이크포인트](#7-반응형-브레이크포인트)
8. [서비스 특화 컴포넌트](#8-서비스-특화-컴포넌트)
9. [인터랙션 패턴](#9-인터랙션-패턴)

---

## 1. 브랜드 아이덴티티

### 1-1. 디자인 컨셉

**"절제된 다크 럭셔리 + 실용적 정보 전달"**

여행 중 예기치 못한 상황에서 사용자가 패닉 없이 즉각 판단하고 행동할 수 있도록 돕는다.
화면은 데코레이션을 최소화하고, 여행지 사진과 상태 정보가 전면에 드러나는 구성을 취한다.
프리미엄 다크 배경 위에서 상태 색상과 CTA가 명확한 위계를 형성한다.

### 1-2. 브랜드 키워드

| 키워드 | Flutter 구현 방향 |
|--------|------------------|
| **실시간** | 상태 배지 애니메이션, 풀-투-리프레시, 스켈레톤 UI |
| **안심** | 명확한 상태 표시, 고대비 텍스트, 접근성 준수 |
| **최적화** | 정보 밀도 균형, Glanceable 레이아웃, 빠른 CTA 접근 |

### 1-3. 톤 앤 매너

- 감성보다 **기능 중심**: 불필요한 장식 위젯 사용 금지
- 정보는 **계층 구조**로 표현: 상태 → 핵심 요약 → 액션 순서
- 색상은 **의미를 전달**하는 수단: 장식적 컬러 배리에이션 지양
- 애니메이션은 **피드백 목적**에 한정: 300ms 이내 완료 원칙

---

## 2. 디자인 원칙

### 원칙 1 — Glanceable (한눈에 파악)

여행 중 이동하면서 화면을 보는 사용자를 가정한다.
핵심 상태 정보(상태 배지, 현재 일정 장소명, 남은 시간)는 스크롤 없이 첫 화면에서 확인 가능해야 한다.

- Flutter 적용: 상단 고정 영역에 `StatusBadge` + 브리핑 요약 배치
- `AppBar` 또는 `SliverAppBar`의 핀 영역에 배지 노출
- 폰트 크기는 최소 14px(body2) 이상, 상태 정보는 16px(h3) 이상

### 원칙 2 — Actionable (즉각 행동 가능)

정보 확인 후 다음 행동까지 탭 수를 최소화한다.
브리핑 화면에서 대안 장소 확인 → 일정 교체까지 3탭 이내로 완결한다.

- Flutter 적용: CTA 버튼(`ElevatedButton`)은 항상 화면 하단 고정 영역 또는 카드 내부에 노출
- `BottomSheet`를 통한 컨텍스트 액션으로 화면 전환 최소화

### 원칙 3 — Context-First (맥락 우선)

사용자가 현재 어떤 상황인지(어디에 있는지, 무슨 문제가 생겼는지)를 먼저 표시하고, 그 맥락 안에서 정보를 제공한다.

- Flutter 적용: 브리핑 카드 최상단에 현재 장소 + 상태를 배치
- 대안 카드에는 현재 위치 기준 거리와 이동 시간을 병기

### 원칙 4 — Transparent Trust (투명한 신뢰)

AI 추천의 근거를 간략히 노출하여 사용자가 판단을 위임하지 않고 검증할 수 있게 한다.

- Flutter 적용: 대안 카드에 추천 이유 레이블(최대 2줄) 표시
- 데이터 출처(Google Maps, 날씨 API 등) 아이콘 표기

### 원칙 5 — Media-Immersive (미디어 몰입)

여행지 사진이 UI를 압도하는 구성을 기본으로 한다. Flutter 위젯 계층에서 이미지가 배경을 채우고, 텍스트/버튼은 그라디언트 오버레이 위에 얹힌다.

- Flutter 적용: `Stack` + `Image.network` + `Gradient` 오버레이 패턴
- `CachedNetworkImage`로 이미지 캐시 처리 (성능 요건)
- 이미지 로딩 전 `shimmer` 플레이스홀더 표시

---

## 3. 컬러 시스템

### 3-1. 배경 컬러 (Dark Theme)

| 역할 | 토큰명 | HEX | Flutter 활용 |
|------|--------|-----|-------------|
| 최상위 배경 | `bgPrimary` | `#0A0A0A` | `Scaffold.backgroundColor` |
| 카드/패널 배경 | `bgCard` | `#1A1A1A` | `Card.color`, `BottomSheet` 배경 |
| 입력/구분 배경 | `bgInput` | `#242424` | `TextField` fill, `SearchBar` 배경 |
| 호버/프레스 배경 | `bgHover` | `#2A2A2A` | `InkWell` splash/highlight color |
| 오버레이 딤 | `bgOverlay` | `rgba(0,0,0,0.6)` | `ModalBarrier`, `showBottomSheet` barrierColor |

> OLED 번인 방지: 순수 `#000000` 대신 `#0A0A0A`를 최하위 배경으로 사용한다.

### 3-2. 텍스트 컬러

| 역할 | 토큰명 | HEX | 대비비 (on #0A0A0A) | Flutter 활용 |
|------|--------|-----|:------------------:|-------------|
| 기본 텍스트 | `textPrimary` | `#F0F0F0` | 18.3:1 | `onBackground`, `onSurface` |
| 보조 텍스트 | `textSecondary` | `#8A8A8A` | 5.1:1 | `onSurfaceVariant` |
| 비활성 텍스트 | `textDisabled` | `#555555` | 2.8:1 (장식용) | `disabledColor` |
| 반전 텍스트 | `textInverse` | `#0A0A0A` | — | 밝은 배경 위 텍스트 |

> 눈부심 방지: 순수 `#FFFFFF` 대신 `#F0F0F0`을 기본 텍스트 색상으로 사용한다.

### 3-3. 상태 배지 컬러

| 상태 | 토큰명 | HEX | 아이콘 | 의미 | 대비비 (on #1A1A1A) |
|------|--------|-----|--------|------|:------------------:|
| 정상 | `statusGreen` | `#34C759` | check_circle | 모든 항목 정상 | 4.6:1 |
| 주의 | `statusYellow` | `#FFD60A` | warning_amber | 1개 이상 주의 | 9.5:1 |
| 위험 | `statusRed` | `#FF3B30` | cancel | 1개 이상 위험 | 4.5:1 |
| 미확인 | `statusGray` | `#8E8E93` | help | 데이터 수집 불가 | 4.1:1 |

> WCAG 2.1 AA 준수: 상태 배지는 색상 + 아이콘 조합을 필수로 적용한다 (색약 사용자 지원).
> Flutter 아이콘: `Icons.check_circle`, `Icons.warning_amber`, `Icons.cancel`, `Icons.help`

### 3-4. 기능 강조 컬러 (Accent)

| 역할 | 토큰명 | HEX | Flutter 활용 |
|------|--------|-----|-------------|
| CTA/긴급 | `accentRed` | `#FF2D2D` | `primary`, `ElevatedButton` 배경 |
| 추천/별점 | `accentAmber` | `#FFB830` | 평점 별, 추천 강조 |
| 프리미엄/구독 | `accentPurple` | `#7B3FE0` | Pro 배지, Paywall 배경 |
| 정보/링크 | `accentBlue` | `#0A84FF` | `TextButton`, 하이퍼링크, `focusColor` |

### 3-5. 그라디언트

| 용도 | 시작색 | 종료색 | 방향 | 적용 위젯 |
|------|--------|--------|------|----------|
| 카드 하단 오버레이 | `transparent` | `rgba(0,0,0,0.7)` | 상→하 | `Stack` 내 `Container` (그라디언트 장식) |
| 프리미엄 배경 | `#7B3FE0` | `#4A1FB0` | 135° | Paywall 위젯 배경 |
| 위험 강조 배경 | `#FF2D2D` | `#CC0000` | 135° | 긴급 알림 카드 배경 |

### 3-6. Flutter ColorScheme.dark() 매핑

Material Design 3의 `ColorScheme.dark()`에 아래 값을 오버라이드하여 적용한다.

| ColorScheme 필드 | 매핑 값 | 비고 |
|-----------------|---------|------|
| `brightness` | `Brightness.dark` | |
| `primary` | `#FF2D2D` (accentRed) | 주요 CTA 색상 |
| `onPrimary` | `#F0F0F0` | primary 위 텍스트 |
| `secondary` | `#7B3FE0` (accentPurple) | 프리미엄/구독 |
| `onSecondary` | `#F0F0F0` | |
| `tertiary` | `#0A84FF` (accentBlue) | 정보/링크 |
| `onTertiary` | `#F0F0F0` | |
| `surface` | `#1A1A1A` (bgCard) | 카드, 시트 배경 |
| `onSurface` | `#F0F0F0` (textPrimary) | |
| `surfaceVariant` | `#242424` (bgInput) | 입력 필드 배경 |
| `onSurfaceVariant` | `#8A8A8A` (textSecondary) | |
| `background` | `#0A0A0A` (bgPrimary) | Scaffold 배경 |
| `onBackground` | `#F0F0F0` | |
| `error` | `#FF3B30` (statusRed) | 에러 상태 |
| `onError` | `#F0F0F0` | |
| `outline` | `#2A2A2A` | 테두리, 구분선 |
| `shadow` | `#000000` | 그림자 (다크 테마에선 최소화) |
| `inverseSurface` | `#F0F0F0` | 토스트/스낵바 배경 |
| `onInverseSurface` | `#0A0A0A` | 토스트 텍스트 |

### 3-7. Elevation (레이어 기반 깊이)

다크 테마에서는 그림자 대신 배경색 밝기 차이로 깊이를 표현한다.

| 레벨 | 배경색 | Flutter 활용 |
|:----:|--------|-------------|
| 0 — Base | `#0A0A0A` | `Scaffold.backgroundColor` |
| 1 — Surface | `#1A1A1A` | `Card`, `ListTile` |
| 2 — Raised | `#242424` | `TextField`, `Chip`, 드롭다운 |
| 3 — Overlay | `#2A2A2A` | `BottomSheet`, `Dialog` |
| 4 — Top | `#333333` | `SnackBar`, `Toast` |

---

## 4. 타이포그래피

### 4-1. 폰트 패밀리

| 플랫폼 | 기본 폰트 | 폴백 체인 | 비고 |
|--------|----------|----------|------|
| Android | Pretendard | sans-serif | `pubspec.yaml`에 asset 등록 |
| iOS | Pretendard | SF Pro Text → .SF UI Text | iOS 시스템 폰트 자동 폴백 |
| 숫자/단위 | Pretendard | — | 가변 폰트, 일관된 숫자 폭 |
| 모노스페이스 | SF Mono | Fira Code → monospace | 코드/시간값 표시 (최소 사용) |

> Pretendard 선정 이유: 한/영 혼용 지원, 가변 폰트(100~900), iOS SF Pro 톤과 유사한 산세리프.

### 4-2. Flutter TextTheme 매핑

Flutter의 `TextTheme` 필드를 아래 기준으로 정의한다.
`ThemeData.textTheme`에 전체 등록하여 앱 전반에 일관되게 적용한다.

| TextTheme 필드 | 크기 | 굵기 | 줄높이 | 자간 | 용도 |
|---------------|:----:|:----:|:------:|:----:|------|
| `displayLarge` | 28px | 700 | 36px (1.29) | -0.5px | 온보딩 메인 타이틀, 히어로 텍스트 |
| `displayMedium` | 22px | 700 | 28px (1.27) | -0.3px | 화면 타이틀 (AppBar title) |
| `displaySmall` | 18px | 600 | 24px (1.33) | -0.2px | 섹션 타이틀, 카드 타이틀 |
| `headlineMedium` | 16px | 600 | 22px (1.38) | 0px | 서브 타이틀, 리스트 타이틀 |
| `headlineSmall` | 15px | 400 | 22px (1.47) | 0px | 본문 텍스트 (bodyLarge 대응) |
| `titleLarge` | 14px | 400 | 20px (1.43) | 0.1px | 보조 본문, 카드 설명 |
| `titleMedium` | 12px | 400 | 16px (1.33) | 0.2px | 날짜, 메타 정보, 서브 레이블 |
| `titleSmall` | 11px | 600 | 14px (1.27) | 0.5px | 배지 레이블, 카테고리 태그 (Overline) |
| `bodyLarge` | 15px | 400 | 22px (1.47) | 0px | 본문 기본 |
| `bodyMedium` | 14px | 400 | 20px (1.43) | 0.1px | 보조 본문 |
| `bodySmall` | 12px | 400 | 16px (1.33) | 0.2px | 캡션, 날짜, 메타 |
| `labelLarge` | 16px | 600 | 22px | 0px | ElevatedButton 텍스트 |
| `labelMedium` | 14px | 600 | 20px | 0.1px | OutlinedButton, TextButton 텍스트 |
| `labelSmall` | 11px | 600 | 14px | 0.5px | 칩, 배지 레이블 |

### 4-3. 굵기 기준

| 굵기 | 값 | Flutter FontWeight | 용도 |
|------|:--:|-------------------|------|
| Bold | 700 | `FontWeight.w700` | 타이틀, CTA 버튼 텍스트 |
| SemiBold | 600 | `FontWeight.w600` | 카드 타이틀, 탭 레이블, 강조 |
| Regular | 400 | `FontWeight.w400` | 본문, 설명 텍스트 |
| Light | 300 | `FontWeight.w300` | 날짜, 보조 정보 (제한적 사용) |

---

## 5. 간격 시스템

### 5-1. 기본 단위

**4px Grid** — 모든 간격과 크기는 4의 배수를 기준으로 한다.

Flutter `SizedBox`, `Padding`, `EdgeInsets` 사용 시 아래 토큰 값을 준수한다.

### 5-2. 간격 토큰

| 토큰명 | 값 | 용도 | Flutter 활용 |
|--------|:--:|------|-------------|
| `space2xs` | 2px | 미세 간격, 아이콘 내부 정렬 | `SizedBox(width: 2)` |
| `spaceXs` | 4px | 최소 간격, 인라인 요소 사이 | `SizedBox(width: 4)` |
| `spaceSm` | 8px | 컴포넌트 내부 패딩(소), 아이콘-텍스트 간격 | `EdgeInsets.all(8)` |
| `spaceMd` | 12px | 컴포넌트 내부 패딩(중), 카드 간 간격 | `EdgeInsets.all(12)` |
| `spaceBase` | 16px | 화면 좌우 여백, 표준 패딩 | `EdgeInsets.symmetric(horizontal: 16)` |
| `spaceLg` | 20px | 그룹 간 간격 | `SizedBox(height: 20)` |
| `spaceXl` | 24px | 섹션 간 간격(소) | `SizedBox(height: 24)` |
| `space2xl` | 32px | 섹션 간 간격(대) | `SizedBox(height: 32)` |
| `space3xl` | 40px | 히어로 영역 상하 여백 | `SizedBox(height: 40)` |
| `space4xl` | 48px | 화면 상단/하단 여유 공간 | `SizedBox(height: 48)` |

### 5-3. 레이아웃 고정값

| 요소 | 값 | Flutter 적용 방식 |
|------|:--:|-----------------|
| 화면 좌우 패딩 | 16px | `Scaffold` > `Padding(horizontal: 16)` |
| AppBar 높이 | 56px | `PreferredSize.fromHeight(56)` |
| BottomNavigationBar 높이 | 56px | `BottomNavigationBar.selectedFontSize` 조정 |
| iOS 홈 인디케이터 세이프 에어리어 | 34px | `MediaQuery.of(context).padding.bottom` |
| BottomSheet 핸들 너비 | 36px | 커스텀 핸들 `Container` |
| BottomSheet 핸들 높이 | 4px | 커스텀 핸들 `Container` |
| 터치 타겟 최소 크기 | 44×44px | `InkWell` 내 `ConstrainedBox(minWidth: 44, minHeight: 44)` |
| 리스트 아이템 높이 | 56~72px | `ListTile.minVerticalPadding` 조정 |

### 5-4. 카드 내부 패딩 기준

| 카드 유형 | 내부 패딩 | 비고 |
|----------|---------|------|
| 브리핑 카드 | 16px 전체 | 표준 카드 |
| 대안 카드 | 상 0 / 좌우 16 / 하 16 | 상단 이미지 영역 패딩 없음 |
| 일정 타임라인 아이템 | 좌 48 / 우 16 / 상하 12 | 좌측 시간축 공간 확보 |
| 섹션 헤더 | 상 24 / 하 8 / 좌우 16 | 섹션 구분 |

---

## 6. 컴포넌트 스타일

### 6-1. 버튼 (Button)

#### ElevatedButton — 주요 CTA

| 속성 | 값 |
|------|---|
| 배경색 | `accentRed` (#FF2D2D) |
| 텍스트 색 | `textPrimary` (#F0F0F0) |
| 텍스트 스타일 | 16px / FontWeight.w600 / letterSpacing 0 |
| 높이 | 52px |
| 모서리 반경 | 8px (BorderRadius.circular(8)) |
| 좌우 패딩 | 24px |
| 프레스 효과 | splashColor `#FF2D2D` 30% 불투명도, scale 0.97 |
| 비활성 배경 | `#555555` |
| 비활성 텍스트 | `#8A8A8A` |
| 최소 너비 | 전체 너비 (주요 CTA) 또는 120px (인라인 CTA) |

#### OutlinedButton — 보조 액션

| 속성 | 값 |
|------|---|
| 테두리 색 | `accentRed` (#FF2D2D) |
| 테두리 두께 | 1.5px |
| 텍스트 색 | `accentRed` (#FF2D2D) |
| 배경색 | 투명 |
| 높이 | 52px |
| 모서리 반경 | 8px |
| 프레스 효과 | `accentRed` 10% 불투명도 배경 |

#### TextButton — 부가 액션/링크

| 속성 | 값 |
|------|---|
| 텍스트 색 | `accentBlue` (#0A84FF) |
| 텍스트 스타일 | 14px / FontWeight.w600 |
| 언더라인 | 없음 (기본), 필요 시 `TextDecoration.underline` |
| 프레스 효과 | `accentBlue` 10% 불투명도 배경 |
| 패딩 | 상하 8px / 좌우 12px |

### 6-2. 카드 (Card)

| 속성 | 값 |
|------|---|
| 배경색 | `bgCard` (#1A1A1A) |
| 모서리 반경 | 12px |
| 테두리 | 없음 (기본) / 선택 시 1.5px `accentRed` |
| 그림자 | 없음 (다크 테마 레이어 기반 깊이 표현) |
| 내부 패딩 | 16px 전체 |
| 클리핑 | `ClipRRect(borderRadius: 12)` — 이미지 오버플로 방지 |

### 6-3. BottomSheet

| 속성 | 값 |
|------|---|
| 배경색 | `#2A2A2A` (Overlay 레벨) |
| 상단 모서리 반경 | 20px |
| 핸들 색상 | `#555555` |
| 핸들 크기 | 36×4px |
| 핸들 상단 여백 | 12px |
| 드래그 닫기 | 활성화 (`enableDrag: true`) |
| 최소 높이 | 화면 높이의 40% |
| 최대 높이 | 화면 높이의 90% |
| barrierColor | `rgba(0,0,0,0.6)` |

### 6-4. AppBar

| 속성 | 값 |
|------|---|
| 배경색 | `bgPrimary` (#0A0A0A) |
| 타이틀 색 | `textPrimary` (#F0F0F0) |
| 타이틀 스타일 | 18px / FontWeight.w600 (displaySmall) |
| 타이틀 정렬 | 중앙 (`centerTitle: true`) |
| 높이 | 56px (`toolbarHeight: 56`) |
| 구분선 | 없음 (`elevation: 0`) |
| 아이콘 색 | `textPrimary` (#F0F0F0) |
| 스크롤 시 | `SliverAppBar` 핀 고정 또는 그대로 유지 |

### 6-5. BottomNavigationBar

| 속성 | 값 |
|------|---|
| 배경색 | `bgCard` (#1A1A1A) |
| 활성 아이콘 색 | `textPrimary` (#F0F0F0) |
| 비활성 아이콘 색 | `textSecondary` (#8A8A8A) |
| 활성 레이블 색 | `textPrimary` (#F0F0F0) |
| 비활성 레이블 색 | `textSecondary` (#8A8A8A) |
| 레이블 스타일 | 11px / FontWeight.w600 (labelSmall) |
| 높이 | 56px + 세이프 에어리어 |
| 상단 테두리 | 1px `#2A2A2A` |
| 탭 수 | 3 (일정 / 브리핑 / 마이페이지) |
| 타입 | `BottomNavigationBarType.fixed` |

#### 탭 아이콘 매핑

| 탭 | 비활성 아이콘 | 활성 아이콘 | Material Icons |
|----|-------------|-----------|---------------|
| 일정 | `calendar_today_outlined` | `calendar_today` | 일정 관리 |
| 브리핑 | `notifications_outlined` | `notifications` | 알림/브리핑 |
| 마이페이지 | `person_outline` | `person` | 프로필 |

### 6-6. TabBar

| 속성 | 값 |
|------|---|
| 배경색 | `bgPrimary` (#0A0A0A) |
| 활성 탭 색 | `textPrimary` (#F0F0F0) |
| 비활성 탭 색 | `textSecondary` (#8A8A8A) |
| 인디케이터 색 | `accentRed` (#FF2D2D) |
| 인디케이터 높이 | 2px |
| 탭 레이블 스타일 | 14px / FontWeight.w600 |
| 탭 높이 | 44px |
| 탭 패딩 | 좌우 16px |

### 6-7. TextField / SearchBar

| 속성 | 값 |
|------|---|
| 배경색 | `bgInput` (#242424) |
| 테두리 (기본) | 없음 (`InputBorder.none`) |
| 테두리 (포커스) | 1.5px `accentBlue` (#0A84FF) 하단 라인 |
| 테두리 (에러) | 1.5px `statusRed` (#FF3B30) |
| 모서리 반경 | 8px |
| 텍스트 색 | `textPrimary` (#F0F0F0) |
| 힌트 텍스트 색 | `textSecondary` (#8A8A8A) |
| 레이블 색 | `textSecondary` (#8A8A8A) |
| 커서 색 | `accentBlue` (#0A84FF) |
| 내부 패딩 | 상하 14px / 좌우 16px |
| 아이콘 색 | `textSecondary` (#8A8A8A) |

### 6-8. ListTile

| 속성 | 값 |
|------|---|
| 배경색 | `bgCard` (#1A1A1A) |
| 제목 스타일 | 15px / FontWeight.w400 (bodyLarge) |
| 서브타이틀 스타일 | 13px / FontWeight.w400 / `textSecondary` |
| 최소 높이 | 56px (단일 행) / 72px (2행) |
| 좌우 패딩 | 16px |
| 구분선 | 1px `#2A2A2A` (Divider) |
| 탭 리플 색 | `bgHover` (#2A2A2A) |

### 6-9. Chip

| 속성 | 값 |
|------|---|
| 배경색 | `bgInput` (#242424) |
| 텍스트 색 | `textPrimary` (#F0F0F0) |
| 텍스트 스타일 | 12px / FontWeight.w600 (labelSmall) |
| 모서리 반경 | 24px (Pill 형태) |
| 패딩 | 상하 4px / 좌우 10px |
| 테두리 | 없음 (기본) / 선택 시 1px `accentRed` |
| 선택 배경 | `accentRed` 20% 불투명도 |
| 높이 | 28px |

### 6-10. Badge (배지)

| 속성 | 값 |
|------|---|
| 크기 | 아이콘 16px, 컨테이너 높이 20px 최소 |
| 모서리 반경 | 10px (Pill) |
| 텍스트 스타일 | 11px / FontWeight.w600 |
| 텍스트 색 | `textPrimary` (#F0F0F0) |
| 패딩 | 좌우 6px |
| 위치 | 우상단 오버레이 (`Stack` + `Positioned`) |

---

## 7. 반응형 브레이크포인트

### 7-1. 브레이크포인트 정의

Flutter는 CSS 미디어 쿼리 대신 `MediaQuery.of(context).size.width`로 분기한다.

| 단계 | 너비 범위 | 대상 디바이스 |
|------|----------|-------------|
| Mobile S | 320px ~ 374px | iPhone SE, Galaxy A 소형 |
| **Mobile M (기준)** | **375px ~ 428px** | **iPhone 13/14/15, Galaxy S 시리즈** |
| Mobile L | 429px ~ 599px | iPhone Plus/Max, 대형 Android |
| Tablet | 600px ~ 839px | iPad Mini, 소형 태블릿 |
| Desktop | 840px 이상 | iPad Air/Pro, 데스크톱 브라우저 |

> Mobile First: 375px을 기준 해상도로 설계하고 상위로 확장한다.
> 최소 지원: 320px (iPhone SE). 320px 미만은 미지원.

### 7-2. 레이아웃 변화 표

| 속성 | Mobile (~599px) | Tablet (600~839px) | Desktop (840px+) |
|------|:--------------:|:-----------------:|:---------------:|
| 컬럼 수 | 1 | 2 | 3 |
| 좌우 패딩 | 16px | 24px | 32px |
| 네비게이션 위치 | 하단 탭 바 | 하단 탭 바 | 사이드 네비게이션 |
| 카드 레이아웃 | 풀 와이드, 세로 스택 | 2열 그리드 | 3열 그리드 |
| 최대 컨텐츠 너비 | 100% | 100% | 1200px (중앙 정렬) |
| AppBar 타이틀 정렬 | 중앙 | 중앙 | 좌측 |
| BottomSheet 너비 | 100% | 540px (중앙) | 480px (중앙) |

### 7-3. Flutter 분기 패턴

`LayoutBuilder` 또는 `MediaQuery`를 통해 분기한다.

| 분기 조건 | 적용 레이아웃 |
|----------|------------|
| `width < 600` | 모바일 단일 컬럼 레이아웃 |
| `600 <= width < 840` | 태블릿 2열 `GridView` |
| `width >= 840` | 데스크톱 사이드 네비 + 3열 그리드 |

---

## 8. 서비스 특화 컴포넌트

### 8-1. StatusBadge (상태 배지 위젯)

여행 일정의 현재 상태를 한눈에 전달하는 핵심 위젯.

**스펙**

| 항목 | 명세 |
|------|------|
| 역할 | 일정 항목의 실시간 상태(정상/주의/위험/미확인) 표시 |
| 크기 | 높이 28px, 최소 너비 64px |
| 모서리 반경 | 14px (Pill) |
| 내부 패딩 | 좌우 10px / 상하 4px |
| 아이콘 크기 | 16px |
| 아이콘-텍스트 간격 | 4px |
| 텍스트 스타일 | 12px / FontWeight.w600 |
| 배경색 | 상태 컬러 20% 불투명도 |
| 텍스트/아이콘 색 | 상태 컬러 (full opacity) |

**4가지 상태 명세**

| 상태 | 배경 (20% opacity) | 전경 | 아이콘 | 레이블 |
|------|:-----------------:|------|--------|--------|
| 정상 | `#34C759` 20% | `#34C759` | `Icons.check_circle` | "정상" |
| 주의 | `#FFD60A` 20% | `#FFD60A` | `Icons.warning_amber` | "주의" |
| 위험 | `#FF3B30` 20% | `#FF3B30` | `Icons.cancel` | "위험" |
| 미확인 | `#8E8E93` 20% | `#8E8E93` | `Icons.help` | "미확인" |

**인터랙션**

- 상태 변경 시: 0.8 → 1.2 → 1.0 스케일 바운스 + 색상 크로스페이드 (400ms, spring curve)
- 탭 가능 여부: 기본 비활성 (정보 표시 전용), 탭 시 상태 상세 BottomSheet 열기 옵션

---

### 8-2. BriefingCard (브리핑 카드 위젯)

출발 전 종합 상황을 요약하여 표시하는 카드.

**스펙**

| 항목 | 명세 |
|------|------|
| 역할 | 전체 일정 종합 상태 + AI 총평 + CTA 제공 |
| 배경색 | `bgCard` (#1A1A1A) |
| 모서리 반경 | 12px |
| 내부 패딩 | 16px 전체 |
| 최소 높이 | 160px |

**레이아웃 구조 (상→하)**

| 영역 | 내용 | 스타일 |
|------|------|--------|
| 헤더 | 브리핑 일시 + `StatusBadge` | Row, 양쪽 정렬 |
| 총평 텍스트 | AI 상황 요약 (최대 3줄) | 15px / Regular / `textPrimary` |
| 세부 상태 목록 | 날씨·혼잡도·영업시간·교통 아이콘 + 상태 | Row, 아이콘 16px + 텍스트 12px |
| CTA 버튼 | "대안 보기" 또는 "일정 보기" | `ElevatedButton`, 풀 너비, 상단 여백 16px |

**상태별 카드 변형**

| 상황 | 변형 |
|------|------|
| 모든 항목 정상 | CTA 숨김 또는 "일정 확인" |
| 주의/위험 항목 존재 | CTA "대안 보기" 강조 노출 |
| 데이터 로딩 중 | 총평 영역 Shimmer 플레이스홀더 |

---

### 8-3. AlternativeCard (대안 카드 위젯)

현재 장소의 대안으로 추천되는 장소를 표시하는 카드.

**스펙**

| 항목 | 명세 |
|------|------|
| 역할 | 대안 장소 사진·정보·선택 제공 |
| 배경색 | `bgCard` (#1A1A1A) |
| 모서리 반경 | 12px |
| 이미지 높이 | 160px (상단 풀 너비, 카드 모서리 적용) |
| 이미지 하단 그라디언트 | `transparent → rgba(0,0,0,0.7)` 오버레이 |
| 텍스트 영역 패딩 | 좌우 16px / 상 12px / 하 16px |

**이미지 영역 오버레이 (Stack)**

| 요소 | 위치 | 스타일 |
|------|------|--------|
| 카테고리 칩 | 좌상단 8px | Chip — `bgCard` 60% 불투명도 배경 |
| 거리 + 이동 시간 | 우상단 8px | 12px / `textPrimary` |

**텍스트 영역 (상→하)**

| 요소 | 스타일 |
|------|--------|
| 장소명 | 16px / FontWeight.w600 / 최대 1줄 |
| 평점 (별 아이콘 + 숫자 + 리뷰 수) | 13px / `accentAmber` 별, `textSecondary` 숫자 |
| 추천 이유 | 13px / Regular / `textSecondary` / 최대 2줄 |
| 선택 버튼 | `ElevatedButton` "이 장소로 변경" — 상단 12px 여백 |

**선택 상태**

- 선택 전: 기본 카드 스타일
- 선택 후: 1.5px `accentRed` 테두리, 우상단 체크 아이콘 오버레이

---

### 8-4. ScheduleTimeline (일정 타임라인 위젯)

하루 일정을 시간 순서대로 시각화하는 위젯.

**스펙**

| 항목 | 명세 |
|------|------|
| 역할 | 당일 일정 전체 표시, 장소 순서 변경/삭제 지원 |
| 레이아웃 | 좌측 시간축(48px 고정) + 우측 장소 카드 |
| 시간축 스타일 | 12px / Regular / `textSecondary`, 수직 점선 연결 |
| 장소 카드 | `bgCard` (#1A1A1A), 모서리 반경 8px |
| 카드 간 간격 | 12px |
| 활성 장소 강조 | 좌측 시간축에 2px `accentRed` 세로 라인 |

**장소 카드 내부 (Row)**

| 요소 | 스타일 |
|------|--------|
| 썸네일 이미지 | 56×56px, 모서리 반경 8px |
| 장소명 | 15px / FontWeight.w600 |
| 카테고리 + 예상 체류 시간 | 12px / `textSecondary` |
| `StatusBadge` | 우측 정렬 |
| 드래그 핸들 | 우측 `Icons.drag_handle` 24px / `textSecondary` |

**제스처**

| 제스처 | 동작 |
|--------|------|
| 좌 스와이프 | 빨간 배경 + 삭제 아이콘 노출, 끝까지 스와이프 시 삭제 |
| 길게 누르기 (500ms) | 드래그 앤 드롭 모드 진입 |
| 드래그 앤 드롭 | 순서 변경, 드롭 위치 프리뷰 표시 |

---

### 8-5. PaywallWidget (구독 전환 위젯)

프리미엄 기능 접근 차단 및 구독 유도 위젯.

**스펙**

| 항목 | 명세 |
|------|------|
| 역할 | 무료 사용자에게 Pro 구독 유도 |
| 배경 | `linear-gradient(135deg, #7B3FE0 → #4A1FB0)` |
| 모서리 반경 | 16px |
| 내부 패딩 | 24px 전체 |

**레이아웃 (상→하, 중앙 정렬)**

| 요소 | 스타일 |
|------|--------|
| Pro 아이콘 | 48px `Icons.workspace_premium` / `#FFB830` |
| 타이틀 | 22px / FontWeight.w700 / `textPrimary` |
| 혜택 목록 | 체크 아이콘 + 혜택 텍스트, 14px / Regular, 상하 4px 간격 |
| 구독 CTA 버튼 | `ElevatedButton` — 배경 `#FFB830`, 텍스트 `#0A0A0A`, 너비 풀 |
| 하단 부가 텍스트 | 12px / `textSecondary` — "언제든 취소 가능" |

---

## 9. 인터랙션 패턴

### 9-1. 애니메이션 토큰

| 토큰명 | Duration | Easing (Flutter Curve) | 용도 |
|--------|:--------:|------------------------|------|
| `animFast` | 150ms | `Curves.easeOut` | 버튼 프레스, 토글, 리플 |
| `animNormal` | 250ms | `Curves.easeInOut` | 화면 전환, 카드 확장 |
| `animSlow` | 350ms | `Curves.fastOutSlowIn` (M3 standard) | BottomSheet 열림/닫힘, Dialog |
| `animSpring` | 400ms | `Curves.elasticOut` (바운스 근사) | 배지 상태 변경, 강조 효과 |

> `Curves.fastOutSlowIn`은 Material Design 3의 표준 전환 커브와 동일하다.

### 9-2. 탭/롱프레스 피드백 (InkWell)

| 항목 | 명세 |
|------|------|
| 위젯 | `InkWell` 또는 `GestureDetector` + `AnimatedScale` |
| splashColor | 탭 대상 배경의 밝기 10% 증가 버전 |
| highlightColor | 투명 (splash만 표시) |
| borderRadius | 컨테이너와 동일 |
| 탭 스케일 피드백 | 프레스 시 0.97, 릴리스 시 1.0 (150ms `easeOut`) |
| 롱프레스 임계값 | 500ms |
| 롱프레스 동작 | 컨텍스트 메뉴 `BottomSheet` 또는 `showMenu` |

### 9-3. 스와이프 제스처 (장소 삭제)

| 항목 | 명세 |
|------|------|
| 위젯 | `Dismissible` 또는 커스텀 `GestureDetector` |
| 방향 | `DismissDirection.endToStart` (좌 스와이프) |
| 배경 노출 | 빨간 배경 (#FF3B30) + `Icons.delete_outline` 24px 우측 정렬 |
| 임계값 | 40% 이상 스와이프 시 삭제 확정 |
| 삭제 전 확인 | `SnackBar` "취소" 옵션 제공 (3초) |
| 애니메이션 | 250ms `easeIn` 슬라이드 아웃 |

### 9-4. 드래그 앤 드롭 (장소 순서 변경)

| 항목 | 명세 |
|------|------|
| 위젯 | `ReorderableListView` 또는 `Draggable` + `DragTarget` |
| 진입 방식 | 롱프레스(500ms) 후 드래그 모드 활성화 |
| 드래그 중 피드백 | 원본 위치 반투명(opacity 0.5) + 드래그 아이템 elevation 상승(그림자 4px) |
| 드롭 프리뷰 | 드롭 위치에 2px 높이 `accentRed` 수평 구분선 |
| 완료 애니메이션 | 250ms `easeInOut` 위치 전환 |
| 핸들 아이콘 | `Icons.drag_handle` / `textSecondary` |

### 9-5. BottomSheet 전환 애니메이션

| 상태 | 방향 | Duration | Curve |
|------|------|:--------:|-------|
| 열림 | 하단 → 상단 슬라이드 + 배경 딤 페이드 인 | 350ms | `Curves.fastOutSlowIn` |
| 닫힘 | 상단 → 하단 슬라이드 + 배경 딤 페이드 아웃 | 300ms | `Curves.easeIn` |
| 드래그 닫기 | 드래그 진행률에 비례하여 딤 해제 | 실시간 | — |

### 9-6. 화면 전환 (Route Transition)

| 전환 유형 | 방향 | Duration | Curve |
|----------|------|:--------:|-------|
| Push (상세 진입) | 우 → 좌 슬라이드 | 250ms | `Curves.easeInOut` |
| Pop (뒤로가기) | 좌 → 우 슬라이드 | 250ms | `Curves.easeInOut` |
| 탭 전환 | 즉각 (애니메이션 없음) | 0ms | — |
| Dialog 열림 | 스케일 0.9 → 1.0 + 페이드 인 | 350ms | `Curves.fastOutSlowIn` |

### 9-7. 상태 배지 색상 전환 애니메이션

배지 상태 변경(정상→주의, 주의→위험 등) 시 사용자 주의를 확실히 끈다.

| 단계 | 동작 | Duration |
|------|------|:--------:|
| 1. 바운스 아웃 | 스케일 1.0 → 0.8 | 100ms |
| 2. 색상 크로스페이드 | 이전 색 → 새 색 | 200ms |
| 3. 바운스 오버슈트 | 스케일 0.8 → 1.2 | 150ms |
| 4. 안정화 | 스케일 1.2 → 1.0 | 150ms |
| 전체 | — | **400ms** (spring curve) |

### 9-8. 로딩 상태 (Skeleton UI)

API 호출 대기 중 스켈레톤 플레이스홀더를 표시한다.

| 항목 | 명세 |
|------|------|
| 색상 패턴 | `#1A1A1A` → `#2A2A2A` → `#1A1A1A` 반복 |
| 애니메이션 | Shimmer 효과 (좌→우 빛 이동) |
| 주기 | 1.5초 반복 |
| 적용 범위 | BriefingCard 총평 텍스트, AlternativeCard 이미지/텍스트, StatusBadge |
| 위젯 | `shimmer` 패키지 또는 `AnimatedContainer` + `LinearGradient` 커스텀 구현 |

### 9-9. 토스트 / SnackBar

| 항목 | 명세 |
|------|------|
| 등장 | 화면 하단에서 위로 슬라이드 (250ms `easeOut`) |
| 유지 | 3초 |
| 퇴장 | 페이드 아웃 (200ms) |
| 배경색 | `#333333` (Top 레벨) |
| 텍스트 색 | `textPrimary` (#F0F0F0) |
| 액션 텍스트 색 | `accentBlue` (#0A84FF) |
| 위치 | `SnackBarBehavior.floating`, 하단 탭 바 위 8px |
| 모서리 반경 | 8px |

---

## 부록: 아이콘 라이브러리

### 선택: Material Icons (Flutter 기본)

Flutter의 `Icons` 클래스를 기본으로 사용한다. 추가 아이콘이 필요한 경우 `flutter_lucide` 패키지를 병행한다.

> Lucide Icons는 웹 스타일 가이드에서 사용된 라이브러리로, Flutter 환경에서는 `Material Icons`가 기본이며 시각적 톤이 유사하다.

### 아이콘 크기 기준

| 크기 | 값 | 용도 |
|------|:--:|------|
| Small | 16px | 인라인 아이콘, 배지 내 아이콘 |
| Medium | 20px | 리스트 아이콘, 버튼 내 아이콘 |
| Default | 24px | 네비게이션 탭, 표준 아이콘 |
| Large | 32px | 상태 아이콘, 빈 상태 일러스트 |
| XLarge | 48px | 온보딩 일러스트, 히어로 아이콘 |

### 접근성 지침

| 항목 | 기준 |
|------|------|
| 색상 대비 (일반 텍스트) | 4.5:1 이상 (WCAG 2.1 AA) |
| 색상 대비 (큰 텍스트 H1/H2) | 3:1 이상 |
| 터치 타겟 최소 크기 | 44×44px |
| 색상만으로 정보 전달 금지 | 상태 배지: 색상 + 아이콘 조합 필수 |
| 포커스 인디케이터 | 2px `accentBlue` (#0A84FF) outline, offset 2px |
| `Semantics` 위젯 | 모든 아이콘, 이미지, 상태 정보에 `label` 제공 |
| 동적 콘텐츠 변경 | `Semantics(liveRegion: true)` — 배지 상태 변경 알림 |

---

*이 문서는 Flutter 3.x 기준으로 작성되었으며, Material Design 3 사양을 기반으로 한다.*
*구현 코드는 포함하지 않으며, 설계 명세로 사용한다.*
*참조 문서: `docs/plan/design/uiux/style-guide.md`*
