import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';

/// 결제 완료 화면
/// UFR-PAY-030: 결제 성공 확인 및 서비스 진입
class PaymentSuccessPage extends StatelessWidget {
  const PaymentSuccessPage({super.key, this.plan});

  final String? plan;

  @override
  Widget build(BuildContext context) {
    final isTripPass = plan == 'trip_pass';
    final planName = isTripPass ? 'Trip Pass' : 'Pro 구독';

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.spaceBase),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),

              // 성공 아이콘 (애니메이션 대용)
              Center(
                child: Container(
                  width: 120,
                  height: 120,
                  decoration: BoxDecoration(
                    color: AppColors.statusGreen.withOpacity(0.12),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.check_circle,
                    size: 64,
                    color: AppColors.statusGreen,
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.space2xl),

              // 축하 메시지
              Text(
                '결제가 완료되었습니다!',
                textAlign: TextAlign.center,
                style: AppTypography.displayLarge.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: AppSpacing.spaceSm),
              Text(
                '$planName을 구독하셨어요.\n이제 프리미엄 기능을 모두 사용할 수 있습니다.',
                textAlign: TextAlign.center,
                style: AppTypography.bodyLarge.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),

              const SizedBox(height: AppSpacing.space3xl),

              // 주요 혜택 요약
              Container(
                padding: const EdgeInsets.all(AppSpacing.spaceBase),
                decoration: BoxDecoration(
                  color: AppColors.bgCard,
                  borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
                ),
                child: Column(
                  children: [
                    Text(
                      '활성화된 혜택',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.spaceMd),
                    ..._getActivatedBenefits(isTripPass).map(
                      (benefit) => Padding(
                        padding:
                            const EdgeInsets.only(bottom: AppSpacing.spaceXs),
                        child: Row(
                          children: [
                            const Icon(
                              Icons.check_circle,
                              size: 16,
                              color: AppColors.statusGreen,
                            ),
                            const SizedBox(width: AppSpacing.spaceSm),
                            Text(
                              benefit,
                              style: AppTypography.bodySmall.copyWith(
                                color: AppColors.textPrimary,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),

              const Spacer(flex: 2),

              // 여행 시작 CTA
              SizedBox(
                height: AppSpacing.buttonHeight,
                child: ElevatedButton(
                  onPressed: () => context.goNamed(AppRoutes.tripListName),
                  child: const Text('여행 시작하기'),
                ),
              ),
              const SizedBox(height: AppSpacing.spaceMd),

              // 구독 관리 바로가기
              OutlinedButton(
                onPressed: () =>
                    context.goNamed(AppRoutes.subscriptionName),
                child: const Text('구독 관리'),
              ),

              const SizedBox(height: AppSpacing.spaceBase),
            ],
          ),
        ),
      ),
    );
  }

  List<String> _getActivatedBenefits(bool isTripPass) {
    if (isTripPass) {
      return [
        '실시간 대안 장소 3곳 즉시 추천',
        '이번 여행 브리핑 무제한',
        '상태 변화 Push 알림',
      ];
    }
    return [
      '실시간 대안 장소 3곳 즉시 추천',
      '1일 브리핑 무제한',
      '상태 변화 즉시 Push 알림',
      '여행 일정 자동 최적화',
    ];
  }
}
