import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../shared/widgets/app_bottom_sheet.dart';
import '../../../../shared/widgets/app_snack_bar.dart';
import '../providers/trip_provider.dart';

/// 여행 생성 화면 (SCR-011)
/// UFR-SCHD-010: 여행명, 기간, 도시 입력
class TripCreatePage extends ConsumerStatefulWidget {
  const TripCreatePage({super.key});

  @override
  ConsumerState<TripCreatePage> createState() => _TripCreatePageState();
}

class _TripCreatePageState extends ConsumerState<TripCreatePage> {
  final _formKey = GlobalKey<FormState>();
  final _tripNameController = TextEditingController();

  String? _selectedCity;
  DateTime? _startDate;
  DateTime? _endDate;

  static const List<String> _supportedCities = [
    '도쿄',
    '오사카',
    '방콕',
    '싱가포르',
    '파리',
  ];

  @override
  void dispose() {
    _tripNameController.dispose();
    super.dispose();
  }

  Future<void> _pickDateRange() async {
    final now = DateTime.now();
    final picked = await showDateRangePicker(
      context: context,
      firstDate: now,
      lastDate: now.add(const Duration(days: 365)),
      initialDateRange: _startDate != null && _endDate != null
          ? DateTimeRange(start: _startDate!, end: _endDate!)
          : null,
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: Theme.of(context).colorScheme.copyWith(
                  primary: AppColors.accentRed,
                ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) {
      setState(() {
        _startDate = picked.start;
        _endDate = picked.end;
      });
    }
  }

  Future<void> _selectCity() async {
    await AppBottomSheet.show(
      context,
      child: _CitySelectSheet(
        cities: _supportedCities,
        selectedCity: _selectedCity,
        onSelect: (city) {
          setState(() => _selectedCity = city);
          Navigator.of(context).pop();
        },
      ),
    );
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_selectedCity == null) {
      AppSnackBar.show(context, '도시를 선택해주세요.');
      return;
    }
    if (_startDate == null || _endDate == null) {
      AppSnackBar.show(context, '여행 기간을 선택해주세요.');
      return;
    }

    final notifier = ref.read(tripCreateNotifierProvider.notifier);
    final trip = await notifier.createTrip(
      tripName: _tripNameController.text.trim(),
      city: _selectedCity!,
      startDate: _startDate!,
      endDate: _endDate!,
    );

    if (!mounted) return;
    if (trip != null) {
      context.goNamed(
        AppRoutes.permissionName,
        pathParameters: {},
      );
    } else {
      final state = ref.read(tripCreateNotifierProvider);
      state.whenOrNull(
        error: (err, _) {
          AppSnackBar.showError(context, '여행 생성에 실패했습니다.');
        },
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final createState = ref.watch(tripCreateNotifierProvider);
    final isLoading = createState.isLoading;
    final dateFormat = DateFormat('yyyy.MM.dd', 'ko');

    return Scaffold(
      appBar: AppBar(title: const Text('새 여행 만들기')),
      body: Form(
        key: _formKey,
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.spaceBase),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 여행 이름
              Text(
                '여행 이름',
                style: AppTypography.headlineMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: AppSpacing.spaceSm),
              TextFormField(
                controller: _tripNameController,
                decoration: const InputDecoration(
                  hintText: '예: 도쿄 봄 여행',
                ),
                validator: (value) {
                  if (value == null || value.trim().isEmpty) {
                    return '여행 이름을 입력해주세요.';
                  }
                  if (value.trim().length > 30) {
                    return '30자 이내로 입력해주세요.';
                  }
                  return null;
                },
              ),
              const SizedBox(height: AppSpacing.spaceXl),

              // 도시 선택
              Text(
                '여행 도시',
                style: AppTypography.headlineMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: AppSpacing.spaceSm),
              InkWell(
                onTap: _selectCity,
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
                        _selectedCity ?? 'MVP 지원 도시 선택',
                        style: AppTypography.bodyMedium.copyWith(
                          color: _selectedCity != null
                              ? AppColors.textPrimary
                              : AppColors.textSecondary,
                        ),
                      ),
                      const Icon(
                        Icons.keyboard_arrow_down,
                        color: AppColors.textSecondary,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.spaceXl),

              // 여행 기간
              Text(
                '여행 기간',
                style: AppTypography.headlineMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: AppSpacing.spaceSm),
              InkWell(
                onTap: _pickDateRange,
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
                        _startDate != null && _endDate != null
                            ? '${dateFormat.format(_startDate!)} ~ ${dateFormat.format(_endDate!)}'
                            : '출발일 - 귀국일 선택',
                        style: AppTypography.bodyMedium.copyWith(
                          color: _startDate != null
                              ? AppColors.textPrimary
                              : AppColors.textSecondary,
                        ),
                      ),
                      const Icon(
                        Icons.calendar_today_outlined,
                        color: AppColors.textSecondary,
                        size: 20,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.space3xl),

              // 생성 버튼
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
                      : const Text('여행 만들기'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 도시 선택 바텀시트 (BS-004)
class _CitySelectSheet extends StatelessWidget {
  const _CitySelectSheet({
    required this.cities,
    required this.selectedCity,
    required this.onSelect,
  });

  final List<String> cities;
  final String? selectedCity;
  final ValueChanged<String> onSelect;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(
            AppSpacing.spaceBase,
            AppSpacing.spaceSm,
            AppSpacing.spaceBase,
            AppSpacing.spaceBase,
          ),
          child: Text(
            'MVP 지원 도시 선택',
            style: AppTypography.displaySmall.copyWith(
              color: AppColors.textPrimary,
            ),
          ),
        ),
        const Divider(height: 1),
        ...cities.map(
          (city) => ListTile(
            title: Text(
              city,
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            trailing: selectedCity == city
                ? const Icon(Icons.check, color: AppColors.accentRed)
                : null,
            onTap: () => onSelect(city),
          ),
        ),
        SizedBox(height: MediaQuery.of(context).padding.bottom + 8),
      ],
    );
  }
}
