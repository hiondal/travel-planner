import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../shared/providers/app_user_provider.dart';
import '../../../../shared/widgets/app_skeleton.dart';
import '../../../payment/domain/models/payment_model.dart';
import '../../../payment/presentation/providers/payment_provider.dart';

/// 구독 관리 화면 (SCR-031)
/// UFR-PAY-010: 현재 구독 정보 및 플랜 변경 유도
class SubscriptionPage extends ConsumerWidget {
  const SubscriptionPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final userState = ref.watch(appUserProvider);
    final statusAsync = ref.watch(subscriptionStatusProvider);
    final plansAsync = ref.watch(subscriptionPlansProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('구독 관리')),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.spaceBase),
        children: [
          // 현재 구독 상태 카드
          statusAsync.when(
            loading: () => AppSkeleton(
              width: double.infinity,
              height: 120,
              borderRadius: AppSpacing.radiusCard,
            ),
            error: (_, __) => _StatusErrorCard(),
            data: (status) => _CurrentPlanCard(
              tier: userState.subscriptionTier.displayName,
              isPremium: userState.isPremium,
              expiresAt: status.expiresAt,
              autoRenew: status.autoRenew,
            ),
          ),

          const SizedBox(height: AppSpacing.space2xl),

          // 이용 가능 플랜 섹션
          Text(
            '이용 가능한 플랜',
            style: AppTypography.displaySmall.copyWith(
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: AppSpacing.spaceBase),

          plansAsync.when(
            loading: () => Column(
              children: List.generate(
                2,
                (_) => Padding(
                  padding: const EdgeInsets.only(bottom: AppSpacing.spaceMd),
                  child: AppSkeleton(
                    width: double.infinity,
                    height: 120,
                    borderRadius: AppSpacing.radiusCard,
                  ),
                ),
              ),
            ),
            error: (_, __) => const Center(
              child: Text(
                '플랜 정보를 불러오지 못했습니다.',
                style: TextStyle(color: AppColors.textSecondary),
              ),
            ),
            data: (plans) => Column(
              children: plans
                  .map(
                    (plan) => Padding(
                      padding:
                          const EdgeInsets.only(bottom: AppSpacing.spaceMd),
                      child: _PlanCard(
                        plan: plan,
                        isCurrentPlan: _isCurrentPlan(
                          plan.planId,
                          userState.subscriptionTier.name,
                        ),
                        onSelect: () => context.goNamed(
                          AppRoutes.paymentCheckoutName,
                          queryParameters: {'plan': plan.planId},
                        ),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),

          const SizedBox(height: AppSpacing.spaceBase),

          // 구독 취소 안내
          if (userState.isPremium) ...[
            const Divider(height: 1),
            const SizedBox(height: AppSpacing.spaceBase),
            Text(
              '구독 취소는 앱스토어/플레이스토어에서 직접 진행해 주세요.\n취소 후에도 만료일까지 Pro 기능을 사용할 수 있습니다.',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
              textAlign: TextAlign.center,
            ),
          ],

          const SizedBox(height: AppSpacing.space2xl),
        ],
      ),
    );
  }

  bool _isCurrentPlan(String planId, String tierName) {
    if (planId == 'pro' && tierName == 'pro') return true;
    if (planId == 'trip_pass' && tierName == 'tripPass') return true;
    return false;
  }
}

class _CurrentPlanCard extends StatelessWidget {
  const _CurrentPlanCard({
    required this.tier,
    required this.isPremium,
    this.expiresAt,
    required this.autoRenew,
  });

  final String tier;
  final bool isPremium;
  final DateTime? expiresAt;
  final bool autoRenew;

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('yyyy년 MM월 dd일', 'ko');

    return Container(
      padding: const EdgeInsets.all(AppSpacing.spaceXl),
      decoration: BoxDecoration(
        gradient: isPremium
            ? const LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [AppColors.accentPurple, Color(0xFF4A1FB0)],
              )
            : null,
        color: isPremium ? null : AppColors.bgCard,
        borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                Icons.workspace_premium,
                color: isPremium ? AppColors.accentAmber : AppColors.textSecondary,
                size: 24,
              ),
              const SizedBox(width: AppSpacing.spaceSm),
              Text(
                '현재 플랜',
                style: AppTypography.bodySmall.copyWith(
                  color: isPremium
                      ? AppColors.textPrimary.withOpacity(0.7)
                      : AppColors.textSecondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.spaceSm),
          Text(
            tier,
            style: AppTypography.displayMedium.copyWith(
              color: AppColors.textPrimary,
            ),
          ),
          if (expiresAt != null) ...[
            const SizedBox(height: AppSpacing.spaceXs),
            Text(
              '${dateFormat.format(expiresAt!)} 만료 · ${autoRenew ? '자동 갱신' : '갱신 안함'}',
              style: AppTypography.bodySmall.copyWith(
                color: isPremium
                    ? AppColors.textPrimary.withOpacity(0.7)
                    : AppColors.textSecondary,
              ),
            ),
          ],
          if (!isPremium) ...[
            const SizedBox(height: AppSpacing.spaceMd),
            Text(
              '프리미엄 플랜으로 업그레이드하고\n더 많은 기능을 사용해보세요.',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _PlanCard extends StatelessWidget {
  const _PlanCard({
    required this.plan,
    required this.isCurrentPlan,
    required this.onSelect,
  });

  final SubscriptionPlan plan;
  final bool isCurrentPlan;
  final VoidCallback onSelect;

  @override
  Widget build(BuildContext context) {
    final isTripPass = plan.planId == 'trip_pass';
    final accentColor =
        isTripPass ? AppColors.accentAmber : AppColors.accentPurple;
    final priceFormat =
        NumberFormat('#,###', 'ko').format(plan.price);

    return Container(
      decoration: BoxDecoration(
        color: AppColors.bgCard,
        borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
        border: isCurrentPlan
            ? Border.all(color: accentColor, width: 1.5)
            : null,
      ),
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                plan.planName,
                style: AppTypography.headlineMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
              if (isCurrentPlan)
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.spaceSm,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: accentColor.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    '현재 플랜',
                    style: AppTypography.labelSmall.copyWith(
                      color: accentColor,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: AppSpacing.spaceXs),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                '₩$priceFormat',
                style: AppTypography.displaySmall.copyWith(
                  color: accentColor,
                ),
              ),
              const SizedBox(width: 4),
              Padding(
                padding: const EdgeInsets.only(bottom: 2),
                child: Text(
                  plan.billingPeriod == 'monthly' ? '/월' : '/1회 여행',
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.spaceMd),

          // 혜택 목록
          ...plan.features.map<Widget>(
            (feature) => Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.spaceXs),
              child: Row(
                children: [
                  Icon(Icons.check, size: 14, color: accentColor),
                  const SizedBox(width: AppSpacing.spaceXs),
                  Expanded(
                    child: Text(
                      feature,
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: AppSpacing.spaceMd),

          SizedBox(
            width: double.infinity,
            height: AppSpacing.buttonHeight,
            child: ElevatedButton(
              onPressed: isCurrentPlan ? null : onSelect,
              style: ElevatedButton.styleFrom(
                backgroundColor: accentColor,
                disabledBackgroundColor: AppColors.bgInput,
              ),
              child: Text(isCurrentPlan ? '현재 사용 중' : '선택하기'),
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusErrorCard extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      decoration: BoxDecoration(
        color: AppColors.bgCard,
        borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
      ),
      child: Text(
        '구독 정보를 불러오지 못했습니다.',
        style: AppTypography.bodyMedium.copyWith(
          color: AppColors.textSecondary,
        ),
      ),
    );
  }
}
