import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_sign_in/google_sign_in.dart';

import '../../data/google_auth_web_helper.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../core/utils/secure_storage.dart';
import '../../../../shared/models/subscription_tier.dart';
import '../../../../shared/providers/app_user_provider.dart';
import '../../../../shared/widgets/app_snack_bar.dart';
import '../providers/auth_provider.dart';

/// 소셜 로그인 화면 (SCR-001)
/// UFR-AUTH-010: Google / Apple OAuth 인증
class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  bool _isGoogleLoading = false;
  bool _isAppleLoading = false;

  /// Google OAuth 실제 로그인
  Future<void> _handleGoogleLogin() async {
    setState(() => _isGoogleLoading = true);
    try {
      String oauthCode;

      if (kIsWeb) {
        // Web: GIS 직접 호출 (google_sign_in 플러그인의 People API 의존성 우회)
        const clientId = '1052974040617-bhirm9gls5h3vgias0nqmhe7lkai1rf3.apps.googleusercontent.com';
        final token = await triggerGoogleSignInWeb(clientId);
        if (token == null) {
          if (mounted) setState(() => _isGoogleLoading = false);
          return;
        }
        oauthCode = token;
      } else {
        // Mobile: google_sign_in 패키지 사용
        final googleSignIn = GoogleSignIn(scopes: ['email', 'profile']);
        final account = await googleSignIn.signIn();
        if (account == null) {
          if (mounted) setState(() => _isGoogleLoading = false);
          return;
        }
        final auth = await account.authentication;
        oauthCode = account.serverAuthCode
            ?? auth.idToken
            ?? auth.accessToken
            ?? '';
      }

      if (oauthCode.isEmpty) {
        throw Exception('Google 인증 코드를 가져올 수 없습니다.');
      }

      final notifier = ref.read(socialLoginNotifierProvider.notifier);
      final isNewUser = await notifier.login(
        provider: 'google',
        oauthCode: oauthCode,
      );
      if (!mounted) return;
      if (isNewUser) {
        context.goNamed(AppRoutes.onboardingName);
      } else {
        context.goNamed(AppRoutes.tripListName);
      }
    } catch (e) {
      if (mounted) {
        AppSnackBar.showError(context, 'Google 로그인에 실패했습니다.');
      }
    } finally {
      if (mounted) setState(() => _isGoogleLoading = false);
    }
  }

  Future<void> _handleAppleLogin() async {
    setState(() => _isAppleLoading = true);
    try {
      final notifier = ref.read(socialLoginNotifierProvider.notifier);
      final isNewUser = await notifier.login(
        provider: 'apple',
        oauthCode: 'mock_apple_code_for_prism',
      );
      if (!mounted) return;
      if (isNewUser) {
        context.goNamed(AppRoutes.onboardingName);
      } else {
        context.goNamed(AppRoutes.tripListName);
      }
    } catch (e) {
      if (mounted) {
        AppSnackBar.showError(context, 'Apple 로그인에 실패했습니다.');
      }
    } finally {
      if (mounted) setState(() => _isAppleLoading = false);
    }
  }

  /// 개발 환경 전용: 유효한 JWT로 바이패스
  Future<void> _handleDevBypass() async {
    // HS256 JWT signed with dev secret, 1-year expiry
    const devToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.'
        'eyJzdWIiOiJkZXYtdXNlciIsImVtYWlsIjoiZGV2QHRlc3QuY29tIiwidGllciI6IlBSTyIsImlhdCI6MTc3MTkzNjk4OSwiZXhwIjoxODAzNDcyOTg5fQ.'
        '0v53MnXAf5_u1iSpHrvBBRG-PnWXFdHAn29u3pJVVog';
    final storage = ref.read(secureStorageProvider);
    await storage.saveAccessToken(devToken);
    await storage.saveRefreshToken(devToken);
    ref.read(appUserProvider.notifier).signIn(
      userId: 'dev-user',
      email: 'dev@test.com',
      tier: SubscriptionTier.pro,
    );
    if (mounted) context.goNamed(AppRoutes.tripListName);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.screenHorizontalPadding,
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // 히어로 영역
              _buildHeroSection(),
              const SizedBox(height: AppSpacing.space3xl),

              // 소셜 로그인 버튼들
              _buildGoogleSignInButton(),
              const SizedBox(height: AppSpacing.spaceMd),
              _buildAppleSignInButton(),

              const SizedBox(height: AppSpacing.spaceXl),
              // 개발 환경 바이패스 (Prism Mock 테스트용)
              TextButton(
                onPressed: _handleDevBypass,
                child: Text(
                  '[DEV] 토큰 없이 바로 진입',
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textDisabled,
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.spaceXl),
              // 약관 동의 안내
              Text(
                '계속하면 서비스 이용약관 및 개인정보처리방침에\n동의하는 것으로 간주합니다.',
                textAlign: TextAlign.center,
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeroSection() {
    return Column(
      children: [
        // 앱 아이콘
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
        Text(
          'travel-planner',
          textAlign: TextAlign.center,
          style: AppTypography.displayLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: AppSpacing.spaceSm),
        Text(
          '여행 중 예상치 못한 상황에서\n최적의 대안을 바로 찾아드립니다',
          textAlign: TextAlign.center,
          style: AppTypography.bodyLarge.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
      ],
    );
  }

  Widget _buildGoogleSignInButton() {
    return SizedBox(
      height: AppSpacing.buttonHeight,
      child: ElevatedButton(
        onPressed: _isGoogleLoading ? null : _handleGoogleLogin,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.bgCard,
          foregroundColor: AppColors.textPrimary,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
            side: const BorderSide(color: AppColors.outline),
          ),
          elevation: 0,
        ),
        child: _isGoogleLoading
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: AppColors.textPrimary,
                ),
              )
            : Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // Google 로고 대체 아이콘
                  Container(
                    width: 20,
                    height: 20,
                    decoration: const BoxDecoration(
                      color: Colors.white,
                      shape: BoxShape.circle,
                    ),
                    child: const Center(
                      child: Text(
                        'G',
                        style: TextStyle(
                          color: Color(0xFF4285F4),
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.spaceSm),
                  Text(
                    'Google로 계속하기',
                    style: AppTypography.labelLarge.copyWith(
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
      ),
    );
  }

  Widget _buildAppleSignInButton() {
    return SizedBox(
      height: AppSpacing.buttonHeight,
      child: OutlinedButton(
        onPressed: _isAppleLoading ? null : _handleAppleLogin,
        style: OutlinedButton.styleFrom(
          backgroundColor: Colors.transparent,
          foregroundColor: AppColors.textPrimary,
          side: const BorderSide(color: AppColors.outline),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
          ),
        ),
        child: _isAppleLoading
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: AppColors.textPrimary,
                ),
              )
            : Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.apple, size: 20),
                  const SizedBox(width: AppSpacing.spaceSm),
                  Text(
                    'Apple로 계속하기',
                    style: AppTypography.labelLarge.copyWith(
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}
