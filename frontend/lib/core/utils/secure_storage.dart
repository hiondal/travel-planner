import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'secure_storage.g.dart';

/// flutter_secure_storage 키 상수
abstract final class _Keys {
  static const String accessToken = 'access_token';
  static const String refreshToken = 'refresh_token';
}

/// flutter_secure_storage 래퍼
/// JWT 토큰의 안전한 저장/조회/삭제를 담당한다.
@riverpod
SecureStorage secureStorage(Ref ref) {
  return SecureStorage();
}

class SecureStorage {
  SecureStorage()
      : _storage = const FlutterSecureStorage(
          aOptions: AndroidOptions(encryptedSharedPreferences: true),
          iOptions: IOSOptions(
            accessibility: KeychainAccessibility.first_unlock_this_device,
          ),
        );

  final FlutterSecureStorage _storage;

  // ---------------------------------------------------------------------------
  // 액세스 토큰
  // ---------------------------------------------------------------------------

  Future<void> saveAccessToken(String token) async {
    await _storage.write(key: _Keys.accessToken, value: token);
  }

  Future<String?> getAccessToken() async {
    return _storage.read(key: _Keys.accessToken);
  }

  Future<void> deleteAccessToken() async {
    await _storage.delete(key: _Keys.accessToken);
  }

  // ---------------------------------------------------------------------------
  // 리프레시 토큰
  // ---------------------------------------------------------------------------

  Future<void> saveRefreshToken(String token) async {
    await _storage.write(key: _Keys.refreshToken, value: token);
  }

  Future<String?> getRefreshToken() async {
    return _storage.read(key: _Keys.refreshToken);
  }

  Future<void> deleteRefreshToken() async {
    await _storage.delete(key: _Keys.refreshToken);
  }

  // ---------------------------------------------------------------------------
  // 전체 토큰 삭제 (로그아웃)
  // ---------------------------------------------------------------------------

  Future<void> clearTokens() async {
    await Future.wait([
      deleteAccessToken(),
      deleteRefreshToken(),
    ]);
  }

  /// 토큰 존재 여부 (로그인 상태 판단용)
  Future<bool> hasAccessToken() async {
    final token = await getAccessToken();
    return token != null && token.isNotEmpty;
  }
}
