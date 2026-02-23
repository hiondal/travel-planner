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
    } on AppException {
      rethrow;
    }
  }

  /// 로그아웃
  /// 서버 토큰 폐기 후 로컬 토큰 삭제
  Future<void> logout() async {
    try {
      await dataSource.logout();
    } catch (_) {
      // 서버 오류여도 로컬 토큰은 반드시 삭제
    } finally {
      await secureStorage.clearTokens();
    }
  }

  /// 현재 사용자 프로필 조회
  Future<UserProfile> getMe() async {
    return dataSource.getMe();
  }
}
