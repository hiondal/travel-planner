import 'package:flutter/material.dart';

import '../../core/constants/app_colors.dart';
import '../../core/constants/app_spacing.dart';
import '../../core/constants/app_typography.dart';

/// 앱 공통 배지 위젯 (숫자 카운트 배지)
/// style-guide.md 6-10 기반
class AppBadge extends StatelessWidget {
  const AppBadge({
    super.key,
    required this.child,
    this.count,
    this.showBadge = true,
  });

  final Widget child;
  final int? count;
  final bool showBadge;

  @override
  Widget build(BuildContext context) {
    if (!showBadge) return child;
    return Stack(
      clipBehavior: Clip.none,
      children: [
        child,
        Positioned(
          top: -4,
          right: -4,
          child: Container(
            constraints: const BoxConstraints(minWidth: 20, minHeight: 20),
            padding: const EdgeInsets.symmetric(horizontal: 6),
            decoration: const BoxDecoration(
              color: AppColors.accentRed,
              borderRadius: BorderRadius.all(Radius.circular(10)),
            ),
            child: Center(
              child: count != null
                  ? Text(
                      count! > 99 ? '99+' : '$count',
                      style: AppTypography.labelSmall.copyWith(
                        color: AppColors.textPrimary,
                      ),
                    )
                  : const SizedBox.shrink(),
            ),
          ),
        ),
      ],
    );
  }
}

/// 앱 공통 태그 위젯 (상태/카테고리 태그)
/// style-guide.md 6-9 Chip 기반
class AppTag extends StatelessWidget {
  const AppTag({
    super.key,
    required this.label,
    this.color,
    this.backgroundColor,
    this.icon,
  });

  final String label;
  final Color? color;
  final Color? backgroundColor;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: 10,
        vertical: 4,
      ),
      decoration: BoxDecoration(
        color: backgroundColor ?? AppColors.bgInput,
        borderRadius: BorderRadius.circular(AppSpacing.radiusChip),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 14, color: color ?? AppColors.textPrimary),
            const SizedBox(width: 4),
          ],
          Text(
            label,
            style: AppTypography.labelSmall.copyWith(
              color: color ?? AppColors.textPrimary,
            ),
          ),
        ],
      ),
    );
  }
}
