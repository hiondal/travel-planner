import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../shared/widgets/app_alert_dialog.dart';
import '../../../../shared/widgets/app_skeleton.dart';
import '../../../../shared/widgets/app_snack_bar.dart';
import '../../../../shared/widgets/status_badge.dart';
import '../../domain/models/trip_model.dart';
import '../providers/trip_provider.dart';

/// 여행 목록 화면 (SCR-010)
/// UFR-SCHD-050: 여행 목록 조회
class TripListPage extends ConsumerWidget {
  const TripListPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tripsAsync = ref.watch(tripListProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('내 여행'),
      ),
      body: tripsAsync.when(
        loading: () => const _TripListSkeleton(),
        error: (err, _) => _ErrorView(
          message: err.toString(),
          onRetry: () => ref.invalidate(tripListProvider),
        ),
        data: (trips) => trips.isEmpty
            ? _EmptyTripView(
                onCreateTap: () => context.goNamed(AppRoutes.tripCreateName),
              )
            : _TripListView(
                trips: trips,
                onRetry: () => ref.invalidate(tripListProvider),
              ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => context.goNamed(AppRoutes.tripCreateName),
        backgroundColor: AppColors.accentRed,
        child: const Icon(Icons.add, color: AppColors.textPrimary),
      ),
    );
  }
}

class _TripListView extends ConsumerWidget {
  const _TripListView({required this.trips, required this.onRetry});

  final List<Trip> trips;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return RefreshIndicator(
      color: AppColors.accentRed,
      backgroundColor: AppColors.bgCard,
      onRefresh: () async {
        ref.invalidate(tripListProvider);
        await ref.read(tripListProvider.future);
      },
      child: ListView.separated(
        padding: const EdgeInsets.all(AppSpacing.spaceBase),
        itemCount: trips.length,
        separatorBuilder: (_, __) => const SizedBox(height: AppSpacing.spaceMd),
        itemBuilder: (context, index) {
          final trip = trips[index];
          return Dismissible(
            key: Key(trip.tripId),
            direction: DismissDirection.endToStart,
            background: Container(
              alignment: Alignment.centerRight,
              padding: const EdgeInsets.only(right: AppSpacing.spaceXl),
              decoration: BoxDecoration(
                color: AppColors.statusRed,
                borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
              ),
              child: const Icon(
                Icons.delete_outline,
                color: AppColors.textPrimary,
                size: 28,
              ),
            ),
            confirmDismiss: (_) => _confirmDelete(context, ref, trip),
            child: _TripCard(trip: trip),
          );
        },
      ),
    );
  }

  Future<bool> _confirmDelete(
    BuildContext context,
    WidgetRef ref,
    Trip trip,
  ) async {
    final confirmed = await AppAlertDialog.show(
      context,
      title: '여행 삭제',
      content: '\'${trip.tripName}\' 여행을 삭제할까요?\n관련된 모든 일정이 함께 삭제됩니다.',
      confirmLabel: '삭제',
      cancelLabel: '취소',
      isDestructive: true,
    );
    if (confirmed != true) return false;

    final notifier = ref.read(tripDeleteNotifierProvider.notifier);
    final success = await notifier.deleteTrip(trip.tripId);
    if (context.mounted) {
      if (success) {
        AppSnackBar.show(context, '\'${trip.tripName}\' 여행이 삭제되었습니다.');
      } else {
        AppSnackBar.showError(context, '여행 삭제에 실패했습니다.');
      }
    }
    return false; // Dismissible 자체 삭제는 하지 않음 (provider가 목록 갱신)
  }
}

class _TripCard extends StatelessWidget {
  const _TripCard({required this.trip});

  final Trip trip;

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('MM.dd', 'ko');
    final dateRange =
        '${dateFormat.format(trip.startDate)} - ${dateFormat.format(trip.endDate)}';

    return InkWell(
      onTap: () => context.goNamed(
        AppRoutes.scheduleDetailName,
        pathParameters: {'tripId': trip.tripId},
        queryParameters: {
          'startDate': trip.startDate.toIso8601String().substring(0, 10),
          'endDate': trip.endDate.toIso8601String().substring(0, 10),
        },
      ),
      borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
      child: Container(
        padding: const EdgeInsets.all(AppSpacing.spaceBase),
        decoration: BoxDecoration(
          color: AppColors.bgCard,
          borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
        ),
        child: Row(
          children: [
            // 썸네일 또는 아이콘
            Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(
                color: AppColors.bgInput,
                borderRadius: BorderRadius.circular(AppSpacing.spaceSm),
              ),
              child: const Icon(
                Icons.flight_takeoff,
                size: 28,
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(width: AppSpacing.spaceMd),
            // 여행 정보
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    trip.tripName,
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
                        Icons.location_on_outlined,
                        size: 14,
                        color: AppColors.textSecondary,
                      ),
                      const SizedBox(width: 2),
                      Text(
                        trip.city,
                        style: AppTypography.bodySmall.copyWith(
                          color: AppColors.textSecondary,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.spaceSm),
                      Text(
                        dateRange,
                        style: AppTypography.bodySmall.copyWith(
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                  if (trip.totalPlaces > 0) ...[
                    const SizedBox(height: AppSpacing.spaceXs),
                    Text(
                      '장소 ${trip.totalPlaces}개',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ],
              ),
            ),
            // 상태 배지
            StatusBadge(level: trip.status),
          ],
        ),
      ),
    );
  }
}

class _EmptyTripView extends StatelessWidget {
  const _EmptyTripView({required this.onCreateTap});

  final VoidCallback onCreateTap;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.spaceBase),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.flight_takeoff,
              size: 64,
              color: AppColors.textDisabled,
            ),
            const SizedBox(height: AppSpacing.spaceXl),
            Text(
              '아직 여행이 없습니다',
              style: AppTypography.displaySmall.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.spaceSm),
            Text(
              '새 여행을 만들고 일정을 등록해 보세요.',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.space2xl),
            SizedBox(
              width: double.infinity,
              height: AppSpacing.buttonHeight,
              child: ElevatedButton.icon(
                onPressed: onCreateTap,
                icon: const Icon(Icons.add),
                label: const Text('첫 여행 만들기'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TripListSkeleton extends StatelessWidget {
  const _TripListSkeleton();

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      itemCount: 4,
      separatorBuilder: (_, __) => const SizedBox(height: AppSpacing.spaceMd),
      itemBuilder: (_, __) => Container(
        height: 88,
        padding: const EdgeInsets.all(AppSpacing.spaceBase),
        decoration: BoxDecoration(
          color: AppColors.bgCard,
          borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
        ),
        child: Row(
          children: [
            AppSkeleton(width: 56, height: 56, borderRadius: 8),
            const SizedBox(width: AppSpacing.spaceMd),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  AppSkeleton(width: 140, height: 16),
                  const SizedBox(height: AppSpacing.spaceXs),
                  AppSkeleton(width: 100, height: 12),
                ],
              ),
            ),
            AppSkeleton(width: 56, height: 24, borderRadius: 12),
          ],
        ),
      ),
    );
  }
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.spaceBase),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 48, color: AppColors.statusRed),
            const SizedBox(height: AppSpacing.spaceBase),
            Text(
              '데이터를 불러오지 못했습니다.',
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.spaceXl),
            OutlinedButton(
              onPressed: onRetry,
              child: const Text('다시 시도'),
            ),
          ],
        ),
      ),
    );
  }
}
