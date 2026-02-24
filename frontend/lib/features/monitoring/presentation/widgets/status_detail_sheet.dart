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
import '../../domain/models/monitoring_model.dart';
import '../providers/monitoring_provider.dart';

/// 상태 상세 바텀시트 (BS-002)
/// UFR-MNTR-040: 4개 항목 개별 상태, 판정 사유, "대안 보기" CTA
class StatusDetailSheet extends ConsumerWidget {
  const StatusDetailSheet({
    super.key,
    required this.placeId,
    required this.placeName,
    required this.tripId,
  });

  final String placeId;
  final String placeName;
  final String tripId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final statusAsync = ref.watch(placeStatusProvider(placeId));

    return DraggableScrollableSheet(
      initialChildSize: 0.55,
      minChildSize: 0.4,
      maxChildSize: 0.9,
      expand: false,
      builder: (context, scrollController) {
        return SingleChildScrollView(
          controller: scrollController,
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.spaceBase,
          ),
          child: statusAsync.when(
            loading: () => _LoadingBody(placeName: placeName),
            error: (err, _) => _ErrorBody(placeName: placeName),
            data: (status) => _StatusBody(
              status: status,
              tripId: tripId,
              onClose: () => Navigator.of(context).pop(),
            ),
          ),
        );
      },
    );
  }
}

/// 핸들 바 공통 헤더
class _SheetHandle extends StatelessWidget {
  const _SheetHandle();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        margin: const EdgeInsets.only(top: 12, bottom: 20),
        width: AppSpacing.bottomSheetHandleWidth,
        height: AppSpacing.bottomSheetHandleHeight,
        decoration: BoxDecoration(
          color: AppColors.textDisabled,
          borderRadius: BorderRadius.circular(2),
        ),
      ),
    );
  }
}

class _LoadingBody extends StatelessWidget {
  const _LoadingBody({required this.placeName});

  final String placeName;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _SheetHandle(),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(placeName, style: Theme.of(context).textTheme.displaySmall),
            AppSkeleton(width: 64, height: 24, borderRadius: 12),
          ],
        ),
        const SizedBox(height: AppSpacing.spaceXl),
        ...List.generate(
          4,
          (_) => Padding(
            padding: const EdgeInsets.only(bottom: AppSpacing.spaceMd),
            child: Row(
              children: [
                AppSkeleton(width: 20, height: 20, borderRadius: 10),
                const SizedBox(width: AppSpacing.spaceSm),
                AppSkeleton(width: 80, height: 14),
                const Spacer(),
                AppSkeleton(width: 48, height: 22, borderRadius: 11),
              ],
            ),
          ),
        ),
        const SizedBox(height: AppSpacing.space2xl),
      ],
    );
  }
}

class _ErrorBody extends StatelessWidget {
  const _ErrorBody({required this.placeName});

  final String placeName;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _SheetHandle(),
        Text(placeName, style: Theme.of(context).textTheme.displaySmall),
        const SizedBox(height: AppSpacing.spaceXl),
        Center(
          child: Column(
            children: [
              const Icon(Icons.cloud_off, size: 40, color: AppColors.textDisabled),
              const SizedBox(height: AppSpacing.spaceSm),
              Text(
                '상태 정보를 불러올 수 없습니다.',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.space2xl),
      ],
    );
  }
}

class _StatusBody extends StatelessWidget {
  const _StatusBody({
    required this.status,
    required this.tripId,
    required this.onClose,
  });

  final PlaceStatus status;
  final String tripId;
  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    final timeFormat = DateFormat('HH:mm', 'ko');
    final isActionNeeded = status.overallStatus == StatusLevel.caution ||
        status.overallStatus == StatusLevel.danger;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _SheetHandle(),
        // 헤더: 장소명 + 종합 배지
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Expanded(
              child: Text(
                status.placeName.isNotEmpty ? status.placeName : '장소 상태',
                style: Theme.of(context).textTheme.displaySmall,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const SizedBox(width: AppSpacing.spaceSm),
            StatusBadge(level: status.overallStatus),
          ],
        ),
        const SizedBox(height: AppSpacing.spaceXs),
        Text(
          '업데이트: ${timeFormat.format(status.updatedAt)}',
          style: AppTypography.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        const SizedBox(height: AppSpacing.spaceXl),

        // 4개 상태 항목
        _StatusDetailItem(
          icon: Icons.wb_sunny_outlined,
          label: '날씨',
          detail: status.weather,
        ),
        _StatusDetailItem(
          icon: Icons.people_outline,
          label: '혼잡도',
          detail: status.congestion,
        ),
        _StatusDetailItem(
          icon: Icons.access_time_outlined,
          label: '영업시간',
          detail: status.businessStatus,
        ),
        _StatusDetailItem(
          icon: Icons.directions_car_outlined,
          label: '교통',
          detail: status.travelTime,
        ),

        // 판정 사유
        if (status.reason != null && status.reason!.isNotEmpty) ...[
          const SizedBox(height: AppSpacing.spaceBase),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(AppSpacing.spaceMd),
            decoration: BoxDecoration(
              color: AppColors.bgInput,
              borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '판정 사유',
                  style: AppTypography.labelSmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(height: AppSpacing.spaceXs),
                Text(
                  status.reason!,
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textPrimary,
                  ),
                ),
              ],
            ),
          ),
        ],

        const SizedBox(height: AppSpacing.spaceXl),

        // CTA 버튼
        if (isActionNeeded)
          SizedBox(
            width: double.infinity,
            height: AppSpacing.buttonHeight,
            child: ElevatedButton(
              onPressed: () {
                onClose();
                context.goNamed(
                  AppRoutes.briefingListName,
                );
              },
              child: const Text('대안 보기'),
            ),
          )
        else
          SizedBox(
            width: double.infinity,
            height: AppSpacing.buttonHeight,
            child: OutlinedButton(
              onPressed: onClose,
              child: const Text('확인'),
            ),
          ),
        SizedBox(height: MediaQuery.of(context).padding.bottom + 16),
      ],
    );
  }
}

/// 개별 상태 항목 행
class _StatusDetailItem extends StatelessWidget {
  const _StatusDetailItem({
    required this.icon,
    required this.label,
    required this.detail,
  });

  final IconData icon;
  final String label;
  final StatusDetail detail;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.spaceMd),
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
          if (detail.message != null && detail.message!.isNotEmpty) ...[
            const SizedBox(width: AppSpacing.spaceXs),
            Expanded(
              child: Text(
                detail.message!,
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ] else
            const Spacer(),
          StatusBadge(level: detail.status),
        ],
      ),
    );
  }
}
