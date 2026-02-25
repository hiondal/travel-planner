import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../shared/models/subscription_tier.dart';
import '../../../../shared/providers/app_user_provider.dart';
import '../../data/repositories/auth_repository.dart';

part 'auth_provider.g.dart';

/// 소셜 로그인 AsyncNotifier
@riverpod
class SocialLoginNotifier extends _$SocialLoginNotifier {
  @override
  AsyncValue<void> build() => const AsyncValue.data(null);

  Future<bool> login({
    required String provider,
    required String oauthCode,
  }) async {
    state = const AsyncValue.loading();
    final result = await AsyncValue.guard(() async {
      final repo = ref.read(authRepositoryProvider);
      final profile = await repo.socialLogin(
        provider: provider,
        oauthCode: oauthCode,
      );
      // 전역 사용자 상태 갱신
      ref.read(appUserProvider.notifier).signIn(
            userId: profile.userId,
            email: profile.nickname,
            tier: SubscriptionTier.fromString(profile.tier),
          );
      return profile.isNewUser;
    });

    state = result.when(
      data: (_) => const AsyncValue.data(null),
      loading: AsyncValue.loading,
      error: AsyncValue.error,
    );

    return result.valueOrNull ?? false;
  }
}

/// 로그아웃 AsyncNotifier
@riverpod
class LogoutNotifier extends _$LogoutNotifier {
  @override
  AsyncValue<void> build() => const AsyncValue.data(null);

  Future<void> logout() async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await ref.read(authRepositoryProvider).logout();
      ref.read(appUserProvider.notifier).signOut();
    });
  }
}

/// dev 환경 전용 테스트 로그인 AsyncNotifier
/// POST /test/login — TestAuthController (@Profile("dev"))
/// 실제 백엔드에서 JWT를 발급받아 저장하므로 토큰 갱신 흐름도 정상 동작
@riverpod
class TestLoginNotifier extends _$TestLoginNotifier {
  @override
  AsyncValue<void> build() => const AsyncValue.data(null);

  Future<bool> login(String userId) async {
    state = const AsyncValue.loading();
    final result = await AsyncValue.guard(() async {
      final repo = ref.read(authRepositoryProvider);
      final profile = await repo.testLogin(userId);
      ref.read(appUserProvider.notifier).signIn(
            userId: profile.userId,
            email: profile.nickname,
            tier: SubscriptionTier.fromString(profile.tier),
          );
      return profile.isNewUser;
    });

    state = result.when(
      data: (_) => const AsyncValue.data(null),
      loading: AsyncValue.loading,
      error: AsyncValue.error,
    );

    return result.valueOrNull ?? false;
  }
}
