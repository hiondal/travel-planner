import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../core/network/dio_client.dart';
import '../../domain/models/auth_model.dart';

part 'auth_datasource.g.dart';

/// Auth Service DataSource
/// Prism Mock: http://localhost:4010
/// api-mapping.md API-01 ~ API-04 기반 엔드포인트 구현
@riverpod
AuthDataSource authDataSource(Ref ref) {
  final dio = ref.watch(dioClientProvider);
  return AuthDataSource(dio: dio);
}

class AuthDataSource {
  AuthDataSource({required this.dio});

  final Dio dio;

  /// POST /auth/social-login
  /// Google / Apple OAuth 인증 코드 → JWT 발급
  Future<AuthTokenResponse> socialLogin(SocialLoginRequest request) async {
    final response = await dio.post<Map<String, dynamic>>(
      '/auth/social-login',
      data: request.toJson(),
    );
    return AuthTokenResponse.fromJson(response.data!);
  }

  /// POST /auth/token/refresh
  /// Access Token 만료 시 Refresh Token으로 재발급
  Future<TokenRefreshResponse> refreshToken(String refreshToken) async {
    final response = await dio.post<Map<String, dynamic>>(
      '/auth/token/refresh',
      data: {'refresh_token': refreshToken},
    );
    return TokenRefreshResponse.fromJson(response.data!);
  }

  /// POST /auth/logout
  /// 서버 토큰 폐기 (백엔드: refresh_token 필드 필요)
  Future<void> logout(String refreshToken) async {
    await dio.post<void>(
      '/auth/logout',
      data: {'refresh_token': refreshToken},
    );
  }

  /// POST /users/consent
  /// 사용자 위치정보/Push 동의 저장
  /// 주의: 백엔드에 /auth/me 엔드포인트 없음
  /// 사용자 프로필은 socialLogin 응답의 user 필드에서 획득
  Future<Map<String, dynamic>> saveConsent({
    required bool location,
    required bool push,
    required String timestamp,
  }) async {
    final response = await dio.post<Map<String, dynamic>>(
      '/users/consent',
      data: {
        'location': location,
        'push': push,
        'timestamp': timestamp,
      },
    );
    return response.data!;
  }
}
