import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../shared/widgets/app_snack_bar.dart';

/// 위치정보 동의 관리 화면 (SCR-033)
/// UFR-LOC: 위치 권한 상태 확인 및 OS 설정 연동 안내
class LocationConsentPage extends StatefulWidget {
  const LocationConsentPage({super.key});

  @override
  State<LocationConsentPage> createState() => _LocationConsentPageState();
}

class _LocationConsentPageState extends State<LocationConsentPage> {
  // Prism Mock 환경에서는 권한이 허용된 상태로 시작
  bool _locationGranted = true;
  bool _preciseLocation = true;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('위치정보 동의 관리')),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.spaceBase),
        children: [
          // 현재 위치 권한 상태 카드
          _LocationStatusCard(isGranted: _locationGranted),
          const SizedBox(height: AppSpacing.spaceXl),

          // 위치 정보 수집 목적
          Text(
            '위치 정보 수집 목적',
            style: AppTypography.displaySmall.copyWith(
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: AppSpacing.spaceMd),

          ..._purposes.map(
            (purpose) => Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.spaceSm),
              child: _PurposeItem(
                icon: purpose['icon'] as IconData,
                title: purpose['title'] as String,
                description: purpose['description'] as String,
              ),
            ),
          ),

          const SizedBox(height: AppSpacing.spaceXl),

          // 정밀 위치 토글
          Container(
            padding: const EdgeInsets.all(AppSpacing.spaceBase),
            decoration: BoxDecoration(
              color: AppColors.bgCard,
              borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '위치 정확도 설정',
                  style: AppTypography.headlineMedium.copyWith(
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: AppSpacing.spaceXs),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '정밀 위치',
                            style: AppTypography.bodyMedium.copyWith(
                              color: _locationGranted
                                  ? AppColors.textPrimary
                                  : AppColors.textDisabled,
                            ),
                          ),
                          Text(
                            '대안 장소 거리 계산 정확도를 높여줘요.',
                            style: AppTypography.bodySmall.copyWith(
                              color: AppColors.textDisabled,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Switch(
                      value: _locationGranted && _preciseLocation,
                      onChanged: _locationGranted
                          ? (val) {
                              setState(() => _preciseLocation = val);
                              AppSnackBar.show(context, '위치 설정이 변경되었습니다.');
                            }
                          : null,
                      activeColor: AppColors.accentPurple,
                      inactiveTrackColor: AppColors.bgInput,
                    ),
                  ],
                ),
              ],
            ),
          ),

          const SizedBox(height: AppSpacing.spaceXl),

          // OS 설정 연동 버튼
          SizedBox(
            height: AppSpacing.buttonHeight,
            child: OutlinedButton.icon(
              onPressed: () => _openSystemSettings(),
              icon: const Icon(Icons.settings_outlined, size: 18),
              label: const Text('시스템 위치 설정 열기'),
            ),
          ),

          const SizedBox(height: AppSpacing.space2xl),

          // 개인정보 처리 방침
          Center(
            child: TextButton(
              onPressed: () => _showPrivacyPolicy(context),
              child: Text(
                '위치정보 처리방침 보기',
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                  decoration: TextDecoration.underline,
                ),
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.spaceBase),
        ],
      ),
    );
  }

  void _openSystemSettings() {
    // 실제 앱에서는 app_settings 패키지로 시스템 설정 열기
    // Prism Mock에서는 안내 메시지만 표시
    AppSnackBar.show(context, '시스템 설정에서 위치 권한을 변경할 수 있어요.');
  }

  void _showPrivacyPolicy(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.bgCard,
        title: Text(
          '위치정보 처리방침',
          style: AppTypography.headlineMedium.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        content: Text(
          '수집 항목: 위치 정보 (위도, 경도)\n'
          '수집 목적: 대안 장소 거리 계산, 이동 경로 최적화\n'
          '보유 기간: 서비스 이용 기간 동안 보관\n\n'
          '위치 정보는 제3자에게 제공되지 않습니다.',
          style: AppTypography.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }

  static const List<Map<String, Object>> _purposes = [
    {
      'icon': Icons.place_outlined,
      'title': '대안 장소 추천',
      'description': '현재 위치에서 가장 가까운 대안 장소 3곳을 추천해요.',
    },
    {
      'icon': Icons.directions_outlined,
      'title': '이동 시간 계산',
      'description': '현재 위치에서 대안 장소까지 예상 이동 시간을 표시해요.',
    },
    {
      'icon': Icons.analytics_outlined,
      'title': '혼잡도 모니터링',
      'description': '방문 예정 장소 주변 실시간 혼잡도를 분석해요.',
    },
  ];
}

class _LocationStatusCard extends StatelessWidget {
  const _LocationStatusCard({required this.isGranted});

  final bool isGranted;

  @override
  Widget build(BuildContext context) {
    final color = isGranted ? AppColors.statusGreen : AppColors.statusRed;
    final bgColor = color.withOpacity(0.1);
    final label = isGranted ? '위치 권한 허용됨' : '위치 권한 거부됨';
    final sublabel = isGranted
        ? '앱이 백그라운드에서도 위치 정보를 사용할 수 있어요.'
        : '위치 권한이 필요한 기능을 사용하려면 권한을 허용해 주세요.';

    return Container(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(
        children: [
          Icon(
            isGranted ? Icons.check_circle : Icons.cancel,
            color: color,
            size: 32,
          ),
          const SizedBox(width: AppSpacing.spaceBase),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: AppTypography.headlineMedium.copyWith(
                    color: color,
                  ),
                ),
                const SizedBox(height: AppSpacing.spaceXs),
                Text(
                  sublabel,
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
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

class _PurposeItem extends StatelessWidget {
  const _PurposeItem({
    required this.icon,
    required this.title,
    required this.description,
  });

  final IconData icon;
  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 36,
          height: 36,
          decoration: BoxDecoration(
            color: AppColors.accentPurple.withOpacity(0.12),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Icon(icon, color: AppColors.accentPurple, size: 18),
        ),
        const SizedBox(width: AppSpacing.spaceBase),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
              Text(
                description,
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
