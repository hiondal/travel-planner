import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/routing/app_routes.dart';
import '../../../../core/utils/secure_storage.dart';

/// 스플래시 페이지
/// - 앱 진입 시 토큰 유효성 확인
/// - 토큰 있음 → /schedule 이동
/// - 토큰 없음 → /auth/login 이동
///
/// TODO: 실제 구현 시 브랜드 로고 애니메이션 추가
class SplashPage extends ConsumerStatefulWidget {
  const SplashPage({super.key});

  @override
  ConsumerState<SplashPage> createState() => _SplashPageState();
}

class _SplashPageState extends ConsumerState<SplashPage> {
  @override
  void initState() {
    super.initState();
    _checkAuthAndNavigate();
  }

  Future<void> _checkAuthAndNavigate() async {
    // 스플래시 최소 표시 시간 (브랜드 노출)
    await Future.delayed(const Duration(milliseconds: 1500));

    if (!mounted) return;

    final secureStorage = ref.read(secureStorageProvider);
    final hasToken = await secureStorage.hasAccessToken();

    if (!mounted) return;

    if (hasToken) {
      context.goNamed(AppRoutes.tripListName);
    } else {
      context.goNamed(AppRoutes.loginName);
    }
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // TODO: 브랜드 로고 위젯으로 교체
            Icon(Icons.explore, size: 64),
            SizedBox(height: 16),
            Text('travel-planner'),
          ],
        ),
      ),
    );
  }
}
