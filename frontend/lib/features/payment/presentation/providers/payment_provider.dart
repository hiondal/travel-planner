import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../data/repositories/payment_repository.dart';
import '../../domain/models/payment_model.dart';

part 'payment_provider.g.dart';

/// 구독 플랜 목록 Provider
@riverpod
Future<List<SubscriptionPlan>> subscriptionPlans(Ref ref) async {
  return ref.watch(paymentRepositoryProvider).getPlans();
}

/// 구독 상태 Provider
@riverpod
Future<SubscriptionStatus> subscriptionStatus(Ref ref) async {
  return ref.watch(paymentRepositoryProvider).getSubscriptionStatus();
}

/// 구독 구매 Notifier
@riverpod
class PurchaseNotifier extends _$PurchaseNotifier {
  @override
  AsyncValue<SubscriptionStatus?> build() => const AsyncValue.data(null);

  Future<SubscriptionStatus?> purchase({
    required String planId,
    required String receiptData,
    required String platform,
  }) async {
    state = const AsyncValue.loading();
    final result = await AsyncValue.guard(() async {
      final status = await ref.read(paymentRepositoryProvider).purchase(
            planId: planId,
            receiptData: receiptData,
            platform: platform,
          );
      ref.invalidate(subscriptionStatusProvider);
      return status;
    });
    state = result;
    return result.valueOrNull;
  }
}
