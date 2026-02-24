import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../core/utils/secure_storage.dart';
import '../../../../shared/models/subscription_tier.dart';
import '../../../../shared/providers/app_user_provider.dart';

/// 스플래시 페이지 (SCR-000)
/// - 앱 진입 시 1.5초 브랜드 표시
/// - Access Token 존재 → /trips 이동
/// - 토큰 없음 → /login 이동
class SplashPage extends ConsumerStatefulWidget {
  const SplashPage({super.key});

  @override
  ConsumerState<SplashPage> createState() => _SplashPageState();
}

class _SplashPageState extends ConsumerState<SplashPage>
    with SingleTickerProviderStateMixin {
  late AnimationController _fadeController;
  late Animation<double> _fadeAnimation;

  @override
  void initState() {
    super.initState();
    _fadeController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    );
    _fadeAnimation = CurvedAnimation(
      parent: _fadeController,
      curve: Curves.easeIn,
    );
    _fadeController.forward();
    _checkAuthAndNavigate();
  }

  Future<void> _checkAuthAndNavigate() async {
    await Future.delayed(const Duration(milliseconds: 1500));
    if (!mounted) return;

    final secureStorage = ref.read(secureStorageProvider);
    final accessToken = await secureStorage.getAccessToken();
    final hasToken = accessToken != null && accessToken.isNotEmpty;

    if (!mounted) return;

    if (hasToken) {
      // JWT 페이로드 디코딩 → 인메모리 auth 상태 복원
      try {
        final parts = accessToken.split('.');
        if (parts.length == 3) {
          final normalized = base64Url.normalize(parts[1]);
          final decoded = utf8.decode(base64Url.decode(normalized));
          final payload = jsonDecode(decoded) as Map<String, dynamic>;
          ref.read(appUserProvider.notifier).signIn(
                userId: payload['sub'] as String? ?? '',
                email: payload['email'] as String? ?? '',
                tier: SubscriptionTier.fromString(
                    payload['tier'] as String? ?? 'FREE'),
              );
        }
        context.goNamed(AppRoutes.tripListName);
      } catch (_) {
        // JWT 디코딩 실패 → 토큰 파기 후 로그인
        await secureStorage.clearTokens();
        if (mounted) context.goNamed(AppRoutes.loginName);
      }
    } else {
      context.goNamed(AppRoutes.loginName);
    }
  }

  @override
  void dispose() {
    _fadeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      body: FadeTransition(
        opacity: _fadeAnimation,
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // 브랜드 아이콘
              Container(
                width: 80,
                height: 80,
                decoration: BoxDecoration(
                  color: AppColors.accentRed.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: const Icon(
                  Icons.explore,
                  size: 48,
                  color: AppColors.accentRed,
                ),
              ),
              const SizedBox(height: AppSpacing.spaceXl),
              // 앱 이름
              Text(
                'travel-planner',
                style: AppTypography.displayMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: AppSpacing.spaceSm),
              // 슬로건
              Text(
                '여행 중 실시간 일정 최적화 가이드',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(height: AppSpacing.space3xl),
              // 로딩 인디케이터
              const SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  valueColor: AlwaysStoppedAnimation<Color>(AppColors.accentRed),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
