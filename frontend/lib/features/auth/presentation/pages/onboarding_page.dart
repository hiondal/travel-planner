import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';

/// 온보딩 가이드 화면 (SCR-002)
/// UFR-SCHD-005: 최초 로그인 후 서비스 소개 3단계
class OnboardingPage extends StatefulWidget {
  const OnboardingPage({super.key, this.initialStep = 1});

  final int initialStep;

  @override
  State<OnboardingPage> createState() => _OnboardingPageState();
}

class _OnboardingPageState extends State<OnboardingPage> {
  late final PageController _pageController;
  late int _currentStep;

  static const int _totalSteps = 3;

  static const List<_OnboardingData> _steps = [
    _OnboardingData(
      icon: Icons.notifications_active,
      iconColor: AppColors.accentRed,
      title: '실시간 상태 배지',
      description: '날씨, 혼잡도, 영업시간, 교통 정보를\n15분마다 자동으로 확인해 드립니다.',
    ),
    _OnboardingData(
      icon: Icons.alt_route,
      iconColor: AppColors.accentPurple,
      title: '출발 전 브리핑',
      description: 'AI가 오늘 일정을 분석하여\n출발 전 상황을 브리핑합니다.',
    ),
    _OnboardingData(
      icon: Icons.location_on,
      iconColor: AppColors.statusGreen,
      title: '위치 정보 사용 동의',
      description: '현재 위치 기반 서비스 제공을 위해\n위치 정보 사용에 동의가 필요합니다.',
    ),
  ];

  @override
  void initState() {
    super.initState();
    _currentStep = widget.initialStep.clamp(1, _totalSteps);
    _pageController = PageController(initialPage: _currentStep - 1);
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _onNext() {
    if (_currentStep < _totalSteps) {
      _pageController.nextPage(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    } else {
      _onComplete();
    }
  }

  void _onComplete() {
    context.goNamed(AppRoutes.tripCreateName);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      body: SafeArea(
        child: Column(
          children: [
            // 건너뛰기
            Align(
              alignment: Alignment.centerRight,
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.spaceBase,
                  vertical: AppSpacing.spaceSm,
                ),
                child: TextButton(
                  onPressed: _onComplete,
                  child: Text(
                    '건너뛰기',
                    style: AppTypography.bodyMedium.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                ),
              ),
            ),
            // 페이지 내용
            Expanded(
              child: PageView.builder(
                controller: _pageController,
                onPageChanged: (index) {
                  setState(() => _currentStep = index + 1);
                },
                itemCount: _totalSteps,
                itemBuilder: (context, index) {
                  return _OnboardingStepWidget(data: _steps[index]);
                },
              ),
            ),
            // 인디케이터 + 버튼
            Padding(
              padding: const EdgeInsets.all(AppSpacing.spaceBase),
              child: Column(
                children: [
                  _buildIndicator(),
                  const SizedBox(height: AppSpacing.spaceXl),
                  SizedBox(
                    width: double.infinity,
                    height: AppSpacing.buttonHeight,
                    child: ElevatedButton(
                      onPressed: _onNext,
                      child: Text(
                        _currentStep < _totalSteps ? '다음' : '시작하기',
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildIndicator() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: List.generate(_totalSteps, (index) {
        final isActive = index == _currentStep - 1;
        return AnimatedContainer(
          duration: const Duration(milliseconds: 250),
          margin: const EdgeInsets.symmetric(horizontal: 4),
          width: isActive ? 24 : 8,
          height: 8,
          decoration: BoxDecoration(
            color: isActive ? AppColors.accentRed : AppColors.textDisabled,
            borderRadius: BorderRadius.circular(4),
          ),
        );
      }),
    );
  }
}

class _OnboardingData {
  const _OnboardingData({
    required this.icon,
    required this.iconColor,
    required this.title,
    required this.description,
  });

  final IconData icon;
  final Color iconColor;
  final String title;
  final String description;
}

class _OnboardingStepWidget extends StatelessWidget {
  const _OnboardingStepWidget({required this.data});

  final _OnboardingData data;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.spaceBase),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            width: 120,
            height: 120,
            decoration: BoxDecoration(
              color: data.iconColor.withOpacity(0.12),
              borderRadius: BorderRadius.circular(32),
            ),
            child: Icon(data.icon, size: 60, color: data.iconColor),
          ),
          const SizedBox(height: AppSpacing.space2xl),
          Text(
            data.title,
            textAlign: TextAlign.center,
            style: AppTypography.displayLarge.copyWith(
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: AppSpacing.spaceBase),
          Text(
            data.description,
            textAlign: TextAlign.center,
            style: AppTypography.bodyLarge.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}
