import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../domain/models/payment_model.dart';
import '../datasources/payment_datasource.dart';

part 'payment_repository.g.dart';

@riverpod
PaymentRepository paymentRepository(Ref ref) {
  return PaymentRepository(
    dataSource: ref.watch(paymentDataSourceProvider),
  );
}

class PaymentRepository {
  PaymentRepository({required this.dataSource});

  final PaymentDataSource dataSource;

  Future<List<SubscriptionPlan>> getPlans() async {
    final response = await dataSource.getPlans();
    return response.plans;
  }

  Future<SubscriptionStatus> getSubscriptionStatus() async {
    return dataSource.getSubscriptionStatus();
  }

  Future<SubscriptionStatus> purchase({
    required String planId,
    required String receiptData,
    required String platform,
  }) async {
    return dataSource.purchase(
      PurchaseRequest(
        planId: planId,
        receiptData: receiptData,
        platform: platform,
      ),
    );
  }
}
