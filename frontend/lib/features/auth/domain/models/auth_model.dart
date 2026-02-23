/// 소셜 로그인 요청 모델
class SocialLoginRequest {
  const SocialLoginRequest({
    required this.provider,
    required this.oauthCode,
  });

  final String provider;
  final String oauthCode;

  Map<String, dynamic> toJson() => {
        'provider': provider,
        'oauth_code': oauthCode,
      };
}

/// 소셜 로그인 응답 모델 (api-mapping.md API-01 기반)
class AuthTokenResponse {
  const AuthTokenResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.userProfile,
  });

  final String accessToken;
  final String refreshToken;
  final UserProfile userProfile;

  factory AuthTokenResponse.fromJson(Map<String, dynamic> json) {
    return AuthTokenResponse(
      accessToken: json['access_token'] as String,
      refreshToken: json['refresh_token'] as String,
      userProfile: UserProfile.fromJson(
        json['user_profile'] as Map<String, dynamic>,
      ),
    );
  }
}

/// 사용자 프로필 모델
class UserProfile {
  const UserProfile({
    required this.userId,
    required this.nickname,
    this.avatarUrl,
    required this.tier,
    required this.isNewUser,
  });

  final String userId;
  final String nickname;
  final String? avatarUrl;
  final String tier;
  final bool isNewUser;

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      userId: json['user_id'] as String,
      nickname: json['nickname'] as String,
      avatarUrl: json['avatar_url'] as String?,
      tier: json['tier'] as String,
      isNewUser: json['is_new_user'] as bool,
    );
  }
}

/// 토큰 갱신 응답 모델 (api-mapping.md API-02 기반)
class TokenRefreshResponse {
  const TokenRefreshResponse({
    required this.accessToken,
    required this.expiresIn,
  });

  final String accessToken;
  final int expiresIn;

  factory TokenRefreshResponse.fromJson(Map<String, dynamic> json) {
    return TokenRefreshResponse(
      accessToken: json['access_token'] as String,
      expiresIn: json['expires_in'] as int,
    );
  }
}
