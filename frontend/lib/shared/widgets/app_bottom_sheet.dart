import 'package:flutter/material.dart';

import '../../core/constants/app_colors.dart';
import '../../core/constants/app_spacing.dart';

/// 앱 공통 바텀시트 래퍼
/// style-guide.md 6-3 BottomSheet 기반
abstract final class AppBottomSheet {
  AppBottomSheet._();

  static Future<T?> show<T>(
    BuildContext context, {
    required Widget child,
    bool isScrollControlled = true,
    bool isDismissible = true,
    bool enableDrag = true,
    double? maxHeightFraction,
  }) {
    return showModalBottomSheet<T>(
      context: context,
      isScrollControlled: isScrollControlled,
      isDismissible: isDismissible,
      enableDrag: enableDrag,
      backgroundColor: AppColors.elevationOverlay,
      barrierColor: AppColors.bgOverlay,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppSpacing.radiusBottomSheet),
        ),
      ),
      builder: (ctx) => _BottomSheetContent(
        maxHeightFraction: maxHeightFraction ?? 0.9,
        child: child,
      ),
    );
  }
}

class _BottomSheetContent extends StatelessWidget {
  const _BottomSheetContent({
    required this.child,
    required this.maxHeightFraction,
  });

  final Widget child;
  final double maxHeightFraction;

  @override
  Widget build(BuildContext context) {
    final maxHeight =
        MediaQuery.of(context).size.height * maxHeightFraction;
    return ConstrainedBox(
      constraints: BoxConstraints(maxHeight: maxHeight),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 핸들 바
          Padding(
            padding: const EdgeInsets.only(top: 12, bottom: 8),
            child: Container(
              width: AppSpacing.bottomSheetHandleWidth,
              height: AppSpacing.bottomSheetHandleHeight,
              decoration: BoxDecoration(
                color: AppColors.textDisabled,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          Flexible(child: child),
        ],
      ),
    );
  }
}
