import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/routing/app_routes.dart';
import '../../../../shared/providers/app_user_provider.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 대안 카드 페이지 (UFR-ALTN-030, UFR-ALTN-040)
/// - 유료 티어: 카드 선택 → 일정 반영
/// - 무료 티어: Paywall 이동
/// - 기존 일정 유지: 닫기
///
/// TODO: AlternativeCardProvider 연결
/// TODO: AlternativeCard 위젯 구현
/// TODO: 구독 티어 분기 처리
class AlternativeCardPage extends ConsumerWidget {
  const AlternativeCardPage({super.key, required this.briefingId});

  final String briefingId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final userState = ref.watch(appUserProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('대안 장소')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              '대안 카드\nbriefingId: $briefingId\n(TODO: 구현 예정)',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            if (!userState.isPremium)
              OutlinedButton(
                onPressed: () {
                  context.goNamed(
                    AppRoutes.paywallName,
                    queryParameters: {'from': 'alternative'},
                  );
                },
                child: const Text('Pro로 업그레이드'),
              ),
          ],
        ),
      ),
    );
  }
}
