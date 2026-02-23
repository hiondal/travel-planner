import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../shared/providers/app_user_provider.dart';
import '../../../../shared/models/subscription_tier.dart';
import '../../../../shared/widgets/app_snack_bar.dart';
import '../providers/payment_provider.dart';

/// 결제 화면 (IAP 연동)
/// UFR-PAY-020: 선택한 플랜 결제 처리
class PaymentCheckoutPage extends ConsumerStatefulWidget {
  const PaymentCheckoutPage({super.key, this.plan});

  /// 결제 플랜 ('trip_pass' | 'pro')
  final String? plan;

  @override
  ConsumerState<PaymentCheckoutPage> createState() =>
      _PaymentCheckoutPageState();
}

class _PaymentCheckoutPageState extends ConsumerState<PaymentCheckoutPage> {
  bool _agreedToTerms = false;

  @override
  Widget build(BuildContext context) {
    final purchaseState = ref.watch(purchaseNotifierProvider);
    final planId = widget.plan ?? 'pro';
    final isTripPass = planId == 'trip_pass';

    final planName = isTripPass ? 'Trip Pass' : 'Pro 구독';
    final planPrice = isTripPass ? 9900 : 12900;
    final planPeriod = isTripPass ? '1회 여행' : '매월';
    final accentColor =
        isTripPass ? AppColors.accentAmber : AppColors.accentPurple;
    final priceFormat = NumberFormat('#,###', 'ko').format(planPrice);

    return Scaffold(
      appBar: AppBar(title: const Text('결제')),
      body: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(AppSpacing.spaceBase),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // 플랜 요약 카드
                  Container(
                    padding: const EdgeInsets.all(AppSpacing.spaceXl),
                    decoration: BoxDecoration(
                      color: AppColors.bgCard,
                      borderRadius:
                          BorderRadius.circular(AppSpacing.radiusCard),
                      border: Border.all(
                        color: accentColor.withOpacity(0.3),
                        width: 1,
                      ),
                    ),
                    child: Column(
                      children: [
                        Icon(
                          Icons.workspace_premium,
                          size: 40,
                          color: accentColor,
                        ),
                        const SizedBox(height: AppSpacing.spaceMd),
                        Text(
                          planName,
                          style: AppTypography.displayMedium.copyWith(
                            color: AppColors.textPrimary,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.spaceXs),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(
                              '₩$priceFormat',
                              style: AppTypography.displayLarge.copyWith(
                                color: accentColor,
                              ),
                            ),
                            Padding(
                              padding: const EdgeInsets.only(
                                  left: 4, bottom: 4),
                              child: Text(
                                '/ $planPeriod',
                                style: AppTypography.bodyMedium.copyWith(
                                  color: AppColors.textSecondary,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: AppSpacing.spaceXl),

                  // 결제 혜택
                  Text(
                    '포함 혜택',
                    style: AppTypography.displaySmall.copyWith(
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.spaceMd),
                  ..._getBenefits(isTripPass).map(
                    (benefit) => Padding(
                      padding:
                          const EdgeInsets.only(bottom: AppSpacing.spaceSm),
                      child: Row(
                        children: [
                          Icon(
                            Icons.check_circle,
                            size: 18,
                            color: accentColor,
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

                  const SizedBox(height: AppSpacing.spaceXl),
                  const Divider(height: 1),
                  const SizedBox(height: AppSpacing.spaceBase),

                  // 결제 방법 (Prism Mock: 시뮬레이션)
                  Text(
                    '결제 방법',
                    style: AppTypography.displaySmall.copyWith(
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.spaceMd),
                  Container(
                    padding: const EdgeInsets.all(AppSpacing.spaceBase),
                    decoration: BoxDecoration(
                      color: AppColors.bgCard,
                      borderRadius:
                          BorderRadius.circular(AppSpacing.radiusCard),
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.payment,
                          color: AppColors.textSecondary,
                          size: 24,
                        ),
                        const SizedBox(width: AppSpacing.spaceBase),
                        Text(
                          'In-App Purchase (시뮬레이션)',
                          style: AppTypography.bodyMedium.copyWith(
                            color: AppColors.textPrimary,
                          ),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: AppSpacing.spaceXl),

                  // 이용약관 동의
                  CheckboxListTile(
                    value: _agreedToTerms,
                    onChanged: (val) =>
                        setState(() => _agreedToTerms = val ?? false),
                    title: Text(
                      '이용약관 및 개인정보 처리방침에 동의합니다.',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textPrimary,
                      ),
                    ),
                    activeColor: AppColors.accentPurple,
                    controlAffinity: ListTileControlAffinity.leading,
                    contentPadding: EdgeInsets.zero,
                  ),

                  if (!isTripPass) ...[
                    const SizedBox(height: AppSpacing.spaceSm),
                    Text(
                      '구독은 만료 24시간 전 자동으로 갱신됩니다.\n언제든지 취소할 수 있습니다.',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textDisabled,
                      ),
                    ),
                  ],

                  const SizedBox(height: AppSpacing.space2xl),
                ],
              ),
            ),
          ),

          // 결제 CTA
          Container(
            padding: const EdgeInsets.fromLTRB(
              AppSpacing.spaceBase,
              AppSpacing.spaceMd,
              AppSpacing.spaceBase,
              AppSpacing.space2xl,
            ),
            decoration: const BoxDecoration(
              color: AppColors.bgPrimary,
              border: Border(
                top: BorderSide(color: AppColors.bgCard, width: 1),
              ),
            ),
            child: SizedBox(
              height: AppSpacing.buttonHeight,
              child: ElevatedButton(
                onPressed: (purchaseState.isLoading || !_agreedToTerms)
                    ? null
                    : () => _processPurchase(planId),
                style: ElevatedButton.styleFrom(
                  backgroundColor: accentColor,
                ),
                child: purchaseState.isLoading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : Text('₩$priceFormat 결제하기'),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _processPurchase(String planId) async {
    final result = await ref.read(purchaseNotifierProvider.notifier).purchase(
          planId: planId,
          receiptData: 'mock_receipt_${planId}_${DateTime.now().millisecondsSinceEpoch}',
          platform: 'android',
        );

    if (!mounted) return;

    if (result != null) {
      // 구독 티어 업데이트
      final tier = planId == 'pro'
          ? SubscriptionTier.pro
          : SubscriptionTier.tripPass;
      ref.read(appUserProvider.notifier).updateSubscriptionTier(tier);

      context.goNamed(
        AppRoutes.paymentSuccessName,
        queryParameters: {'plan': planId},
      );
    } else {
      AppSnackBar.showError(context, '결제에 실패했습니다. 다시 시도해 주세요.');
    }
  }

  List<String> _getBenefits(bool isTripPass) {
    if (isTripPass) {
      return [
        '실시간 대안 장소 3곳 즉시 추천',
        '1회 여행 기간 동안 무제한 브리핑',
        '상태 변화 Push 알림',
      ];
    }
    return [
      '실시간 대안 장소 3곳 즉시 추천',
      '1일 브리핑 무제한 제공',
      '상태 변화 즉시 Push 알림',
      '여행 일정 자동 최적화',
      '월간 여행 횟수 제한 없음',
    ];
  }
}
