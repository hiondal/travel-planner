import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../shared/widgets/app_snack_bar.dart';

/// 알림 설정 화면 (SCR-032)
/// UFR-NOTF: 알림 유형별 On/Off 관리
class NotificationSettingsPage extends StatefulWidget {
  const NotificationSettingsPage({super.key});

  @override
  State<NotificationSettingsPage> createState() =>
      _NotificationSettingsPageState();
}

class _NotificationSettingsPageState extends State<NotificationSettingsPage> {
  // 알림 설정 상태 (로컬 상태 — 실제에서는 서버 or SharedPreferences 연동)
  bool _pushEnabled = true;
  bool _statusChangeEnabled = true;
  bool _briefingReadyEnabled = true;
  bool _alternativeSuggestEnabled = true;
  bool _marketingEnabled = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('알림 설정')),
      body: ListView(
        children: [
          // 전체 Push 알림 마스터 토글
          _NotificationSection(
            title: '기기 알림',
            children: [
              _NotificationTile(
                icon: Icons.notifications,
                iconColor: AppColors.accentPurple,
                title: 'Push 알림',
                subtitle: '기기 알림을 켜야 앱 알림을 받을 수 있어요.',
                value: _pushEnabled,
                onChanged: (val) {
                  setState(() => _pushEnabled = val);
                  _saveSettings();
                },
              ),
            ],
          ),

          // 여행 알림 섹션
          _NotificationSection(
            title: '여행 알림',
            children: [
              _NotificationTile(
                icon: Icons.warning_amber_rounded,
                iconColor: AppColors.accentAmber,
                title: '상태 변화 알림',
                subtitle: '날씨, 혼잡도, 영업시간 변화가 감지될 때 알려드려요.',
                value: _pushEnabled && _statusChangeEnabled,
                enabled: _pushEnabled,
                onChanged: (val) {
                  setState(() => _statusChangeEnabled = val);
                  _saveSettings();
                },
              ),
              _NotificationTile(
                icon: Icons.article_outlined,
                iconColor: AppColors.accentPurple,
                title: '브리핑 준비 완료',
                subtitle: '오늘의 여행 브리핑이 생성되면 알려드려요.',
                value: _pushEnabled && _briefingReadyEnabled,
                enabled: _pushEnabled,
                onChanged: (val) {
                  setState(() => _briefingReadyEnabled = val);
                  _saveSettings();
                },
              ),
              _NotificationTile(
                icon: Icons.place_outlined,
                iconColor: AppColors.statusGreen,
                title: '대안 장소 추천',
                subtitle: '방문 예정 장소의 대안이 준비되면 알려드려요.',
                value: _pushEnabled && _alternativeSuggestEnabled,
                enabled: _pushEnabled,
                onChanged: (val) {
                  setState(() => _alternativeSuggestEnabled = val);
                  _saveSettings();
                },
              ),
            ],
          ),

          // 마케팅 알림 섹션
          _NotificationSection(
            title: '혜택/마케팅',
            children: [
              _NotificationTile(
                icon: Icons.local_offer_outlined,
                iconColor: AppColors.textSecondary,
                title: '혜택 및 이벤트',
                subtitle: '프리미엄 할인, 신규 기능 등 혜택 정보를 받아보세요.',
                value: _pushEnabled && _marketingEnabled,
                enabled: _pushEnabled,
                onChanged: (val) {
                  setState(() => _marketingEnabled = val);
                  _saveSettings();
                },
              ),
            ],
          ),

          const SizedBox(height: AppSpacing.space2xl),

          // 안내 문구
          Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.spaceBase,
            ),
            child: Text(
              '일부 알림은 기기의 시스템 설정에서도 조정할 수 있어요.\n알림을 끄면 중요한 여행 정보를 놓칠 수 있습니다.',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textDisabled,
              ),
              textAlign: TextAlign.center,
            ),
          ),
          const SizedBox(height: AppSpacing.spaceBase),
        ],
      ),
    );
  }

  void _saveSettings() {
    // Prism Mock 환경: 실제 API 저장 없이 로컬 상태만 업데이트
    // 실제 앱에서는 PATCH /users/me/notification-settings 호출
    AppSnackBar.show(context, '알림 설정이 저장되었습니다.');
  }
}

class _NotificationSection extends StatelessWidget {
  const _NotificationSection({
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

class _NotificationTile extends StatelessWidget {
  const _NotificationTile({
    required this.icon,
    required this.iconColor,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onChanged,
    this.enabled = true,
  });

  final IconData icon;
  final Color iconColor;
  final String title;
  final String subtitle;
  final bool value;
  final ValueChanged<bool> onChanged;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(
        icon,
        color: enabled ? iconColor : AppColors.textDisabled,
        size: 24,
      ),
      title: Text(
        title,
        style: AppTypography.bodyLarge.copyWith(
          color: enabled ? AppColors.textPrimary : AppColors.textDisabled,
        ),
      ),
      subtitle: Text(
        subtitle,
        style: AppTypography.bodySmall.copyWith(
          color: AppColors.textDisabled,
        ),
      ),
      trailing: Switch(
        value: value,
        onChanged: enabled ? onChanged : null,
        activeColor: AppColors.accentPurple,
        inactiveTrackColor: AppColors.bgInput,
      ),
    );
  }
}
