import 'package:flutter/material.dart';

/// 브리핑 목록 페이지
/// TAB 2 루트 화면
///
/// TODO: BriefingListProvider 연결
/// TODO: BriefingListItem 위젯 구현
/// TODO: 스켈레톤 로딩, 빈 상태 처리
class BriefingListPage extends StatelessWidget {
  const BriefingListPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('브리핑')),
      body: const Center(
        child: Text('브리핑 목록\n(TODO: 구현 예정)'),
      ),
    );
  }
}
