import 'package:flutter/material.dart';

/// 마이페이지 메인
/// TAB 3 루트 화면
///
/// TODO: ProfileProvider 연결
/// TODO: ProfileHeader 위젯 구현 (이미지/닉네임/이메일/플랜)
/// TODO: SettingsSection, SettingsTile 위젯 구현
class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('마이페이지')),
      body: const Center(
        child: Text('마이페이지\n(TODO: 구현 예정)'),
      ),
    );
  }
}
