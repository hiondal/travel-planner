import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../shared/providers/app_user_provider.dart';
import '../../../auth/presentation/providers/auth_provider.dart';

/// 마이페이지 화면 (SCR-030)
/// TAB3 루트
class ProfilePage extends ConsumerWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final userState = ref.watch(appUserProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('마이페이지')),
      body: ListView(
        children: [
          // 프로필 헤더
          _ProfileHeader(
            nickname: userState.email ?? '여행자',
            tier: userState.subscriptionTier.displayName,
          ),
          const Divider(height: 1),

          // 구독 섹션
          _SettingsSection(
            title: '구독',
            children: [
              _SettingsTile(
                icon: Icons.workspace_premium,
                label: '구독 관리',
                trailing: Text(
                  userState.subscriptionTier.displayName,
                  style: AppTypography.bodySmall.copyWith(
                    color: userState.isPremium
                        ? AppColors.accentPurple
                        : AppColors.textSecondary,
                  ),
                ),
                onTap: () => context.goNamed(AppRoutes.subscriptionName),
              ),
            ],
          ),

          // 알림/위치 섹션
          _SettingsSection(
            title: '설정',
            children: [
              _SettingsTile(
                icon: Icons.notifications_outlined,
                label: '알림 설정',
                onTap: () =>
                    context.goNamed(AppRoutes.notificationSettingsName),
              ),
              _SettingsTile(
                icon: Icons.location_on_outlined,
                label: '위치 정보 동의 관리',
                onTap: () => context.goNamed(AppRoutes.locationConsentName),
              ),
            ],
          ),

          // 계정 섹션
          _SettingsSection(
            title: '계정',
            children: [
              _SettingsTile(
                icon: Icons.logout,
                label: '로그아웃',
                textColor: AppColors.statusRed,
                onTap: () => _handleLogout(context, ref),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.space2xl),
        ],
      ),
    );
  }

  Future<void> _handleLogout(BuildContext context, WidgetRef ref) async {
    await ref.read(logoutNotifierProvider.notifier).logout();
    if (context.mounted) {
      context.goNamed(AppRoutes.loginName);
    }
  }
}

class _ProfileHeader extends StatelessWidget {
  const _ProfileHeader({required this.nickname, required this.tier});

  final String nickname;
  final String tier;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(AppSpacing.spaceXl),
      child: Row(
        children: [
          CircleAvatar(
            radius: 32,
            backgroundColor: AppColors.bgInput,
            child: Text(
              nickname.isNotEmpty ? nickname[0].toUpperCase() : 'U',
              style: AppTypography.displaySmall.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
          ),
          const SizedBox(width: AppSpacing.spaceBase),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  nickname,
                  style: AppTypography.displaySmall.copyWith(
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: 4),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.spaceSm,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.accentPurple.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    tier,
                    style: AppTypography.labelSmall.copyWith(
                      color: AppColors.accentPurple,
                    ),
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

class _SettingsSection extends StatelessWidget {
  const _SettingsSection({
    required this.title,
    required this.children,
  });

  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(
            AppSpacing.spaceBase,
            AppSpacing.spaceXl,
            AppSpacing.spaceBase,
            AppSpacing.spaceSm,
          ),
          child: Text(
            title,
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ),
        ...children,
      ],
    );
  }
}

class _SettingsTile extends StatelessWidget {
  const _SettingsTile({
    required this.icon,
    required this.label,
    required this.onTap,
    this.trailing,
    this.textColor,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final Widget? trailing;
  final Color? textColor;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: textColor ?? AppColors.textPrimary, size: 24),
      title: Text(
        label,
        style: AppTypography.bodyLarge.copyWith(
          color: textColor ?? AppColors.textPrimary,
        ),
      ),
      trailing: trailing ??
          const Icon(
            Icons.chevron_right,
            color: AppColors.textSecondary,
          ),
      onTap: onTap,
    );
  }
}
