import 'package:flutter/material.dart';

import '../../core/constants/app_colors.dart';
import '../../core/constants/app_spacing.dart';

/// 앱 공통 스낵바 유틸
/// style-guide.md 9-9 토스트/SnackBar 기반
abstract final class AppSnackBar {
  AppSnackBar._();

  static void show(
    BuildContext context,
    String message, {
    String? actionLabel,
    VoidCallback? onAction,
    Duration duration = const Duration(seconds: 3),
    bool isError = false,
  }) {
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: duration,
        backgroundColor:
            isError ? AppColors.statusRed : AppColors.elevationTop,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSnackBar),
        ),
        action: actionLabel != null
            ? SnackBarAction(
                label: actionLabel,
                textColor: AppColors.accentBlue,
                onPressed: onAction ?? () {},
              )
            : null,
      ),
    );
  }

  static void showError(BuildContext context, String message) {
    show(context, message, isError: true);
  }
}
