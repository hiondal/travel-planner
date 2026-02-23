import 'package:flutter/material.dart';

import '../../core/constants/app_colors.dart';
import '../../core/constants/app_spacing.dart';
import '../../core/constants/app_typography.dart';

/// 앱 공통 버튼 위젯
/// style-guide.md 6-1 기반
enum AppButtonVariant { elevated, outlined, text }

class AppButton extends StatelessWidget {
  const AppButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.variant = AppButtonVariant.elevated,
    this.isFullWidth = false,
    this.isLoading = false,
    this.icon,
    this.backgroundColor,
    this.foregroundColor,
  });

  final String label;
  final VoidCallback? onPressed;
  final AppButtonVariant variant;
  final bool isFullWidth;
  final bool isLoading;
  final IconData? icon;
  final Color? backgroundColor;
  final Color? foregroundColor;

  @override
  Widget build(BuildContext context) {
    final child = isLoading
        ? const SizedBox(
            width: 20,
            height: 20,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              color: AppColors.textPrimary,
            ),
          )
        : icon != null
            ? Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(icon, size: 20),
                  const SizedBox(width: AppSpacing.spaceSm),
                  Text(label),
                ],
              )
            : Text(label);

    Widget button;
    switch (variant) {
      case AppButtonVariant.elevated:
        button = ElevatedButton(
          onPressed: isLoading ? null : onPressed,
          style: backgroundColor != null
              ? ElevatedButton.styleFrom(
                  backgroundColor: backgroundColor,
                  foregroundColor: foregroundColor ?? AppColors.textPrimary,
                  minimumSize: const Size(120, AppSpacing.buttonHeight),
                  padding: const EdgeInsets.symmetric(horizontal: 24),
                  shape: RoundedRectangleBorder(
                    borderRadius:
                        BorderRadius.circular(AppSpacing.radiusButton),
                  ),
                  textStyle: AppTypography.labelLarge,
                  elevation: 0,
                )
              : null,
          child: child,
        );
      case AppButtonVariant.outlined:
        button = OutlinedButton(
          onPressed: isLoading ? null : onPressed,
          child: child,
        );
      case AppButtonVariant.text:
        button = TextButton(
          onPressed: isLoading ? null : onPressed,
          child: child,
        );
    }

    if (isFullWidth) {
      return SizedBox(width: double.infinity, child: button);
    }
    return button;
  }
}
