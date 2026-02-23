/// style-guide.md 5절 기반 간격 시스템 (4px Grid)
abstract final class AppSpacing {
  AppSpacing._();

  // ---------------------------------------------------------------------------
  // 5-2. 간격 토큰
  // ---------------------------------------------------------------------------

  /// 2px — 미세 간격, 아이콘 내부 정렬
  static const double space2xs = 2.0;

  /// 4px — 최소 간격, 인라인 요소 사이
  static const double spaceXs = 4.0;

  /// 8px — 컴포넌트 내부 패딩(소), 아이콘-텍스트 간격
  static const double spaceSm = 8.0;

  /// 12px — 컴포넌트 내부 패딩(중), 카드 간 간격
  static const double spaceMd = 12.0;

  /// 16px — 화면 좌우 여백, 표준 패딩
  static const double spaceBase = 16.0;

  /// 20px — 그룹 간 간격
  static const double spaceLg = 20.0;

  /// 24px — 섹션 간 간격(소)
  static const double spaceXl = 24.0;

  /// 32px — 섹션 간 간격(대)
  static const double space2xl = 32.0;

  /// 40px — 히어로 영역 상하 여백
  static const double space3xl = 40.0;

  /// 48px — 화면 상단/하단 여유 공간
  static const double space4xl = 48.0;

  // ---------------------------------------------------------------------------
  // 5-3. 레이아웃 고정값
  // ---------------------------------------------------------------------------

  /// 화면 좌우 패딩
  static const double screenHorizontalPadding = 16.0;

  /// AppBar 높이
  static const double appBarHeight = 56.0;

  /// BottomNavigationBar 높이
  static const double bottomNavBarHeight = 56.0;

  /// iOS 홈 인디케이터 세이프 에어리어 (SafeArea로 자동 처리)
  static const double iosHomeIndicator = 34.0;

  /// BottomSheet 핸들 너비
  static const double bottomSheetHandleWidth = 36.0;

  /// BottomSheet 핸들 높이
  static const double bottomSheetHandleHeight = 4.0;

  /// 터치 타겟 최소 크기
  static const double touchTargetMin = 44.0;

  /// 리스트 아이템 최소 높이 (단일 행)
  static const double listItemMinHeight = 56.0;

  /// 리스트 아이템 최소 높이 (2행)
  static const double listItemTwoLineHeight = 72.0;

  // ---------------------------------------------------------------------------
  // 모서리 반경
  // ---------------------------------------------------------------------------

  /// 버튼 모서리 반경
  static const double radiusButton = 8.0;

  /// 카드 모서리 반경
  static const double radiusCard = 12.0;

  /// BottomSheet 상단 모서리 반경
  static const double radiusBottomSheet = 20.0;

  /// 배지(Pill) 모서리 반경
  static const double radiusBadge = 14.0;

  /// Chip(Pill) 모서리 반경
  static const double radiusChip = 24.0;

  /// Paywall 위젯 모서리 반경
  static const double radiusPaywall = 16.0;

  /// SnackBar 모서리 반경
  static const double radiusSnackBar = 8.0;

  // ---------------------------------------------------------------------------
  // 서비스 특화 고정값
  // ---------------------------------------------------------------------------

  /// 일정 타임라인 좌측 시간축 너비
  static const double timelineAxisWidth = 48.0;

  /// 일정 타임라인 썸네일 크기
  static const double thumbnailSize = 56.0;

  /// 대안 카드 이미지 높이
  static const double alternativeCardImageHeight = 160.0;

  /// 상태 배지 높이
  static const double statusBadgeHeight = 28.0;

  /// ElevatedButton 높이
  static const double buttonHeight = 52.0;
}
