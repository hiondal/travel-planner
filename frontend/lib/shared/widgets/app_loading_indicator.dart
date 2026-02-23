import 'package:flutter/material.dart';

import '../../core/constants/app_colors.dart';

/// 앱 공통 로딩 인디케이터
class AppLoadingIndicator extends StatelessWidget {
  const AppLoadingIndicator({
    super.key,
    this.size = 24,
    this.color,
    this.strokeWidth = 2.5,
  });

  final double size;
  final Color? color;
  final double strokeWidth;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: size,
      height: size,
      child: CircularProgressIndicator(
        strokeWidth: strokeWidth,
        valueColor: AlwaysStoppedAnimation<Color>(
          color ?? AppColors.accentRed,
        ),
      ),
    );
  }
}

/// 전체 화면 로딩 오버레이
class AppFullScreenLoader extends StatelessWidget {
  const AppFullScreenLoader({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(child: AppLoadingIndicator(size: 40)),
    );
  }
}
