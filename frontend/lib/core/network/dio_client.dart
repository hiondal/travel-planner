import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../config/app_config.dart';
import 'auth_interceptor.dart';
import 'error_interceptor.dart';

part 'dio_client.g.dart';

// 변경: 단일 dioClient → 서비스별 Dio provider family 패턴으로 전환
// 각 마이크로서비스가 다른 포트(8081~8087)를 사용하므로 baseUrl을 서비스별로 분리

/// 서비스별 Dio 인스턴스를 생성하는 provider family
/// 사용 예: ref.watch(dioClientProvider(ApiService.auth))
@riverpod
Dio dioClient(Ref ref, ApiService service) {
  final baseUrl = AppConfig.serviceUrl(service);

  final dio = Dio(
    BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(milliseconds: AppConfig.connectTimeout),
      receiveTimeout: const Duration(milliseconds: AppConfig.receiveTimeout),
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
    ),
  );

  // JWT Bearer 토큰 자동 주입 및 401 토큰 갱신 인터셉터
  dio.interceptors.add(ref.watch(authInterceptorProvider));

  // HTTP 에러 -> AppException 변환 인터셉터
  dio.interceptors.add(ErrorInterceptor());

  // 개발 환경에서만 로그 인터셉터 활성화
  if (AppConfig.isDev) {
    dio.interceptors.add(
      LogInterceptor(
        requestBody: true,
        responseBody: true,
        requestHeader: true,
        responseHeader: false,
        error: true,
      ),
    );
  }

  return dio;
}
