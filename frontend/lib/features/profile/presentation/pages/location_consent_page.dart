import 'package:flutter/material.dart';

/// 위치정보 동의 관리 페이지
///
/// TODO: 위치 권한 현재 상태 조회
/// TODO: 권한 변경 → OS 설정 앱 연동
class LocationConsentPage extends StatelessWidget {
  const LocationConsentPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('위치정보 동의')),
      body: const Center(
        child: Text('위치정보 동의 관리\n(TODO: 구현 예정)'),
      ),
    );
  }
}
