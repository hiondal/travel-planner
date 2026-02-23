import 'package:flutter/material.dart';

/// 권한 동의 페이지 (첫 일정 등록 시 표시)
/// - 위치 권한
/// - Push 알림 권한
///
/// TODO: 실제 권한 요청 로직 구현 (permission_handler 패키지)
class PermissionPage extends StatelessWidget {
  const PermissionPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('권한 설정')),
      body: const Center(
        child: Text('위치 및 알림 권한 동의\n(TODO: 구현 예정)'),
      ),
    );
  }
}
