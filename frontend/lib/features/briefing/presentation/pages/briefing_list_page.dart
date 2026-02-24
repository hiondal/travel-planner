import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../shared/widgets/app_skeleton.dart';
import '../../../../shared/models/status_level.dart';
import '../../../../shared/widgets/status_badge.dart';
import '../../domain/models/briefing_model.dart';
import '../providers/briefing_provider.dart';

/// 브리핑 목록 화면 (SCR-020)
/// UFR-BRIF-060: 브리핑 목록 카드 TAB2
class BriefingListPage extends ConsumerWidget {
  const BriefingListPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final briefingsAsync = ref.watch(briefingListProvider());

    return Scaffold(
      appBar: AppBar(title: const Text('브리핑')),
      body: briefingsAsync.when(
        loading: () => const _BriefingListSkeleton(),
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
              Text(
                '브리핑을 불러오지 못했습니다.',
                style: AppTypography.bodyLarge,
              ),
              const SizedBox(height: AppSpacing.spaceBase),
              OutlinedButton(
                onPressed: () => ref.invalidate(briefingListProvider()),
                child: const Text('다시 시도'),
              ),
            ],
          ),
        ),
        data: (briefings) => briefings.isEmpty
            ? _EmptyBriefingView()
            : RefreshIndicator(
                color: AppColors.accentRed,
                backgroundColor: AppColors.bgCard,
                onRefresh: () async {
                  ref.invalidate(briefingListProvider());
                  await ref.read(briefingListProvider().future);
                },
                child: ListView.separated(
                  padding: const EdgeInsets.all(AppSpacing.spaceBase),
                  itemCount: briefings.length,
                  separatorBuilder: (_, __) =>
                      const SizedBox(height: AppSpacing.spaceMd),
                  itemBuilder: (context, index) {
                    return _BriefingCard(briefing: briefings[index]);
                  },
                ),
              ),
      ),
    );
  }
}

class _BriefingCard extends StatelessWidget {
  const _BriefingCard({required this.briefing});

  final Briefing briefing;

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('MM.dd HH:mm', 'ko');

    return InkWell(
      onTap: () => context.goNamed(
        AppRoutes.briefingDetailName,
        pathParameters: {'briefingId': briefing.briefingId},
      ),
      borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
      child: Container(
        padding: const EdgeInsets.all(AppSpacing.spaceBase),
        decoration: BoxDecoration(
          color: AppColors.bgCard,
          borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
          border: briefing.overallStatus?.name == 'danger'
              ? Border.all(color: AppColors.statusRed.withOpacity(0.4))
              : null,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 헤더: 날짜 + 배지
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  dateFormat.format(briefing.createdAt),
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
                Row(
                  children: [
                    if (!briefing.isRead)
                      Container(
                        width: 8,
                        height: 8,
                        margin: const EdgeInsets.only(right: AppSpacing.spaceXs),
                        decoration: const BoxDecoration(
                          color: AppColors.accentRed,
                          shape: BoxShape.circle,
                        ),
                      ),
                    StatusBadge(level: briefing.overallStatus ?? StatusLevel.unknown),
                  ],
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.spaceSm),
            // 요약 텍스트
            Text(
              briefing.summary ?? briefing.placeName,
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textPrimary,
              ),
              maxLines: 3,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}

class _DetailStatusRow extends StatelessWidget {
  const _DetailStatusRow({required this.details});

  final BriefingDetails details;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        _DetailIcon(
          icon: Icons.wb_sunny_outlined,
          status: details.weather.status,
        ),
        const SizedBox(width: AppSpacing.spaceMd),
        _DetailIcon(
          icon: Icons.people_outline,
          status: details.crowding.status,
        ),
        const SizedBox(width: AppSpacing.spaceMd),
        _DetailIcon(
          icon: Icons.access_time_outlined,
          status: details.businessHours.status,
        ),
        const SizedBox(width: AppSpacing.spaceMd),
        _DetailIcon(
          icon: Icons.directions_car_outlined,
          status: details.traffic.status,
        ),
      ],
    );
  }
}

class _DetailIcon extends StatelessWidget {
  const _DetailIcon({required this.icon, required this.status});

  final IconData icon;
  final dynamic status;

  @override
  Widget build(BuildContext context) {
    return Icon(icon, size: 16, color: (status as dynamic).color as Color);
  }
}

class _BriefingListSkeleton extends StatelessWidget {
  const _BriefingListSkeleton();

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      itemCount: 4,
      separatorBuilder: (_, __) => const SizedBox(height: AppSpacing.spaceMd),
      itemBuilder: (_, __) => const BriefingCardSkeleton(),
    );
  }
}

class _EmptyBriefingView extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.notifications_none,
            size: 64,
            color: AppColors.textDisabled,
          ),
          const SizedBox(height: AppSpacing.spaceXl),
          Text(
            '아직 브리핑이 없습니다',
            style: AppTypography.displaySmall.copyWith(
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: AppSpacing.spaceSm),
          Text(
            '여행 일정을 추가하면 출발 전 브리핑을 받을 수 있습니다.',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
