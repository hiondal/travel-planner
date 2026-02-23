import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../shared/providers/app_user_provider.dart';
import 'app_routes.dart';

/// 인증 상태 기반 리다이렉트 로직
/// GoRouter의 redirect 콜백에서 사용한다.
class RouterGuard {
  RouterGuard({required this.ref});

  final Ref ref;

  /// GoRouter redirect 콜백
  /// - 미인증 상태에서 보호된 경로 접근 시 /auth/login 리다이렉트
  /// - 인증 상태에서 /splash, /auth/* 접근 시 /schedule 리다이렉트
  String? redirect(GoRouterState state) {
    final appUserState = ref.read(appUserProvider);
    final isAuthenticated = appUserState.isAuthenticated;
    final location = state.matchedLocation;

    // 공개 경로 (인증 불필요)
    final isPublicRoute = location.startsWith('/auth') ||
        location == AppRoutes.splashPath;

    if (!isAuthenticated && !isPublicRoute) {
      // 미인증 → 로그인 페이지 리다이렉트
      return AppRoutes.loginPath;
    }

    if (isAuthenticated && isPublicRoute && location != AppRoutes.splashPath) {
      // 인증 완료 상태에서 로그인/온보딩 재진입 시 메인으로
      return AppRoutes.tripListPath;
    }

    // 리다이렉트 없음
    return null;
  }
}
