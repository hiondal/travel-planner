import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/routing/app_routes.dart';

/// 여행 목록 페이지 (메인)
/// TAB 1 루트 화면
///
/// TODO: TripListProvider 연결
/// TODO: TripCard 위젯 구현
/// TODO: 빈 상태 처리 (EmptyStateView)
/// TODO: 스켈레톤 로딩 (SkeletonLoader)
class TripListPage extends StatelessWidget {
  const TripListPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('내 여행'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => context.goNamed(AppRoutes.tripCreateName),
            tooltip: '새 여행 만들기',
          ),
        ],
      ),
      body: const Center(
        child: Text('여행 목록\n(TODO: 구현 예정)'),
      ),
    );
  }
}
