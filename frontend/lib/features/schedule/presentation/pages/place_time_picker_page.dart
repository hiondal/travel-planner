import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../shared/widgets/app_snack_bar.dart';
import '../providers/trip_provider.dart';

/// 장소 시간 지정 화면 (SCR-015)
/// UFR-SCHD-030: 날짜/시간 선택 후 일정 추가
class PlaceTimePickerPage extends ConsumerStatefulWidget {
  const PlaceTimePickerPage({
    super.key,
    required this.tripId,
    this.placeId,
    this.startDate,
    this.endDate,
  });

  final String tripId;
  final String? placeId;
  final String? startDate;
  final String? endDate;

  @override
  ConsumerState<PlaceTimePickerPage> createState() =>
      _PlaceTimePickerPageState();
}

class _PlaceTimePickerPageState extends ConsumerState<PlaceTimePickerPage> {
  late DateTime _selectedDate;
  TimeOfDay _selectedTime = const TimeOfDay(hour: 10, minute: 0);
  int _durationMinutes = 60;

  @override
  void initState() {
    super.initState();
    if (widget.startDate != null) {
      _selectedDate = DateTime.parse(widget.startDate!);
    } else {
      _selectedDate = DateTime.now();
    }
  }

  static const List<int> _durationOptions = [30, 60, 90, 120, 180, 240];

  Future<void> _pickDate() async {
    final firstDate = widget.startDate != null
        ? DateTime.parse(widget.startDate!)
        : DateTime.now();
    final lastDate = widget.endDate != null
        ? DateTime.parse(widget.endDate!)
        : DateTime.now().add(const Duration(days: 365));
    final picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: firstDate,
      lastDate: lastDate,
      builder: (context, child) => Theme(
        data: Theme.of(context).copyWith(
          colorScheme: Theme.of(context).colorScheme.copyWith(
                primary: AppColors.accentRed,
              ),
        ),
        child: child!,
      ),
    );
    if (picked != null) setState(() => _selectedDate = picked);
  }

  Future<void> _pickTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: _selectedTime,
      builder: (context, child) => Theme(
        data: Theme.of(context).copyWith(
          colorScheme: Theme.of(context).colorScheme.copyWith(
                primary: AppColors.accentRed,
              ),
        ),
        child: child!,
      ),
    );
    if (picked != null) setState(() => _selectedTime = picked);
  }

  Future<void> _submit() async {
    if (widget.placeId == null) {
      AppSnackBar.showError(context, '장소 정보가 없습니다.');
      return;
    }

    final scheduledAt = DateTime(
      _selectedDate.year,
      _selectedDate.month,
      _selectedDate.day,
      _selectedTime.hour,
      _selectedTime.minute,
    );

    final notifier = ref.read(scheduleItemAddNotifierProvider.notifier);
    final item = await notifier.addItem(
      tripId: widget.tripId,
      placeId: widget.placeId!,
      scheduledAt: scheduledAt,
      durationMinutes: _durationMinutes,
    );

    if (!mounted) return;
    if (item != null) {
      AppSnackBar.show(context, '일정에 추가되었습니다.');
      final params = [
        if (widget.startDate != null) 'startDate=${widget.startDate}',
        if (widget.endDate != null) 'endDate=${widget.endDate}',
      ];
      final query = params.isEmpty ? '' : '?${params.join('&')}';
      context.go('${AppRoutes.scheduleDetail(widget.tripId)}$query');
    } else {
      AppSnackBar.showError(context, '장소 추가에 실패했습니다.');
    }
  }

  @override
  Widget build(BuildContext context) {
    final addState = ref.watch(scheduleItemAddNotifierProvider);
    final isLoading = addState.isLoading;
    final dateFormat = DateFormat('yyyy년 MM월 dd일 (E)', 'ko');

    return Scaffold(
      appBar: AppBar(title: const Text('방문 시간 지정')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.spaceBase),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 날짜 선택
            Text(
              '방문 날짜',
              style: AppTypography.headlineMedium.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.spaceSm),
            InkWell(
              onTap: _pickDate,
              borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.spaceBase,
                  vertical: 14,
                ),
                decoration: BoxDecoration(
                  color: AppColors.bgInput,
                  borderRadius:
                      BorderRadius.circular(AppSpacing.radiusButton),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      dateFormat.format(_selectedDate),
                      style: AppTypography.bodyMedium.copyWith(
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const Icon(
                      Icons.calendar_today_outlined,
                      size: 20,
                      color: AppColors.textSecondary,
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.spaceXl),

            // 시간 선택
            Text(
              '방문 시간',
              style: AppTypography.headlineMedium.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.spaceSm),
            InkWell(
              onTap: _pickTime,
              borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.spaceBase,
                  vertical: 14,
                ),
                decoration: BoxDecoration(
                  color: AppColors.bgInput,
                  borderRadius:
                      BorderRadius.circular(AppSpacing.radiusButton),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      _selectedTime.format(context),
                      style: AppTypography.bodyMedium.copyWith(
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const Icon(
                      Icons.access_time,
                      size: 20,
                      color: AppColors.textSecondary,
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.spaceXl),

            // 체류 시간
            Text(
              '예상 체류 시간',
              style: AppTypography.headlineMedium.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.spaceSm),
            Wrap(
              spacing: AppSpacing.spaceSm,
              runSpacing: AppSpacing.spaceSm,
              children: _durationOptions.map((minutes) {
                final isSelected = _durationMinutes == minutes;
                final label = minutes < 60
                    ? '$minutes분'
                    : '${minutes ~/ 60}시간${minutes % 60 > 0 ? ' ${minutes % 60}분' : ''}';
                return InkWell(
                  onTap: () => setState(() => _durationMinutes = minutes),
                  borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    padding: const EdgeInsets.symmetric(
                      horizontal: AppSpacing.spaceBase,
                      vertical: AppSpacing.spaceSm,
                    ),
                    decoration: BoxDecoration(
                      color: isSelected
                          ? AppColors.accentRed.withOpacity(0.15)
                          : AppColors.bgInput,
                      border: Border.all(
                        color: isSelected
                            ? AppColors.accentRed
                            : Colors.transparent,
                        width: 1.5,
                      ),
                      borderRadius:
                          BorderRadius.circular(AppSpacing.radiusButton),
                    ),
                    child: Text(
                      label,
                      style: AppTypography.labelMedium.copyWith(
                        color: isSelected
                            ? AppColors.accentRed
                            : AppColors.textPrimary,
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: AppSpacing.space3xl),

            // 추가 버튼
            SizedBox(
              width: double.infinity,
              height: AppSpacing.buttonHeight,
              child: ElevatedButton(
                onPressed: isLoading ? null : _submit,
                child: isLoading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: AppColors.textPrimary,
                        ),
                      )
                    : const Text('일정에 추가'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
