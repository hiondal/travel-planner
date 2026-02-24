import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../shared/widgets/app_alert_dialog.dart';
import '../../../../shared/widgets/app_bottom_sheet.dart';
import '../../../../shared/widgets/app_skeleton.dart';
import '../../../../shared/widgets/app_snack_bar.dart';
import '../../../../shared/widgets/status_badge.dart';
import '../../../monitoring/presentation/widgets/status_detail_sheet.dart';
import '../../domain/models/trip_model.dart';
import '../providers/trip_provider.dart';

/// 일정표 화면 (SCR-013)
/// UFR-SCHD-050: 일별 타임라인, 배지 4단계 색상
/// UFR-MNTR-030: 상태 배지 실시간 표시
class ScheduleDetailPage extends ConsumerStatefulWidget {
  const ScheduleDetailPage({
    super.key,
    required this.tripId,
    required this.startDate,
    required this.endDate,
  });

  final String tripId;
  final DateTime startDate;
  final DateTime endDate;

  @override
  ConsumerState<ScheduleDetailPage> createState() =>
      _ScheduleDetailPageState();
}

class _ScheduleDetailPageState extends ConsumerState<ScheduleDetailPage> {
  late DateTime _selectedDate;

  @override
  void initState() {
    super.initState();
    _selectedDate = DateTime(widget.startDate.year, widget.startDate.month, widget.startDate.day);
  }

  @override
  Widget build(BuildContext context) {
    final scheduleAsync = ref.watch(
      scheduleProvider(widget.tripId, targetDate: _selectedDate),
    );

    return Scaffold(
      appBar: AppBar(
        title: const Text('일정표'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.invalidate(
              scheduleProvider(widget.tripId, targetDate: _selectedDate),
            ),
            tooltip: '새로 고침',
          ),
        ],
      ),
      body: Column(
        children: [
          // 날짜 선택 탭
          _DateSelectorRow(
            startDate: widget.startDate,
            endDate: widget.endDate,
            selectedDate: _selectedDate,
            onDateSelected: (date) {
              setState(() => _selectedDate = date);
            },
          ),
          const Divider(height: 1),
          // 타임라인
          Expanded(
            child: scheduleAsync.when(
              loading: () => ListView.builder(
                padding: const EdgeInsets.all(AppSpacing.spaceBase),
                itemCount: 4,
                itemBuilder: (_, __) => Padding(
                  padding: const EdgeInsets.only(bottom: AppSpacing.spaceSm),
                  child: const ScheduleItemSkeleton(),
                ),
              ),
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
                      '일정을 불러오지 못했습니다.',
                      style: AppTypography.bodyLarge,
                    ),
                    const SizedBox(height: AppSpacing.spaceBase),
                    OutlinedButton(
                      onPressed: () => ref.invalidate(
                        scheduleProvider(widget.tripId,
                            targetDate: _selectedDate),
                      ),
                      child: const Text('다시 시도'),
                    ),
                  ],
                ),
              ),
              data: (items) => items.isEmpty
                  ? _EmptyScheduleView(
                      onAddTap: () => context.go(
                        '${AppRoutes.placeSearch(widget.tripId)}?startDate=${widget.startDate.toIso8601String().substring(0, 10)}&endDate=${widget.endDate.toIso8601String().substring(0, 10)}',
                      ),
                    )
                  : _ScheduleTimeline(
                      tripId: widget.tripId,
                      items: items,
                      onItemTap: (item) => _showStatusDetail(item),
                      onItemDelete: (item) => _deleteItem(item),
                    ),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => context.go(
          '${AppRoutes.placeSearch(widget.tripId)}?startDate=${widget.startDate.toIso8601String().substring(0, 10)}&endDate=${widget.endDate.toIso8601String().substring(0, 10)}',
        ),
        backgroundColor: AppColors.accentRed,
        child: const Icon(Icons.add_location_alt, color: AppColors.textPrimary),
      ),
    );
  }

  Future<void> _showStatusDetail(ScheduleItem item) async {
    await AppBottomSheet.show(
      context,
      child: StatusDetailSheet(
        placeId: item.placeId,
        placeName: item.placeName,
        tripId: widget.tripId,
      ),
    );
  }

  Future<void> _deleteItem(ScheduleItem item) async {
    final confirmed = await AppAlertDialog.show(
      context,
      title: '장소 삭제',
      content: '${item.placeName}을(를) 일정에서 삭제할까요?',
      confirmLabel: '삭제',
      cancelLabel: '취소',
      isDestructive: true,
    );
    if (confirmed != true) return;
    if (!mounted) return;

    final notifier = ref.read(scheduleItemDeleteNotifierProvider.notifier);
    await notifier.delete(
      tripId: widget.tripId,
      scheduleItemId: item.scheduleItemId,
    );
    if (mounted) {
      ref.invalidate(scheduleProvider(widget.tripId, targetDate: _selectedDate));
      AppSnackBar.show(context, '${item.placeName}이(가) 삭제되었습니다.');
    }
  }
}

/// 날짜 선택 행
class _DateSelectorRow extends StatelessWidget {
  const _DateSelectorRow({
    required this.startDate,
    required this.endDate,
    required this.selectedDate,
    required this.onDateSelected,
  });

  final DateTime startDate;
  final DateTime endDate;
  final DateTime selectedDate;
  final ValueChanged<DateTime> onDateSelected;

