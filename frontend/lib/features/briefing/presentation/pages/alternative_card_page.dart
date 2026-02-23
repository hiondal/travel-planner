import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../shared/providers/app_user_provider.dart';
import '../../../../shared/widgets/app_skeleton.dart';
import '../../../../shared/widgets/app_snack_bar.dart';
import '../../domain/models/briefing_model.dart';
import '../providers/briefing_provider.dart';

/// 대안 카드 화면 (SCR-022)
/// UFR-ALTN-020, UFR-ALTN-030: 대안 장소 카드 3장
class AlternativeCardPage extends ConsumerWidget {
  const AlternativeCardPage({super.key, required this.briefingId});

  final String briefingId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final userState = ref.watch(appUserProvider);
    final alternativesAsync = ref.watch(alternativeListProvider(briefingId));

    // 무료 티어 → Paywall 리다이렉트
    if (!userState.isPremium) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        context.goNamed(
          AppRoutes.paywallName,
          queryParameters: {'from': 'alternative'},
        );
      });
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(title: const Text('대안 장소')),
      body: alternativesAsync.when(
        loading: () => _AlternativeSkeleton(),
        error: (err, _) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.error_outline,
                size: 48,
                color: AppColors.statusRed,
              ),
              const SizedBox(height: AppSpacing.spaceBase),
              OutlinedButton(
                onPressed: () =>
                    ref.invalidate(alternativeListProvider(briefingId)),
                child: const Text('다시 시도'),
              ),
            ],
          ),
        ),
        data: (alternatives) => alternatives.isEmpty
            ? _EmptyAlternativeView()
            : _AlternativeListView(
                alternatives: alternatives,
                briefingId: briefingId,
              ),
      ),
    );
  }
}

class _AlternativeListView extends ConsumerWidget {
  const _AlternativeListView({
    required this.alternatives,
    required this.briefingId,
  });

  final List<Alternative> alternatives;
  final String briefingId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return ListView(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      children: [
        Text(
          '추천 대안 장소',
          style: AppTypography.displaySmall.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: AppSpacing.spaceXs),
        Text(
          '현재 위치 기준 최적의 대안을 선택하세요.',
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        const SizedBox(height: AppSpacing.spaceXl),
        ...alternatives.map((alt) => Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.spaceMd),
              child: _AlternativeCard(
                alternative: alt,
                onSelect: () => _applyAlternative(context, ref, alt),
              ),
            )),
        const SizedBox(height: AppSpacing.spaceBase),
        OutlinedButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('기존 일정 유지'),
        ),
      ],
    );
  }

  Future<void> _applyAlternative(
    BuildContext context,
    WidgetRef ref,
    Alternative alt,
  ) async {
    final notifier = ref.read(applyAlternativeNotifierProvider.notifier);
    final result = await notifier.apply(
      alternativeId: alt.alternativeId,
      tripId: '',
      scheduleItemId: '',
    );
    if (!context.mounted) return;
    if (result != null) {
      AppSnackBar.show(context, '${alt.placeName}으로 변경되었습니다.');
      context.goNamed(AppRoutes.tripListName);
    } else {
      AppSnackBar.showError(context, '장소 교체에 실패했습니다.');
    }
  }
}

class _AlternativeCard extends StatelessWidget {
  const _AlternativeCard({
    required this.alternative,
    required this.onSelect,
  });

  final Alternative alternative;
  final VoidCallback onSelect;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.bgCard,
        borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 이미지 영역
          Stack(
            children: [
              Container(
                height: AppSpacing.alternativeCardImageHeight,
                width: double.infinity,
                color: AppColors.bgInput,
                child: const Icon(
                  Icons.image,
                  size: 48,
                  color: AppColors.textDisabled,
                ),
              ),
              // 그라디언트 오버레이
              Positioned.fill(
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [
                        Colors.transparent,
                        Colors.black.withOpacity(0.7),
                      ],
                    ),
                  ),
                ),
              ),
              // 카테고리 칩
              Positioned(
                top: 8,
                left: 8,
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.bgCard.withOpacity(0.8),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    alternative.category,
                    style: AppTypography.labelSmall.copyWith(
                      color: AppColors.textPrimary,
                    ),
                  ),
                ),
              ),
              // 거리 + 이동 시간
              Positioned(
                top: 8,
                right: 8,
                child: Text(
                  '${alternative.distanceKm.toStringAsFixed(1)}km · ${alternative.estimatedMinutes}분',
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
            ],
          ),
          // 텍스트 영역
          Padding(
            padding: const EdgeInsets.fromLTRB(
              AppSpacing.spaceBase,
              AppSpacing.spaceMd,
              AppSpacing.spaceBase,
              AppSpacing.spaceBase,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  alternative.placeName,
                  style: AppTypography.headlineMedium.copyWith(
                    color: AppColors.textPrimary,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: AppSpacing.spaceXs),
                Row(
                  children: [
                    const Icon(
                      Icons.star,
                      size: 14,
                      color: AppColors.accentAmber,
                    ),
                    const SizedBox(width: 2),
                    Text(
                      '${alternative.rating} (${alternative.reviewCount})',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.spaceXs),
                Text(
                  alternative.reason,
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: AppSpacing.spaceMd),
                SizedBox(
                  width: double.infinity,
                  height: AppSpacing.buttonHeight,
                  child: ElevatedButton(
                    onPressed: onSelect,
                    child: const Text('이 장소로 변경'),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _AlternativeSkeleton extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      itemCount: 3,
      separatorBuilder: (_, __) => const SizedBox(height: AppSpacing.spaceMd),
      itemBuilder: (_, __) => Container(
        decoration: BoxDecoration(
          color: AppColors.bgCard,
          borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            AppSkeleton(
              width: double.infinity,
              height: AppSpacing.alternativeCardImageHeight,
              borderRadius: 0,
            ),
            Padding(
              padding: const EdgeInsets.all(AppSpacing.spaceBase),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  AppSkeleton(width: 140, height: 16),
                  const SizedBox(height: AppSpacing.spaceXs),
                  AppSkeleton(width: 80, height: 12),
                  const SizedBox(height: AppSpacing.spaceMd),
                  AppSkeleton(
                    width: double.infinity,
                    height: AppSpacing.buttonHeight,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyAlternativeView extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.search_off,
            size: 48,
            color: AppColors.textDisabled,
          ),
          const SizedBox(height: AppSpacing.spaceBase),
          Text(
            '현재 추천 가능한 대안이 없습니다.',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}
