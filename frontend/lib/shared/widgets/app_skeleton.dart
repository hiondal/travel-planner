import 'package:flutter/material.dart';

import '../../core/constants/app_colors.dart';
import '../../core/constants/app_spacing.dart';

/// 스켈레톤 UI 기본 블록
/// style-guide.md 9-8 Skeleton UI 기반
/// Shimmer: #1A1A1A → #2A2A2A → #1A1A1A 반복 (1.5s)
class AppSkeleton extends StatefulWidget {
  const AppSkeleton({
    super.key,
    required this.width,
    required this.height,
    this.borderRadius,
  });

  final double? width;
  final double height;
  final double? borderRadius;

  @override
  State<AppSkeleton> createState() => _AppSkeletonState();
}

class _AppSkeletonState extends State<AppSkeleton>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _shimmerAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat();
    _shimmerAnimation = Tween<double>(begin: -1.5, end: 1.5).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _shimmerAnimation,
      builder: (context, child) {
        return Container(
          width: widget.width,
          height: widget.height,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(
              widget.borderRadius ?? AppSpacing.radiusCard,
            ),
            gradient: LinearGradient(
              begin: Alignment.centerLeft,
              end: Alignment.centerRight,
              colors: const [
                AppColors.bgCard,
                AppColors.bgHover,
                AppColors.bgCard,
              ],
              stops: [
                (_shimmerAnimation.value - 0.3).clamp(0.0, 1.0),
                (_shimmerAnimation.value).clamp(0.0, 1.0),
                (_shimmerAnimation.value + 0.3).clamp(0.0, 1.0),
              ],
            ),
          ),
        );
      },
    );
  }
}

/// 브리핑 카드 스켈레톤
class BriefingCardSkeleton extends StatelessWidget {
  const BriefingCardSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      decoration: BoxDecoration(
        color: AppColors.bgCard,
        borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              AppSkeleton(width: 120, height: 14),
              AppSkeleton(width: 64, height: 24, borderRadius: 12),
            ],
          ),
          const SizedBox(height: AppSpacing.spaceMd),
          AppSkeleton(width: double.infinity, height: 14),
          const SizedBox(height: AppSpacing.spaceXs),
          AppSkeleton(width: double.infinity, height: 14),
          const SizedBox(height: AppSpacing.spaceXs),
          AppSkeleton(width: 200, height: 14),
          const SizedBox(height: AppSpacing.spaceBase),
          AppSkeleton(width: double.infinity, height: AppSpacing.buttonHeight),
        ],
      ),
    );
  }
}

/// 일정 타임라인 아이템 스켈레톤
class ScheduleItemSkeleton extends StatelessWidget {
  const ScheduleItemSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          // 시간축
          SizedBox(
            width: 48,
            child: Column(
              children: [
                AppSkeleton(width: 32, height: 12, borderRadius: 4),
              ],
            ),
          ),
          const SizedBox(width: AppSpacing.spaceSm),
          // 카드
          Expanded(
            child: Container(
              padding: const EdgeInsets.all(AppSpacing.spaceMd),
              decoration: BoxDecoration(
                color: AppColors.bgCard,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  AppSkeleton(width: 56, height: 56, borderRadius: 8),
                  const SizedBox(width: AppSpacing.spaceMd),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        AppSkeleton(width: 120, height: 15),
                        const SizedBox(height: 6),
                        AppSkeleton(width: 80, height: 12),
                      ],
                    ),
                  ),
                  AppSkeleton(width: 56, height: 24, borderRadius: 12),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
