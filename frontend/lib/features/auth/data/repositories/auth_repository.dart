import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../core/network/api_exception.dart';
import '../../../../core/utils/secure_storage.dart';
import '../../domain/models/auth_model.dart';
import '../datasources/auth_datasource.dart';

part 'auth_repository.g.dart';

@riverpod
AuthRepository authRepository(Ref ref) {
  return AuthRepository(
    dataSource: ref.watch(authDataSourceProvider),
    secureStorage: ref.watch(secureStorageProvider),
  );
}

/// Auth Repository
/// DataSource 호출 후 토큰 저장/삭제 처리를 담당한다.
class AuthRepository {
  AuthRepository({
    required this.dataSource,
    required this.secureStorage,
  });

  final AuthDataSource dataSource;
  final SecureStorage secureStorage;

  /// Google / Apple 소셜 로그인
  /// 성공 시 토큰을 SecureStorage에 저장하고 UserProfile 반환
  Future<UserProfile> socialLogin({
    required String provider,
    required String oauthCode,
  }) async {
    try {
      final response = await dataSource.socialLogin(
        SocialLoginRequest(provider: provider, oauthCode: oauthCode),
      );
      await Future.wait([
        secureStorage.saveAccessToken(response.accessToken),
        secureStorage.saveRefreshToken(response.refreshToken),
      ]);
      return response.userProfile;
    } on ApiException {
      rethrow;
    }
  }

  /// dev 환경 전용 테스트 로그인
  /// POST /test/login — TestAuthController (@Profile("dev"))
  /// 실제 OAuth 없이 userId만으로 JWT 발급
  Future<UserProfile> testLogin(String userId) async {
    try {
      final response = await dataSource.testLogin(userId);
      await Future.wait([
        secureStorage.saveAccessToken(response.accessToken),
        secureStorage.saveRefreshToken(response.refreshToken),
      ]);
      return response.userProfile;
    } on ApiException {
      rethrow;
    }
  }

  /// 로그아웃
  /// 서버 토큰 폐기 후 로컬 토큰 삭제
  Future<void> logout() async {
    try {
      final refreshToken = await secureStorage.getRefreshToken();
      if (refreshToken != null) {
        await dataSource.logout(refreshToken);
      }
    } catch (_) {
      // 서버 오류여도 로컬 토큰은 반드시 삭제
    } finally {
      await secureStorage.clearTokens();
    }
  }
}
