import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/routing/app_routes.dart';

/// Paywall 페이지
/// - Trip Pass / Pro 플랜 선택
/// - 결제 화면(/payment/checkout)으로 이동
///
/// TODO: PaywallPlanCard 위젯 구현
/// TODO: 플랜별 혜택 목록 표시
/// TODO: 인앱 결제 연동
class PaywallPage extends StatelessWidget {
  const PaywallPage({super.key, this.from});

  /// 진입 경로 추적 (analytics용)
  final String? from;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('프리미엄 플랜')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Text('Paywall\nfrom: $from\n(TODO: 구현 예정)'),
            const Spacer(),
            ElevatedButton(
              onPressed: () {
                context.goNamed(
                  AppRoutes.paymentCheckoutName,
                  queryParameters: {'plan': 'trip_pass'},
                );
              },
              child: const Text('Trip Pass 구매'),
            ),
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: () {
                context.goNamed(
                  AppRoutes.paymentCheckoutName,
                  queryParameters: {'plan': 'pro'},
                );
              },
              child: const Text('Pro 구독'),
            ),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}
