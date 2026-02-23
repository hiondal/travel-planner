/// 앱 공통 예외 타입 정의
/// HTTP 에러를 도메인 의미 있는 예외로 변환한다.
sealed class ApiException implements Exception {
  const ApiException({
    required this.message,
    this.statusCode,
  });

  final String message;
  final int? statusCode;

  @override
  String toString() => 'ApiException($statusCode): $message';
}

/// 401 Unauthorized — 인증 토큰 만료 또는 미인증
final class UnauthorizedException extends ApiException {
  const UnauthorizedException({super.message = '인증이 필요합니다.'})
      : super(statusCode: 401);
}

/// 403 Forbidden — 권한 부족 (구독 티어 등)
final class ForbiddenException extends ApiException {
  const ForbiddenException({super.message = '접근 권한이 없습니다.'})
      : super(statusCode: 403);
}

/// 404 Not Found
final class NotFoundException extends ApiException {
  const NotFoundException({super.message = '요청한 리소스를 찾을 수 없습니다.'})
      : super(statusCode: 404);
}

/// 422 Unprocessable Entity — 유효성 오류
final class ValidationException extends ApiException {
  const ValidationException({required super.message}) : super(statusCode: 422);
}

/// 429 Too Many Requests — 사용 한도 초과
final class QuotaExceededException extends ApiException {
  const QuotaExceededException({super.message = '사용 한도를 초과했습니다.'})
      : super(statusCode: 429);
}

/// 5xx Server Error
final class ServerException extends ApiException {
  const ServerException({
    super.message = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
    super.statusCode,
  });
}

/// 네트워크 연결 오류 (타임아웃, DNS 등)
final class NetworkException extends ApiException {
  const NetworkException({super.message = '네트워크 연결을 확인해주세요.'})
      : super(statusCode: null);
}

/// 기타 알 수 없는 오류
final class UnknownException extends ApiException {
  const UnknownException({super.message = '알 수 없는 오류가 발생했습니다.'})
      : super(statusCode: null);
}
