import 'package:flutter/material.dart';

import '../../core/constants/app_colors.dart';
import '../../core/constants/app_spacing.dart';

/// 앱 공통 모달 래퍼
/// Dialog 스타일 전체 오버레이
abstract final class AppModal {
  AppModal._();

  static Future<T?> show<T>(
    BuildContext context, {
    required Widget child,
    bool barrierDismissible = true,
  }) {
    return showDialog<T>(
      context: context,
      barrierDismissible: barrierDismissible,
      barrierColor: AppColors.bgOverlay,
      builder: (ctx) => Dialog(
        backgroundColor: AppColors.elevationOverlay,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
        ),
        child: child,
      ),
    );
  }
}
