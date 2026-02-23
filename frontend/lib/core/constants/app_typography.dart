import 'package:flutter/material.dart';

/// style-guide.md 4절 기반 타이포그래피 정의
/// Flutter TextTheme 필드 매핑 기준으로 작성한다.
abstract final class AppTypography {
  AppTypography._();

  static const String _fontFamily = 'Pretendard';

  // ---------------------------------------------------------------------------
  // 4-2. Flutter TextTheme 매핑
  // ---------------------------------------------------------------------------

  /// 28px / w700 / lh 36px / ls -0.5px
  /// 온보딩 메인 타이틀, 히어로 텍스트
  static const TextStyle displayLarge = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 28,
    fontWeight: FontWeight.w700,
    height: 1.29, // 36/28
    letterSpacing: -0.5,
  );

  /// 22px / w700 / lh 28px / ls -0.3px
  /// 화면 타이틀 (AppBar title)
  static const TextStyle displayMedium = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 22,
    fontWeight: FontWeight.w700,
    height: 1.27, // 28/22
    letterSpacing: -0.3,
  );

  /// 18px / w600 / lh 24px / ls -0.2px
  /// 섹션 타이틀, 카드 타이틀
  static const TextStyle displaySmall = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 18,
    fontWeight: FontWeight.w600,
    height: 1.33, // 24/18
    letterSpacing: -0.2,
  );

  /// 16px / w600 / lh 22px / ls 0px
  /// 서브 타이틀, 리스트 타이틀
  static const TextStyle headlineMedium = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 16,
    fontWeight: FontWeight.w600,
    height: 1.38, // 22/16
    letterSpacing: 0,
  );

  /// 15px / w400 / lh 22px / ls 0px
  /// 본문 텍스트 (bodyLarge 대응)
  static const TextStyle headlineSmall = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 15,
    fontWeight: FontWeight.w400,
    height: 1.47, // 22/15
    letterSpacing: 0,
  );

  /// 14px / w400 / lh 20px / ls 0.1px
  /// 보조 본문, 카드 설명
  static const TextStyle titleLarge = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 14,
    fontWeight: FontWeight.w400,
    height: 1.43, // 20/14
    letterSpacing: 0.1,
  );

  /// 12px / w400 / lh 16px / ls 0.2px
  /// 날짜, 메타 정보, 서브 레이블
  static const TextStyle titleMedium = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 12,
    fontWeight: FontWeight.w400,
    height: 1.33, // 16/12
    letterSpacing: 0.2,
  );

  /// 11px / w600 / lh 14px / ls 0.5px
  /// 배지 레이블, 카테고리 태그 (Overline)
  static const TextStyle titleSmall = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 11,
    fontWeight: FontWeight.w600,
    height: 1.27, // 14/11
    letterSpacing: 0.5,
  );

  /// 15px / w400 / lh 22px / ls 0px
  /// 본문 기본
  static const TextStyle bodyLarge = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 15,
    fontWeight: FontWeight.w400,
    height: 1.47, // 22/15
    letterSpacing: 0,
  );

  /// 14px / w400 / lh 20px / ls 0.1px
  /// 보조 본문
  static const TextStyle bodyMedium = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 14,
    fontWeight: FontWeight.w400,
    height: 1.43, // 20/14
    letterSpacing: 0.1,
  );

  /// 12px / w400 / lh 16px / ls 0.2px
  /// 캡션, 날짜, 메타
  static const TextStyle bodySmall = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 12,
    fontWeight: FontWeight.w400,
    height: 1.33, // 16/12
    letterSpacing: 0.2,
  );

  /// 16px / w600 / lh 22px / ls 0px
  /// ElevatedButton 텍스트
  static const TextStyle labelLarge = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 16,
    fontWeight: FontWeight.w600,
    height: 1.38, // 22/16
    letterSpacing: 0,
  );

  /// 14px / w600 / lh 20px / ls 0.1px
  /// OutlinedButton, TextButton 텍스트
  static const TextStyle labelMedium = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 14,
    fontWeight: FontWeight.w600,
    height: 1.43, // 20/14
    letterSpacing: 0.1,
  );

  /// 11px / w600 / lh 14px / ls 0.5px
  /// 칩, 배지 레이블
  static const TextStyle labelSmall = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 11,
    fontWeight: FontWeight.w600,
    height: 1.27, // 14/11
    letterSpacing: 0.5,
  );
}
