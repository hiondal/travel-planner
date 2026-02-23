import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/routing/app_routes.dart';

/// 장소 교체 결과 페이지
/// - 대안 카드 선택 후 일정 교체 완료 확인
/// - 교체된 장소 정보 표시
/// - 일정표로 돌아가기 CTA
///
/// TODO: 교체 결과 데이터 표시
class ScheduleChangeResultPage extends StatelessWidget {
  const ScheduleChangeResultPage({
    super.key,
    required this.tripId,
    this.alternativeId,
  });

  final String tripId;
  final String? alternativeId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('장소 교체 완료')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              '일정이 변경되었습니다.\n(TODO: 구현 예정)',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () {
                context.goNamed(
                  AppRoutes.scheduleDetailName,
                  pathParameters: {'tripId': tripId},
                );
              },
              child: const Text('일정표로 돌아가기'),
            ),
          ],
        ),
      ),
    );
  }
}
