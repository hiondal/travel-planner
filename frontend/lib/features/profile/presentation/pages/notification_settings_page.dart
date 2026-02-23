import 'package:flutter/material.dart';

/// 알림 설정 페이지
///
/// TODO: NotificationSettingsProvider 연결
/// TODO: 알림 유형별 토글 UI
class NotificationSettingsPage extends StatelessWidget {
  const NotificationSettingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('알림 설정')),
      body: const Center(
        child: Text('알림 설정\n(TODO: 구현 예정)'),
      ),
    );
  }
}
