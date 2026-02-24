import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../config/app_config.dart';
import '../utils/secure_storage.dart';
import 'api_exception.dart';

part 'auth_interceptor.g.dart';

/// JWT Bearer 토큰 자동 주입 인터셉터
/// - 모든 요청 헤더에 Authorization: Bearer {accessToken} 자동 첨부
/// - 401 응답 수신 시 리프레시 토큰으로 액세스 토큰 갱신 후 재요청
/// - 토큰 갱신 실패 시 UnauthorizedException 발생 (로그아웃 처리)
@riverpod
AuthInterceptor authInterceptor(Ref ref) {
  final secureStorage = ref.watch(secureStorageProvider);
  return AuthInterceptor(secureStorage: secureStorage);
}

class AuthInterceptor extends Interceptor {
  AuthInterceptor({required this.secureStorage});

  final SecureStorage secureStorage;

  /// 동시 401 처리 시 토큰 갱신을 직렬화하는 Completer
  Completer<String>? _refreshCompleter;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final accessToken = await secureStorage.getAccessToken();
    if (accessToken != null) {
      options.headers['Authorization'] = 'Bearer $accessToken';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    // 401이 아닌 경우 그대로 전파
    if (err.response?.statusCode != 401) {
      handler.next(err);
      return;
    }

    // 이미 토큰 갱신 중이면 완료를 기다린 후 새 토큰으로 재시도
    if (_refreshCompleter != null) {
      try {
        final newToken = await _refreshCompleter!.future;
        final retryOptions = err.requestOptions;
        retryOptions.headers['Authorization'] = 'Bearer $newToken';
        final retryDio = Dio(BaseOptions(
          baseUrl: retryOptions.baseUrl,
          headers: {'Content-Type': 'application/json'},
        ));
        handler.resolve(await retryDio.fetch(retryOptions));
      } catch (_) {
        handler.reject(err);
      }
      return;
    }

    _refreshCompleter = Completer<String>();

    try {
      final refreshToken = await secureStorage.getRefreshToken();
      if (refreshToken == null) {
        _refreshCompleter!.completeError('no refresh token');
        await _handleAuthFailure(handler, err);
        return;
      }

      // auth 서비스는 항상 8081 포트이므로 AppConfig.authBaseUrl 고정 사용
      final authBaseUrl = AppConfig.authBaseUrl;

      // 토큰 갱신 API 호출 (인터셉터 없는 별도 Dio 인스턴스 사용)
      final refreshDio = Dio(
        BaseOptions(
          baseUrl: authBaseUrl,
          headers: {'Content-Type': 'application/json'},
        ),
      );

      final response = await refreshDio.post(
        '/auth/token/refresh',
        data: {'refresh_token': refreshToken},
      );

      // 백엔드 응답: ApiResponse { success, data: { access_token, expires_in } }
      final data = response.data['data'] as Map<String, dynamic>?;
      final newAccessToken = data?['access_token'] as String?;

      if (newAccessToken == null) {
        _refreshCompleter!.completeError('no access token in response');
        await _handleAuthFailure(handler, err);
        return;
      }

      // 새 토큰 저장
      await secureStorage.saveAccessToken(newAccessToken);

      // 대기 중인 요청들에게 새 토큰 전달
      _refreshCompleter!.complete(newAccessToken);

      // 원래 요청 재시도 (새 토큰으로)
      final retryOptions = err.requestOptions;
      retryOptions.headers['Authorization'] = 'Bearer $newAccessToken';

      final retryDio = Dio(
        BaseOptions(
          baseUrl: retryOptions.baseUrl,
          headers: {'Content-Type': 'application/json'},
        ),
      );
      final retryResponse = await retryDio.fetch(retryOptions);
      handler.resolve(retryResponse);
    } on DioException {
      if (!_refreshCompleter!.isCompleted) {
        _refreshCompleter!.completeError('refresh failed');
      }
      await _handleAuthFailure(handler, err);
    } finally {
      _refreshCompleter = null;
    }
  }

  /// 인증 실패 처리 — 토큰 삭제 후 UnauthorizedException 발생
  Future<void> _handleAuthFailure(
    ErrorInterceptorHandler handler,
    DioException err,
  ) async {
    await secureStorage.clearTokens();
    handler.reject(
      DioException(
        requestOptions: err.requestOptions,
        error: const UnauthorizedException(),
        type: DioExceptionType.badResponse,
        response: err.response,
      ),
    );
  }
}
