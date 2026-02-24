import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/network/dio_client.dart';
import '../../domain/models/payment_model.dart';

part 'payment_datasource.g.dart';

/// Payment Service DataSource
// 변경: Prism Mock(http://localhost:4010) → PAY 서비스(http://localhost:8087/api/v1)
// dioClientProvider(ApiService.payment) 주입으로 포트 분리
// 주의: baseUrl은 /api/v1 까지만 설정. 백엔드 컨트롤러가 @RequestMapping("/api/v1/subscriptions")를
// 사용하므로 datasource의 /subscriptions/plans 경로와 조합하면 /api/v1/subscriptions/plans 가 된다.
// baseUrl을 /api/v1/subscriptions 로 설정하면 이중 경로 발생.
@riverpod
PaymentDataSource paymentDataSource(Ref ref) {
  final dio = ref.watch(dioClientProvider(ApiService.payment));
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
