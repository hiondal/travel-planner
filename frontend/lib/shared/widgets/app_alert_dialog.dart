import 'package:flutter/material.dart';

import '../../core/constants/app_colors.dart';
import '../../core/constants/app_spacing.dart';

/// 앱 공통 알림 다이얼로그
/// style-guide.md 9-6 Dialog 열림 전환 기반
class AppAlertDialog extends StatelessWidget {
  const AppAlertDialog({
    super.key,
    required this.title,
    this.content,
    required this.confirmLabel,
    this.cancelLabel,
    this.onConfirm,
    this.onCancel,
    this.isDestructive = false,
  });

  final String title;
  final String? content;
  final String confirmLabel;
  final String? cancelLabel;
  final VoidCallback? onConfirm;
  final VoidCallback? onCancel;
  final bool isDestructive;

  static Future<bool?> show(
    BuildContext context, {
    required String title,
    String? content,
    required String confirmLabel,
    String? cancelLabel,
    bool isDestructive = false,
  }) {
    return showDialog<bool>(
      context: context,
      barrierColor: AppColors.bgOverlay,
      builder: (ctx) => AppAlertDialog(
        title: title,
        content: content,
        confirmLabel: confirmLabel,
        cancelLabel: cancelLabel,
        onConfirm: () => Navigator.of(ctx).pop(true),
        onCancel: () => Navigator.of(ctx).pop(false),
        isDestructive: isDestructive,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: AppColors.elevationOverlay,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
      ),
      title: Text(title),
      content: content != null ? Text(content!) : null,
      actions: [
        if (cancelLabel != null)
          TextButton(
            onPressed: onCancel ?? () => Navigator.of(context).pop(false),
            child: Text(cancelLabel!),
          ),
        TextButton(
          onPressed: onConfirm ?? () => Navigator.of(context).pop(true),
          style: TextButton.styleFrom(
            foregroundColor:
                isDestructive ? AppColors.statusRed : AppColors.accentRed,
          ),
          child: Text(confirmLabel),
        ),
      ],
    );
  }
}
