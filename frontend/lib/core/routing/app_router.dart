import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../features/auth/presentation/pages/login_page.dart';
import '../../features/auth/presentation/pages/onboarding_page.dart';
import '../../features/auth/presentation/pages/splash_page.dart';
import '../../features/briefing/presentation/pages/alternative_card_page.dart';
import '../../features/briefing/presentation/pages/briefing_detail_page.dart';
import '../../features/briefing/presentation/pages/briefing_list_page.dart';
import '../../features/briefing/presentation/pages/paywall_page.dart';
import '../../features/payment/presentation/pages/payment_checkout_page.dart';
import '../../features/payment/presentation/pages/payment_success_page.dart';
import '../../features/profile/presentation/pages/location_consent_page.dart';
import '../../features/profile/presentation/pages/notification_settings_page.dart';
import '../../features/profile/presentation/pages/profile_page.dart';
import '../../features/profile/presentation/pages/subscription_page.dart';
import '../../features/schedule/presentation/pages/permission_page.dart';
import '../../features/schedule/presentation/pages/place_search_page.dart';
import '../../features/schedule/presentation/pages/place_time_picker_page.dart';
import '../../features/schedule/presentation/pages/schedule_change_result_page.dart';
import '../../features/schedule/presentation/pages/schedule_detail_page.dart';
import '../../features/schedule/presentation/pages/trip_create_page.dart';
import '../../features/schedule/presentation/pages/trip_list_page.dart';
import '../../shared/providers/app_user_provider.dart';
import 'app_routes.dart';
import 'router_guard.dart';

part 'app_router.g.dart';

