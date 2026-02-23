import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/routing/app_routes.dart';

/// 결제 완료 페이지
///
/// TODO: 결제 완료 후 구독 티어 갱신 (AppUserProvider)
/// TODO: 이전 화면 복귀 또는 메인 이동
class PaymentSuccessPage extends StatelessWidget {
  const PaymentSuccessPage({super.key, this.plan});

  final String? plan;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.check_circle, size: 64),
            const SizedBox(height: 16),
            Text('결제가 완료되었습니다!\nplan: $plan'),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () => context.goNamed(AppRoutes.tripListName),
              child: const Text('여행 시작하기'),
            ),
          ],
        ),
      ),
    );
  }
}
