import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';

/// 권한 동의 화면 (SCR-012)
/// 첫 일정 등록 시 위치/알림 권한 요청
class PermissionPage extends StatelessWidget {
  const PermissionPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.spaceBase),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),

              // 앱 아이콘 영역
              Center(
                child: Container(
                  width: 96,
                  height: 96,
                  decoration: BoxDecoration(
                    color: AppColors.accentPurple.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(24),
                  ),
                  child: const Icon(
                    Icons.travel_explore,
                    size: 52,
                    color: AppColors.accentPurple,
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.spaceXl),

              Text(
                '더 나은 여행 경험을 위해\n권한이 필요해요',
                textAlign: TextAlign.center,
                style: AppTypography.displayMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: AppSpacing.spaceSm),
              Text(
                '아래 권한을 허용하면 실시간으로\n최적의 여행 경로를 안내받을 수 있어요.',
                textAlign: TextAlign.center,
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),

              const SizedBox(height: AppSpacing.space3xl),

              // 권한 항목 목록
              _PermissionItem(
                icon: Icons.location_on,
                iconColor: AppColors.accentPurple,
                title: '위치 정보',
                description: '현재 위치 기준으로 가까운 대안 장소를 추천하고\n이동 시간을 정확하게 계산해요.',
              ),
              const SizedBox(height: AppSpacing.spaceBase),
              _PermissionItem(
                icon: Icons.notifications,
                iconColor: AppColors.accentAmber,
                title: '알림',
                description: '여행지 혼잡도 급증, 날씨 변화, 영업시간 변경 등\n중요한 상황을 즉시 알려드려요.',
              ),

              const Spacer(flex: 2),

              // 허용 버튼
              SizedBox(
                height: AppSpacing.buttonHeight,
                child: ElevatedButton(
                  onPressed: () => _requestPermissions(context),
                  child: const Text('권한 허용하고 시작하기'),
                ),
              ),
              const SizedBox(height: AppSpacing.spaceMd),

              // 나중에 설정
              TextButton(
                onPressed: () => context.goNamed(AppRoutes.tripListName),
                child: Text(
                  '나중에 설정하기',
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.spaceBase),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _requestPermissions(BuildContext context) async {
    // Prism Mock 환경: 권한 요청 없이 일정 목록으로 이동
    // 실제 앱에서는 permission_handler 패키지로 위치/알림 권한 요청 후 이동
    if (!context.mounted) return;
    context.goNamed(AppRoutes.tripListName);
  }
}

class _PermissionItem extends StatelessWidget {
  const _PermissionItem({
    required this.icon,
    required this.iconColor,
    required this.title,
    required this.description,
  });

  final IconData icon;
  final Color iconColor;
  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      decoration: BoxDecoration(
        color: AppColors.bgCard,
        borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: iconColor.withOpacity(0.12),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: iconColor, size: 24),
          ),
          const SizedBox(width: AppSpacing.spaceBase),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: AppTypography.headlineMedium.copyWith(
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: AppSpacing.spaceXs),
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
      ),
    );
  }
}