  @override
  Widget build(BuildContext context) {
    final dayCount = endDate.difference(startDate).inDays + 1;
    final dates = List.generate(dayCount, (i) {
      final d = startDate.add(Duration(days: i));
      return DateTime(d.year, d.month, d.day);
    });

    return SizedBox(
      height: 64,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.spaceBase,
          vertical: 8,
        ),
        itemCount: dates.length,
        itemBuilder: (context, index) {
          final date = dates[index];
          final isSelected = date.year == selectedDate.year &&
              date.month == selectedDate.month &&
              date.day == selectedDate.day;
          return Padding(
            padding: const EdgeInsets.only(right: AppSpacing.spaceSm),
            child: InkWell(
              onTap: () => onDateSelected(date),
              borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.spaceMd,
                  vertical: AppSpacing.spaceXs,
                ),
                decoration: BoxDecoration(
                  color: isSelected ? AppColors.accentRed : AppColors.bgInput,
                  borderRadius:
                      BorderRadius.circular(AppSpacing.radiusButton),
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      DateFormat('M/d').format(date),
                      style: AppTypography.labelSmall.copyWith(
                        color: isSelected
                            ? AppColors.textPrimary
                            : AppColors.textSecondary,
                      ),
                    ),
                    Text(
                      DateFormat('E', 'ko').format(date),
                      style: AppTypography.labelSmall.copyWith(
                        color: isSelected
                            ? AppColors.textPrimary
                            : AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

/// 일정 타임라인
class _ScheduleTimeline extends StatelessWidget {
  const _ScheduleTimeline({
    required this.tripId,
    required this.items,
    required this.onItemTap,
    required this.onItemDelete,
  });

  final String tripId;
  final List<ScheduleItem> items;
  final ValueChanged<ScheduleItem> onItemTap;
  final ValueChanged<ScheduleItem> onItemDelete;

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      itemCount: items.length,
      itemBuilder: (context, index) {
        final item = items[index];
        return Dismissible(
          key: Key(item.scheduleItemId),
          direction: DismissDirection.endToStart,
          background: Container(
            alignment: Alignment.centerRight,
            padding: const EdgeInsets.only(right: AppSpacing.spaceBase),
            color: AppColors.statusRed,
            child: const Icon(
              Icons.delete_outline,
              color: AppColors.textPrimary,
              size: 24,
            ),
          ),
          confirmDismiss: (_) async {
            onItemDelete(item);
            return false; // 실제 삭제는 Alert 확인 후 처리
          },
          child: Padding(
            padding: const EdgeInsets.only(bottom: AppSpacing.spaceMd),
            child: _ScheduleTimelineItem(
              item: item,
              onTap: () => onItemTap(item),
            ),
          ),
        );
      },
    );
  }
}

/// 타임라인 개별 아이템
class _ScheduleTimelineItem extends StatelessWidget {
  const _ScheduleTimelineItem({
    required this.item,
    required this.onTap,
  });

  final ScheduleItem item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final timeFormat = DateFormat('HH:mm');
    final endTime = item.scheduledAt
        .add(Duration(minutes: item.durationMinutes));

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 시간축 (48px 고정)
        SizedBox(
          width: AppSpacing.timelineAxisWidth,
          child: Column(
            children: [
              Text(
                timeFormat.format(item.scheduledAt),
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              Text(
                timeFormat.format(endTime),
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textDisabled,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: AppSpacing.spaceSm),
        // 세로 연결선
        Column(
          children: [
            Container(
              width: 2,
              height: 72,
              color: _isActiveStatus(item)
                  ? AppColors.accentRed
                  : AppColors.outline,
            ),
          ],
        ),
        const SizedBox(width: AppSpacing.spaceSm),
        // 장소 카드
        Expanded(
          child: InkWell(
            onTap: onTap,
            borderRadius: BorderRadius.circular(AppSpacing.spaceSm),
            child: Container(
              padding: const EdgeInsets.all(AppSpacing.spaceMd),
              decoration: BoxDecoration(
                color: AppColors.bgCard,
                borderRadius: BorderRadius.circular(AppSpacing.spaceSm),
              ),
              child: Row(
                children: [
                  // 썸네일
                  Container(
                    width: AppSpacing.thumbnailSize,
                    height: AppSpacing.thumbnailSize,
                    decoration: BoxDecoration(
                      color: AppColors.bgInput,
                      borderRadius: BorderRadius.circular(
                        AppSpacing.spaceSm,
                      ),
                    ),
                    child: const Icon(
                      Icons.place,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  const SizedBox(width: AppSpacing.spaceMd),
                  // 장소 정보
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          item.placeName,
                          style: AppTypography.headlineSmall.copyWith(
                            color: AppColors.textPrimary,
                            fontWeight: FontWeight.w600,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          '${item.category} · ${item.durationMinutes}분',
                          style: AppTypography.bodySmall.copyWith(
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),
                  // 상태 배지 (animate: true for live update)
                  StatusBadge(
                    level: item.status,
                    animate: true,
                    onTap: onTap,
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }

  bool _isActiveStatus(ScheduleItem item) {
    final now = DateTime.now();
    return now.isAfter(item.scheduledAt) &&
        now.isBefore(
          item.scheduledAt.add(Duration(minutes: item.durationMinutes)),
        );
  }
}

class _EmptyScheduleView extends StatelessWidget {
  const _EmptyScheduleView({required this.onAddTap});

  final VoidCallback onAddTap;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.spaceBase),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.add_location_alt,
              size: 64,
              color: AppColors.textDisabled,
            ),
            const SizedBox(height: AppSpacing.spaceXl),
            Text(
              '이 날 일정이 없습니다',
              style: AppTypography.displaySmall.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.spaceSm),
            Text(
              '장소를 추가하여 일정을 만들어 보세요.',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.space2xl),
            SizedBox(
              width: double.infinity,
              height: AppSpacing.buttonHeight,
              child: ElevatedButton.icon(
                onPressed: onAddTap,
                icon: const Icon(Icons.add),
                label: const Text('장소 추가'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
