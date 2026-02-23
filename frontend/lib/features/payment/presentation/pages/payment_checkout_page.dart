import 'package:flutter/material.dart';

/// 결제 화면 (IAP 연동)
///
/// TODO: in_app_purchase 연동
/// TODO: PaymentProvider 연결
/// TODO: 결제 성공 → PaymentSuccessPage 이동
/// TODO: 결제 실패 인라인 처리
class PaymentCheckoutPage extends StatelessWidget {
  const PaymentCheckoutPage({super.key, this.plan});

  /// 결제 플랜 ('trip_pass' | 'pro')
  final String? plan;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('결제')),
      body: Center(
        child: Text('결제 화면\nplan: $plan\n(TODO: 구현 예정)'),
      ),
    );
  }
}
