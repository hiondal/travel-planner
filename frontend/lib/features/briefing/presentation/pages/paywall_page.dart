import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../payment/presentation/providers/payment_provider.dart';

/// Paywall 화면 (SCR-023)
/// UFR-ALTN-050, UFR-PAY-010: 무료 티어 구독 유도
class PaywallPage extends ConsumerWidget {
  const PaywallPage({super.key, this.from});

  final String? from;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final plansAsync = ref.watch(subscriptionPlansProvider);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: const Text('프리미엄 플랜'),
      ),
      body: plansAsync.when(
        loading: () => const Center(
          child: CircularProgressIndicator(color: AppColors.accentPurple),
        ),
        error: (_, __) => _PaywallContent(plans: const [], from: from),
        data: (plans) => _PaywallContent(plans: plans, from: from),
      ),
    );
  }
}

class _PaywallContent extends StatelessWidget {
  const _PaywallContent({required this.plans, this.from});

  final List<dynamic> plans;
  final String? from;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 프리미엄 헤더 카드
          Container(
            padding: const EdgeInsets.all(AppSpacing.spaceXl),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                begin: Alignment(0.0, -1.0),
                end: Alignment(0.7, 1.0),
                colors: [AppColors.accentPurple, Color(0xFF4A1FB0)],
              ),
              borderRadius: BorderRadius.circular(AppSpacing.radiusPaywall),
            ),
            child: Column(
              children: [
                const Icon(
                  Icons.workspace_premium,
                  size: 48,
                  color: AppColors.accentAmber,
                ),
                const SizedBox(height: AppSpacing.spaceMd),
                Text(
                  'Pro로 업그레이드',
                  style: AppTypography.displayMedium.copyWith(
                    color: AppColors.textPrimary,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: AppSpacing.spaceSm),
                Text(
                  '실시간 대안 장소 추천과 더 많은 기능을\n무제한으로 사용하세요.',
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textPrimary.withOpacity(0.8),
                  ),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
          const SizedBox(height: AppSpacing.spaceXl),

          // 혜택 목록
          ..._benefits.map(
            (benefit) => Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.spaceSm),
              child: Row(
                children: [
                  const Icon(
                    Icons.check_circle,
                    size: 20,
                    color: AppColors.accentPurple,
                  ),
                  const SizedBox(width: AppSpacing.spaceSm),
                  Text(
                    benefit,
                    style: AppTypography.bodyMedium.copyWith(
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.space2xl),

          // Trip Pass 버튼
          SizedBox(
            height: AppSpacing.buttonHeight,
            child: ElevatedButton(
              onPressed: () => context.goNamed(
                AppRoutes.paymentCheckoutName,
                queryParameters: {'plan': 'trip_pass'},
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accentAmber,
                foregroundColor: AppColors.textInverse,
              ),
              child: const Text('Trip Pass 구매 (1회 여행)'),
            ),
          ),
          const SizedBox(height: AppSpacing.spaceMd),

          // Pro 구독 버튼
          SizedBox(
            height: AppSpacing.buttonHeight,
            child: ElevatedButton(
              onPressed: () => context.goNamed(
                AppRoutes.paymentCheckoutName,
                queryParameters: {'plan': 'pro'},
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accentPurple,
              ),
              child: const Text('Pro 구독 (월간)'),
            ),
          ),
          const SizedBox(height: AppSpacing.spaceMd),

          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(
              '나중에 하기',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.spaceXs),
          Text(
            '언제든지 취소 가능합니다.',
            textAlign: TextAlign.center,
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textDisabled,
            ),
          ),
        ],
      ),
    );
  }

  static const List<String> _benefits = [
    '실시간 대안 장소 3곳 즉시 추천',
    '1일 브리핑 무제한 제공',
    '상태 변화 즉시 Push 알림',
    '여행 일정 자동 최적화',
  ];
}
