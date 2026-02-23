import 'package:flutter/material.dart';

/// 로그인 페이지
/// - Google 소셜 로그인 (UFR-AUTH-010)
/// - Apple 소셜 로그인 (UFR-AUTH-010)
///
/// TODO: 소셜 로그인 버튼 위젯 구현
/// TODO: google_sign_in, sign_in_with_apple 연동
class LoginPage extends StatelessWidget {
  const LoginPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // TODO: 히어로 이미지/로고 영역
              const Icon(Icons.explore, size: 64),
              const SizedBox(height: 24),
              const Text(
                '여행 중 실시간 가이드',
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 48),
              // TODO: GoogleSignInButton 위젯으로 교체
              ElevatedButton(
                onPressed: () {
                  // TODO: Google 로그인 처리
                },
                child: const Text('Google로 계속하기'),
              ),
              const SizedBox(height: 12),
              // TODO: AppleSignInButton 위젯으로 교체
              OutlinedButton(
                onPressed: () {
                  // TODO: Apple 로그인 처리
                },
                child: const Text('Apple로 계속하기'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
