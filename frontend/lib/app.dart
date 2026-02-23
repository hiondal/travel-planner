import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/routing/app_router.dart';
import 'core/theme/app_theme.dart';

/// MaterialApp.router 진입점
/// go_router와 앱 테마를 주입한다.
class TravelPlannerApp extends ConsumerWidget {
  const TravelPlannerApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(appRouterProvider);

    return MaterialApp.router(
      title: 'travel-planner',
      debugShowCheckedModeBanner: false,

      // 다크 테마 단일 적용 (라이트 테마 미사용)
      theme: AppTheme.darkTheme,
      themeMode: ThemeMode.dark,

      // go_router 연결
      routerConfig: router,
    );
  }
}
