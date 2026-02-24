import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../shared/models/status_level.dart';
import '../../../../shared/widgets/app_skeleton.dart';
import '../../../../shared/widgets/status_badge.dart';
import '../../domain/models/briefing_model.dart';
import '../providers/briefing_provider.dart';

/// 브리핑 상세 화면 (SCR-021)
/// UFR-BRIF-050: 3가지 변형 (안심/주의/만료)
class BriefingDetailPage extends ConsumerWidget {
  const BriefingDetailPage({super.key, required this.briefingId});

  final String briefingId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final briefingAsync = ref.watch(briefingDetailProvider(briefingId));

    return Scaffold(
      appBar: AppBar(title: const Text('브리핑 상세')),
      body: briefingAsync.when(
        loading: () => const _BriefingDetailSkeleton(),
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
                onPressed: () => ref.invalidate(briefingDetailProvider(briefingId)),
                child: const Text('다시 시도'),
              ),
            ],
          ),
        ),
        data: (briefing) => _BriefingDetailBody(
          briefing: briefing,
          briefingId: briefingId,
        ),
      ),
    );
  }
}

class _BriefingDetailBody extends StatelessWidget {
  const _BriefingDetailBody({
    required this.briefing,
    required this.briefingId,
  });

  final Briefing briefing;
  final String briefingId;

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('yyyy년 MM월 dd일 HH:mm', 'ko');
    final isActionNeeded = briefing.overallStatus == StatusLevel.caution ||
        briefing.overallStatus == StatusLevel.danger;
    final content = briefing.content;
    final isExpired = briefing.isExpired;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 헤더
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  dateFormat.format(briefing.createdAt),
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ),
              StatusBadge(level: briefing.overallStatus ?? StatusLevel.unknown),
            ],
          ),
          const SizedBox(height: AppSpacing.spaceBase),

          // 만료 배너
          if (isExpired) ...[
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(AppSpacing.spaceMd),
              decoration: BoxDecoration(
                color: AppColors.textDisabled.withOpacity(0.2),
                borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
              ),
              child: Row(
                children: [
                  const Icon(
                    Icons.history,
                    size: 16,
                    color: AppColors.textSecondary,
                  ),
                  const SizedBox(width: AppSpacing.spaceXs),
                  Text(
                    '이 브리핑은 만료되었습니다.',
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.spaceBase),
          ],

          // 요약 텍스트
          Text(
            briefing.summary ?? briefing.placeName,
            style: AppTypography.bodyLarge.copyWith(
              color: AppColors.textPrimary,
            ),
          ),

          // 세부 상태 (content가 있을 때만)
          if (content != null) ...[
            const SizedBox(height: AppSpacing.spaceXl),
            Text(
              '상세 현황',
              style: AppTypography.displaySmall.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.spaceMd),
            if (content.weather != null)
              _SimpleDetailRow(
                icon: Icons.wb_sunny_outlined,
                label: '날씨',
                value: content.weather!,
              ),
            if (content.congestion != null)
              _SimpleDetailRow(
                icon: Icons.people_outline,
                label: '혼잡도',
                value: content.congestion!,
              ),
            if (content.businessStatus != null)
              _SimpleDetailRow(
                icon: Icons.access_time_outlined,
                label: '영업시간',
                value: content.businessStatus!,
              ),
            if (content.travelTime != null)
              _SimpleDetailRow(
                icon: Icons.directions_car_outlined,
                label: '이동 시간',
                value: '도보 ${content.travelTime!['walking_minutes'] ?? '-'}분',
              ),
          ],

          const SizedBox(height: AppSpacing.space2xl),

          // CTA 버튼
          if (isExpired)
            SizedBox(
              width: double.infinity,
              height: AppSpacing.buttonHeight,
              child: OutlinedButton(
                onPressed: () => context.goNamed(AppRoutes.tripListName),
                child: const Text('최신 상태 조회'),
              ),
            )
          else if (isActionNeeded)
            SizedBox(
              width: double.infinity,
              height: AppSpacing.buttonHeight,
              child: ElevatedButton(
                onPressed: () => context.goNamed(
                  AppRoutes.alternativeCardName,
                  pathParameters: {'briefingId': briefingId},
                ),
                child: const Text('대안 보기'),
              ),
            )
          else
            SizedBox(
              width: double.infinity,
              height: AppSpacing.buttonHeight,
              child: OutlinedButton(
                onPressed: () => context.goNamed(AppRoutes.briefingListName),
                child: const Text('안심 확인 완료'),
              ),
            ),
        ],
      ),
    );
  }
}

class _DetailItemRow extends StatelessWidget {
  const _DetailItemRow({
    required this.icon,
    required this.label,
    required this.detail,
  });

  final IconData icon;
  final String label;
  final StatusItem detail;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.spaceMd),
      child: Container(
        padding: const EdgeInsets.all(AppSpacing.spaceMd),
        decoration: BoxDecoration(
          color: AppColors.bgCard,
          borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
        ),
        child: Row(
          children: [
            Icon(icon, size: 20, color: detail.status.color),
            const SizedBox(width: AppSpacing.spaceSm),
            Text(
              label,
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const Spacer(),
            if (detail.message.isNotEmpty)
              Flexible(
                child: Text(
                  detail.message,
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.end,
                ),
              ),
            const SizedBox(width: AppSpacing.spaceSm),
            StatusBadge(level: detail.status),
          ],
        ),
      ),
    );
  }
}

class _SimpleDetailRow extends StatelessWidget {
  const _SimpleDetailRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.spaceMd),
      child: Container(
        padding: const EdgeInsets.all(AppSpacing.spaceMd),
        decoration: BoxDecoration(
          color: AppColors.bgCard,
          borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
        ),
        child: Row(
          children: [
            Icon(icon, size: 20, color: AppColors.textSecondary),
            const SizedBox(width: AppSpacing.spaceSm),
            Text(
              label,
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const Spacer(),
            Flexible(
              child: Text(
                value,
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.end,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _BriefingDetailSkeleton extends StatelessWidget {
  const _BriefingDetailSkeleton();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              AppSkeleton(width: 120, height: 12),
              AppSkeleton(width: 64, height: 24, borderRadius: 12),
            ],
          ),
          const SizedBox(height: AppSpacing.spaceBase),
          AppSkeleton(width: double.infinity, height: 15),
          const SizedBox(height: AppSpacing.spaceXs),
          AppSkeleton(width: double.infinity, height: 15),
          const SizedBox(height: AppSpacing.spaceXs),
          AppSkeleton(width: 200, height: 15),
        ],
      ),
    );
  }
}
