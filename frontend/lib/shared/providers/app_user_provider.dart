import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../models/subscription_tier.dart';

part 'app_user_provider.g.dart';

/// 전역 앱 사용자 상태
/// 라우터 가드, 구독 티어 분기 등 앱 전역에서 참조한다.
@Riverpod(keepAlive: true)
class AppUser extends _$AppUser {
  @override
  AppUserState build() {
    return const AppUserState();
  }

  /// 로그인 완료 시 호출
  void signIn({
    required String userId,
    required String email,
    required SubscriptionTier tier,
  }) {
    state = state.copyWith(
      isAuthenticated: true,
      userId: userId,
      email: email,
      subscriptionTier: tier,
    );
  }

  /// 로그아웃 시 호출
  void signOut() {
    state = const AppUserState();
  }

  /// 구독 티어 갱신
  void updateSubscriptionTier(SubscriptionTier tier) {
    state = state.copyWith(subscriptionTier: tier);
  }
}

/// 앱 사용자 상태 모델
class AppUserState {
  const AppUserState({
    this.isAuthenticated = false,
    this.userId,
    this.email,
    this.subscriptionTier = SubscriptionTier.free,
  });

  final bool isAuthenticated;
  final String? userId;
  final String? email;
  final SubscriptionTier subscriptionTier;

  bool get isPremium => subscriptionTier.isPremium;

  AppUserState copyWith({
    bool? isAuthenticated,
    String? userId,
    String? email,
    SubscriptionTier? subscriptionTier,
  }) {
    return AppUserState(
      isAuthenticated: isAuthenticated ?? this.isAuthenticated,
      userId: userId ?? this.userId,
      email: email ?? this.email,
      subscriptionTier: subscriptionTier ?? this.subscriptionTier,
    );
  }
}
