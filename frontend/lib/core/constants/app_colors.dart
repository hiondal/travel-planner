import 'package:flutter/material.dart';

/// style-guide.md 기반 디자인 토큰 색상 상수
/// 모든 색상 값은 style-guide.md 3절 컬러 시스템을 따른다.
abstract final class AppColors {
  AppColors._();

  // ---------------------------------------------------------------------------
  // 3-1. 배경 컬러 (Dark Theme)
  // ---------------------------------------------------------------------------

  /// 최상위 배경 — Scaffold.backgroundColor
  static const Color bgPrimary = Color(0xFF0A0A0A);

  /// 카드/패널 배경 — Card.color, BottomSheet 배경
  static const Color bgCard = Color(0xFF1A1A1A);

  /// 입력/구분 배경 — TextField fill, SearchBar 배경
  static const Color bgInput = Color(0xFF242424);

  /// 호버/프레스 배경 — InkWell splash/highlight color
  static const Color bgHover = Color(0xFF2A2A2A);

  /// 오버레이 딤 — ModalBarrier, showBottomSheet barrierColor
  static const Color bgOverlay = Color(0x99000000); // rgba(0,0,0,0.6)

  // ---------------------------------------------------------------------------
  // 3-2. 텍스트 컬러
  // ---------------------------------------------------------------------------

  /// 기본 텍스트 — onBackground, onSurface
  static const Color textPrimary = Color(0xFFF0F0F0);

  /// 보조 텍스트 — onSurfaceVariant
  static const Color textSecondary = Color(0xFF8A8A8A);

  /// 비활성 텍스트 (장식용) — disabledColor
  static const Color textDisabled = Color(0xFF555555);

  /// 반전 텍스트 — 밝은 배경 위 텍스트
  static const Color textInverse = Color(0xFF0A0A0A);

  // ---------------------------------------------------------------------------
  // 3-3. 상태 배지 컬러
  // ---------------------------------------------------------------------------

  /// 정상 상태 — 모든 항목 정상
  static const Color statusGreen = Color(0xFF34C759);

  /// 주의 상태 — 1개 이상 주의
  static const Color statusYellow = Color(0xFFFFD60A);

  /// 위험 상태 — 1개 이상 위험
  static const Color statusRed = Color(0xFFFF3B30);

  /// 미확인 상태 — 데이터 수집 불가
  static const Color statusGray = Color(0xFF8E8E93);

  // ---------------------------------------------------------------------------
  // 3-4. 기능 강조 컬러 (Accent)
  // ---------------------------------------------------------------------------

  /// CTA/긴급 — primary, ElevatedButton 배경
  static const Color accentRed = Color(0xFFFF2D2D);

  /// 추천/별점 — 평점 별, 추천 강조
  static const Color accentAmber = Color(0xFFFFB830);

  /// 프리미엄/구독 — Pro 배지, Paywall 배경
  static const Color accentPurple = Color(0xFF7B3FE0);

  /// 정보/링크 — TextButton, 하이퍼링크, focusColor
  static const Color accentBlue = Color(0xFF0A84FF);

  // ---------------------------------------------------------------------------
  // 3-7. Elevation (레이어 기반 깊이)
  // ---------------------------------------------------------------------------

  /// Level 0 — Base (Scaffold)
  static const Color elevationBase = Color(0xFF0A0A0A);

  /// Level 1 — Surface (Card, ListTile)
  static const Color elevationSurface = Color(0xFF1A1A1A);

  /// Level 2 — Raised (TextField, Chip)
  static const Color elevationRaised = Color(0xFF242424);

  /// Level 3 — Overlay (BottomSheet, Dialog)
  static const Color elevationOverlay = Color(0xFF2A2A2A);

  /// Level 4 — Top (SnackBar, Toast)
  static const Color elevationTop = Color(0xFF333333);

  // ---------------------------------------------------------------------------
  // 구분선 / 테두리
  // ---------------------------------------------------------------------------

  /// 테두리, 구분선 — outline
  static const Color outline = Color(0xFF2A2A2A);

  // ---------------------------------------------------------------------------
  // 상태 배지 배경 (20% opacity 사전 정의)
  // ---------------------------------------------------------------------------

  /// 정상 배지 배경 (statusGreen 20%)
  static const Color statusGreenBg = Color(0x3334C759);

  /// 주의 배지 배경 (statusYellow 20%)
  static const Color statusYellowBg = Color(0x33FFD60A);

  /// 위험 배지 배경 (statusRed 20%)
  static const Color statusRedBg = Color(0x33FF3B30);

  /// 미확인 배지 배경 (statusGray 20%)
  static const Color statusGrayBg = Color(0x338E8E93);
}
