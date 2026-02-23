import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../core/network/dio_client.dart';
import '../../domain/models/payment_model.dart';

part 'payment_datasource.g.dart';

/// Payment Service DataSource
/// Prism Mock: http://localhost:4016
@riverpod
PaymentDataSource paymentDataSource(Ref ref) {
  final dio = ref.watch(dioClientProvider);
  return PaymentDataSource(dio: dio);
}

class PaymentDataSource {
  PaymentDataSource({required this.dio});

  final Dio dio;

  /// GET /subscriptions/plans
  /// 구독 플랜 목록 조회
  Future<SubscriptionPlanListResponse> getPlans() async {
    final response = await dio.get<Map<String, dynamic>>(
      '/subscriptions/plans',
    );
    return SubscriptionPlanListResponse.fromJson(response.data!);
  }

  /// GET /subscriptions/status
  /// 현재 구독 상태 조회
  Future<SubscriptionStatus> getSubscriptionStatus() async {
    final response = await dio.get<Map<String, dynamic>>(
      '/subscriptions/status',
    );
    return SubscriptionStatus.fromJson(response.data!);
  }

  /// POST /subscriptions/purchase
  /// 구독 구매 (IAP 영수증 검증)
  Future<SubscriptionStatus> purchase(PurchaseRequest request) async {
    final response = await dio.post<Map<String, dynamic>>(
      '/subscriptions/purchase',
      data: request.toJson(),
    );
    return SubscriptionStatus.fromJson(response.data!);
  }
}
