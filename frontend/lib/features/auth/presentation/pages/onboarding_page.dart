import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/routing/app_routes.dart';

/// 온보딩 페이지
/// - Step 1: 상태 배지 소개 (UFR-SCHD-005)
/// - Step 2: 출발 전 브리핑 소개
/// - Step 3: 대안 카드 소개
/// 최초 1회 표시 / 설정에서 재진입 가능 (?step=1 쿼리 파라미터)
///
/// TODO: 각 단계별 일러스트 및 설명 위젯 구현
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
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeInOut,
      );
    } else {
      _onComplete();
    }
  }

  void _onComplete() {
    // TODO: 온보딩 완료 플래그 저장 (SharedPreferences 또는 secure_storage)
    context.goNamed(AppRoutes.tripListName);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: PageView(
                controller: _pageController,
                onPageChanged: (index) {
                  setState(() => _currentStep = index + 1);
                },
                children: const [
                  // TODO: 각 단계별 OnboardingStepCard 위젯으로 교체
                  _OnboardingStepPlaceholder(
                    step: 1,
                    title: '실시간 상태 배지',
                    description: '장소마다 정상/주의/위험 상태를 실시간으로 확인하세요.',
                  ),
                  _OnboardingStepPlaceholder(
                    step: 2,
                    title: '출발 전 브리핑',
                    description: 'AI가 오늘 일정을 분석하여 출발 전 상황을 브리핑합니다.',
                  ),
                  _OnboardingStepPlaceholder(
                    step: 3,
                    title: '스마트 대안 카드',
                    description: '문제가 생기면 즉시 최적의 대안 장소를 추천합니다.',
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(16),
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
    );
  }
}

/// 온보딩 단계 플레이스홀더 (실제 구현 전 임시)
class _OnboardingStepPlaceholder extends StatelessWidget {
  const _OnboardingStepPlaceholder({
    required this.step,
    required this.title,
    required this.description,
  });

  final int step;
  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(32),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            '$step / 3',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 24),
          Text(
            title,
            style: Theme.of(context).textTheme.displaySmall,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
          Text(
            description,
            style: Theme.of(context).textTheme.bodyMedium,
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