/// GoRouter 루트 정의 (전체 라우트 트리)
/// ia.md 사이트맵 기반으로 라우트를 구성한다.
@riverpod
GoRouter appRouter(Ref ref) {
  final guard = RouterGuard(ref: ref);

  // 인증 상태 변경 시 라우터 갱신을 위한 리스너
  final authListenable = ValueNotifier<bool>(false);
  ref.listen(appUserProvider, (_, __) {
    authListenable.value = !authListenable.value;
  });

  return GoRouter(
    initialLocation: AppRoutes.splashPath,
    refreshListenable: authListenable,
    redirect: guard.redirect,
    debugLogDiagnostics: true,

    routes: [
      // -----------------------------------------------------------------------
      // 스플래시 (앱 진입점)
      // -----------------------------------------------------------------------
      GoRoute(
        path: AppRoutes.splashPath,
        name: AppRoutes.splashName,
        builder: (context, state) => const SplashPage(),
      ),

      // -----------------------------------------------------------------------
      // 인증/온보딩 라우트
      // -----------------------------------------------------------------------
      GoRoute(
        path: AppRoutes.loginPath,
        name: AppRoutes.loginName,
        builder: (context, state) => const LoginPage(),
      ),
      GoRoute(
        path: AppRoutes.onboardingPath,
        name: AppRoutes.onboardingName,
        builder: (context, state) {
          final step = int.tryParse(
                state.uri.queryParameters['step'] ?? '1',
              ) ??
              1;
          return OnboardingPage(initialStep: step);
        },
      ),

      // -----------------------------------------------------------------------
      // 메인 쉘 (BottomTabBar)
      // -----------------------------------------------------------------------
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) {
          return MainShellPage(navigationShell: navigationShell);
        },
        branches: [
          // TAB 1: 일정
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppRoutes.tripListPath,
                name: AppRoutes.tripListName,
                builder: (context, state) => const TripListPage(),
                routes: [
                  GoRoute(
                    path: 'new',
                    name: AppRoutes.tripCreateName,
                    builder: (context, state) => const TripCreatePage(),
                    routes: [
                      GoRoute(
                        path: 'permission',
                        name: AppRoutes.permissionName,
                        builder: (context, state) => const PermissionPage(),
                      ),
                    ],
                  ),
                  GoRoute(
                    path: ':tripId',
                    name: AppRoutes.scheduleDetailName,
                    builder: (context, state) {
                      final tripId = state.pathParameters['tripId']!;
                      final startDateStr = state.uri.queryParameters['startDate'];
                      final endDateStr = state.uri.queryParameters['endDate'];
                      final startDate = startDateStr != null
                          ? DateTime.parse(startDateStr)
                          : DateTime.now();
                      final endDate = endDateStr != null
                          ? DateTime.parse(endDateStr)
                          : DateTime.now().add(const Duration(days: 6));
                      return ScheduleDetailPage(
                        tripId: tripId,
                        startDate: startDate,
                        endDate: endDate,
                      );
                    },
                    routes: [
                      GoRoute(
                        path: 'place/search',
                        name: AppRoutes.placeSearchName,
                        builder: (context, state) {
                          final tripId = state.pathParameters['tripId']!;
                          return PlaceSearchPage(
                            tripId: tripId,
                            startDate: state.uri.queryParameters['startDate'],
                            endDate: state.uri.queryParameters['endDate'],
                          );
                        },
                        routes: [
                          GoRoute(
                            path: 'time',
                            name: AppRoutes.placeTimePickerName,
                            builder: (context, state) {
                              final tripId = state.pathParameters['tripId']!;
                              final placeId =
                                  state.uri.queryParameters['placeId'];
                              final startDateStr =
                                  state.uri.queryParameters['startDate'];
                              final endDateStr =
                                  state.uri.queryParameters['endDate'];
                              return PlaceTimePickerPage(
                                tripId: tripId,
                                placeId: placeId,
                                startDate: startDateStr,
                                endDate: endDateStr,
                              );
                            },
                          ),
                        ],
                      ),
                      GoRoute(
                        path: 'result',
                        name: AppRoutes.scheduleChangeResultName,
                        builder: (context, state) {
                          final tripId = state.pathParameters['tripId']!;
                          final alternativeId =
                              state.uri.queryParameters['alternativeId'];
                          return ScheduleChangeResultPage(
                            tripId: tripId,
                            alternativeId: alternativeId,
                          );
                        },
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),

          // TAB 2: 브리핑
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppRoutes.briefingListPath,
                name: AppRoutes.briefingListName,
                builder: (context, state) => const BriefingListPage(),
                routes: [
                  GoRoute(
                    path: 'alternative/:briefingId',
                    name: AppRoutes.alternativeCardName,
                    builder: (context, state) {
                      final briefingId =
                          state.pathParameters['briefingId']!;
                      return AlternativeCardPage(briefingId: briefingId);
                    },
                  ),
                  GoRoute(
                    path: 'paywall',
                    name: AppRoutes.paywallName,
                    builder: (context, state) {
                      final from = state.uri.queryParameters['from'];
                      return PaywallPage(from: from);
                    },
                  ),
                  GoRoute(
                    path: ':briefingId',
                    name: AppRoutes.briefingDetailName,
                    builder: (context, state) {
                      final briefingId =
                          state.pathParameters['briefingId']!;
                      return BriefingDetailPage(briefingId: briefingId);
                    },
                  ),
                ],
              ),
            ],
          ),

          // TAB 3: 마이페이지
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppRoutes.profilePath,
                name: AppRoutes.profileName,
                builder: (context, state) => const ProfilePage(),
                routes: [
                  GoRoute(
                    path: 'subscription',
                    name: AppRoutes.subscriptionName,
                    builder: (context, state) => const SubscriptionPage(),
                  ),
                  GoRoute(
                    path: 'notifications',
                    name: AppRoutes.notificationSettingsName,
                    builder: (context, state) =>
                        const NotificationSettingsPage(),
                  ),
                  GoRoute(
                    path: 'location',
                    name: AppRoutes.locationConsentName,
                    builder: (context, state) => const LocationConsentPage(),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),

      // -----------------------------------------------------------------------
      // 결제 라우트 (탭 외부 전역)
      // -----------------------------------------------------------------------
      GoRoute(
        path: AppRoutes.paymentCheckoutPath,
        name: AppRoutes.paymentCheckoutName,
        builder: (context, state) {
          final plan = state.uri.queryParameters['plan'];
          return PaymentCheckoutPage(plan: plan);
        },
      ),
      GoRoute(
        path: AppRoutes.paymentSuccessPath,
        name: AppRoutes.paymentSuccessName,
        builder: (context, state) {
          final plan = state.uri.queryParameters['plan'];
          return PaymentSuccessPage(plan: plan);
        },
      ),
    ],

    errorBuilder: (context, state) => Scaffold(
      body: Center(
        child: Text('페이지를 찾을 수 없습니다: ${state.error}'),
      ),
    ),
  );
}

// ---------------------------------------------------------------------------
// MainShellPage — BottomTabBar 쉘 위젯
// ---------------------------------------------------------------------------

class MainShellPage extends StatelessWidget {
  const MainShellPage({
    super.key,
    required this.navigationShell,
  });

  final StatefulNavigationShell navigationShell;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: NavigationBar(
        selectedIndex: navigationShell.currentIndex,
        onDestinationSelected: (index) {
          navigationShell.goBranch(
            index,
            initialLocation: index == navigationShell.currentIndex,
          );
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.calendar_today_outlined),
            selectedIcon: Icon(Icons.calendar_today),
            label: '일정',
          ),
          NavigationDestination(
            icon: Icon(Icons.notifications_outlined),
            selectedIcon: Icon(Icons.notifications),
            label: '브리핑',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: '마이페이지',
          ),
        ],
      ),
    );
  }
}
