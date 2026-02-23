import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

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

  /// 토큰 갱신 중 중복 요청 방지 플래그
  bool _isRefreshing = false;

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

    // 토큰 갱신 중 401이 발생하면 무한 루프 방지
    if (_isRefreshing) {
      handler.reject(err);
      return;
    }

    _isRefreshing = true;

    try {
      final refreshToken = await secureStorage.getRefreshToken();
      if (refreshToken == null) {
        await _handleAuthFailure(handler, err);
        return;
      }

      // 토큰 갱신 API 호출 (인터셉터 없는 별도 Dio 인스턴스 사용)
      final refreshDio = Dio(
        BaseOptions(
          baseUrl: err.requestOptions.baseUrl,
          headers: {'Content-Type': 'application/json'},
        ),
      );

      final response = await refreshDio.post(
        '/auth/refresh',
        data: {'refreshToken': refreshToken},
      );

      final newAccessToken = response.data['accessToken'] as String?;
      final newRefreshToken = response.data['refreshToken'] as String?;

      if (newAccessToken == null) {
        await _handleAuthFailure(handler, err);
        return;
      }

      // 새 토큰 저장
      await secureStorage.saveAccessToken(newAccessToken);
      if (newRefreshToken != null) {
        await secureStorage.saveRefreshToken(newRefreshToken);
      }

      // 원래 요청 재시도 (새 토큰으로)
      final retryOptions = err.requestOptions;
      retryOptions.headers['Authorization'] = 'Bearer $newAccessToken';

      final retryResponse = await refreshDio.fetch(retryOptions);
      handler.resolve(retryResponse);
    } on DioException {
      await _handleAuthFailure(handler, err);
    } finally {
      _isRefreshing = false;
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
