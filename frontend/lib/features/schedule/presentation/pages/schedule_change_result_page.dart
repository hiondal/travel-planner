import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';

/// 장소 교체 결과 화면 (SCR-016)
/// UFR-SCHD-040: 대안 선택 후 교체 완료 확인
class ScheduleChangeResultPage extends StatelessWidget {
  const ScheduleChangeResultPage({
    super.key,
    required this.tripId,
    this.alternativeId,
  });

  final String tripId;
  final String? alternativeId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('장소 교체 완료')),
      body: Padding(
        padding: const EdgeInsets.all(AppSpacing.spaceBase),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 성공 아이콘
            Container(
              width: 100,
              height: 100,
              margin: const EdgeInsets.symmetric(horizontal: 130),
              decoration: BoxDecoration(
                color: AppColors.statusGreen.withOpacity(0.15),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.check_circle,
                size: 56,
                color: AppColors.statusGreen,
              ),
            ),
            const SizedBox(height: AppSpacing.space2xl),
            Text(
              '일정이 변경되었습니다',
              textAlign: TextAlign.center,
              style: AppTypography.displayLarge.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.spaceSm),
            Text(
              '선택하신 장소로 일정이 업데이트되었습니다.\n새 일정을 확인해 보세요.',
              textAlign: TextAlign.center,
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.space3xl),
            // CTA 버튼
            SizedBox(
              height: AppSpacing.buttonHeight,
              child: ElevatedButton(
                onPressed: () => context.goNamed(
                  AppRoutes.scheduleDetailName,
                  pathParameters: {'tripId': tripId},
                ),
                child: const Text('일정표 확인'),
              ),
            ),
            const SizedBox(height: AppSpacing.spaceMd),
            OutlinedButton(
              onPressed: () => context.goNamed(AppRoutes.briefingListName),
              child: const Text('브리핑으로 돌아가기'),
            ),
          ],
        ),
      ),
    );
  }
}
