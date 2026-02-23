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
  /// 서버 토큰 폐기
  Future<void> logout() async {
    await dio.post<void>('/auth/logout');
  }

  /// GET /auth/me
  /// 현재 사용자 프로필 조회
  Future<UserProfile> getMe() async {
    final response = await dio.get<Map<String, dynamic>>('/auth/me');
    return UserProfile.fromJson(response.data!);
  }
}
