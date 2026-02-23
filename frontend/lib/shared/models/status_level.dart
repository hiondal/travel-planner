import 'package:flutter/material.dart';

import '../../core/constants/app_colors.dart';

/// 상태 배지 레벨 열거형
/// style-guide.md 3-3 상태 배지 컬러 기반
enum StatusLevel {
  /// 정상 — 모든 항목 정상
  safe,

  /// 주의 — 1개 이상 주의 항목 존재
  caution,

  /// 위험 — 1개 이상 위험 항목 존재
  danger,

  /// 미확인 — 데이터 수집 불가
  unknown;

  /// 배지 전경 색상 (텍스트 및 아이콘)
  Color get color => switch (this) {
        StatusLevel.safe => AppColors.statusGreen,
        StatusLevel.caution => AppColors.statusYellow,
        StatusLevel.danger => AppColors.statusRed,
        StatusLevel.unknown => AppColors.statusGray,
      };

  /// 배지 배경 색상 (20% opacity)
  Color get backgroundColor => switch (this) {
        StatusLevel.safe => AppColors.statusGreenBg,
        StatusLevel.caution => AppColors.statusYellowBg,
        StatusLevel.danger => AppColors.statusRedBg,
        StatusLevel.unknown => AppColors.statusGrayBg,
      };

  /// 배지 아이콘 (색약 접근성: 색상 + 아이콘 조합 필수)
  IconData get icon => switch (this) {
        StatusLevel.safe => Icons.check_circle,
        StatusLevel.caution => Icons.warning_amber,
        StatusLevel.danger => Icons.cancel,
        StatusLevel.unknown => Icons.help,
      };

  /// 배지 레이블 텍스트
  String get label => switch (this) {
        StatusLevel.safe => '정상',
        StatusLevel.caution => '주의',
        StatusLevel.danger => '위험',
        StatusLevel.unknown => '미확인',
      };

  /// Semantics 레이블 (스크린 리더용)
  String get semanticsLabel => switch (this) {
        StatusLevel.safe => '상태: 정상',
        StatusLevel.caution => '상태: 주의',
        StatusLevel.danger => '상태: 위험',
        StatusLevel.unknown => '상태: 미확인',
      };

  /// JSON 값에서 변환
  static StatusLevel fromString(String value) => switch (value) {
        'SAFE' || 'safe' || 'green' => StatusLevel.safe,
        'CAUTION' || 'caution' || 'yellow' => StatusLevel.caution,
        'DANGER' || 'danger' || 'red' => StatusLevel.danger,
        _ => StatusLevel.unknown,
      };
}
