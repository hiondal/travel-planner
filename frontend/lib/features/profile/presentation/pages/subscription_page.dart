import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/routing/app_routes.dart';

/// 구독 관리 페이지
///
/// TODO: SubscriptionProvider 연결
/// TODO: 현재 구독 정보 표시
class SubscriptionPage extends StatelessWidget {
  const SubscriptionPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('구독 관리')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text('구독 관리\n(TODO: 구현 예정)'),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () => context.goNamed(AppRoutes.paymentCheckoutName),
              child: const Text('플랜 변경'),
            ),
          ],
        ),
      ),
    );
  }
}
