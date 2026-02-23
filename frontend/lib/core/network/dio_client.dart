import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../config/app_config.dart';
import 'auth_interceptor.dart';
import 'error_interceptor.dart';

part 'dio_client.g.dart';

/// Dio HTTP 클라이언트 팩토리
/// JWT 인터셉터, 에러 핸들링 인터셉터가 자동으로 등록된다.
@riverpod
Dio dioClient(Ref ref) {
  final dio = Dio(
    BaseOptions(
      baseUrl: AppConfig.apiBaseUrl,
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
