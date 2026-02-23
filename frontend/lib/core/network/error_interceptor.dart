import 'package:dio/dio.dart';

import 'api_exception.dart';

/// HTTP 에러를 AppException으로 변환하는 인터셉터
/// 모든 네트워크 오류를 통일된 ApiException 타입으로 래핑한다.
class ErrorInterceptor extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final exception = _mapToApiException(err);

    handler.reject(
      DioException(
        requestOptions: err.requestOptions,
        error: exception,
        type: err.type,
        response: err.response,
        message: exception.message,
      ),
    );
  }

  ApiException _mapToApiException(DioException err) {
    // 이미 ApiException으로 변환된 경우 (AuthInterceptor 등)
    if (err.error is ApiException) {
      return err.error as ApiException;
    }

    // 연결 오류 (타임아웃, DNS 실패 등)
    if (err.type == DioExceptionType.connectionTimeout ||
        err.type == DioExceptionType.receiveTimeout ||
        err.type == DioExceptionType.sendTimeout ||
        err.type == DioExceptionType.connectionError) {
      return const NetworkException();
    }

    final statusCode = err.response?.statusCode;

    return switch (statusCode) {
      401 => const UnauthorizedException(),
      403 => const ForbiddenException(),
      404 => const NotFoundException(),
      422 => ValidationException(
          message: _extractMessage(err) ?? '입력값을 확인해주세요.',
        ),
      429 => const QuotaExceededException(),
      int code when code >= 500 => ServerException(statusCode: code),
      _ => const UnknownException(),
    };
  }

  /// 응답 바디에서 에러 메시지 추출
  String? _extractMessage(DioException err) {
    try {
      final data = err.response?.data;
      if (data is Map<String, dynamic>) {
        return data['message'] as String? ?? data['error'] as String?;
      }
    } catch (_) {
      // 파싱 실패 시 null 반환
    }
    return null;
  }
}
