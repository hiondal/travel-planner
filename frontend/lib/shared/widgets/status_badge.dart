import 'package:flutter/material.dart';

import '../../core/constants/app_spacing.dart';
import '../../core/constants/app_typography.dart';
import '../models/status_level.dart';

/// 상태 배지 위젯
///
/// 여행 일정의 현재 상태(정상/주의/위험/미확인)를 한눈에 전달한다.
/// WCAG 2.1 접근성: 색상 + 아이콘 조합 필수 적용 (색약 사용자 지원).
///
/// style-guide.md 8-1 StatusBadge 스펙 기반
///
/// 사용 예시:
/// ```dart
/// StatusBadge(level: StatusLevel.caution)
/// StatusBadge(level: StatusLevel.danger, onTap: () { /* 상세 시트 열기 */ })
/// ```
class StatusBadge extends StatefulWidget {
  const StatusBadge({
    super.key,
    required this.level,
    this.onTap,
    this.animate = false,
  });

  /// 배지 상태 레벨
  final StatusLevel level;

  /// 탭 콜백 (null이면 비활성)
  final VoidCallback? onTap;

  /// 상태 변경 애니메이션 활성화 여부
  final bool animate;

  @override
  State<StatusBadge> createState() => _StatusBadgeState();
}

class _StatusBadgeState extends State<StatusBadge>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 400),
    );

    // style-guide.md 9-7: 배지 상태 변경 애니메이션
    // 1.0 → 0.8 → 1.2 → 1.0 (spring curve, 400ms)
    _scaleAnimation = TweenSequence<double>([
      TweenSequenceItem(
        tween: Tween(begin: 1.0, end: 0.8)
            .chain(CurveTween(curve: Curves.easeOut)),
        weight: 25,
      ),
      TweenSequenceItem(
        tween: Tween(begin: 0.8, end: 1.2)
            .chain(CurveTween(curve: Curves.easeInOut)),
        weight: 50,
      ),
      TweenSequenceItem(
        tween: Tween(begin: 1.2, end: 1.0)
            .chain(CurveTween(curve: Curves.easeIn)),
        weight: 25,
      ),
    ]).animate(_controller);
  }

  @override
  void didUpdateWidget(StatusBadge oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.animate && oldWidget.level != widget.level) {
      _controller.forward(from: 0);
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final level = widget.level;
    final badge = _buildBadge(level);

    // 애니메이션 적용
    final animatedBadge = widget.animate
        ? ScaleTransition(scale: _scaleAnimation, child: badge)
        : badge;

    // Semantics: 스크린 리더 접근성
    return Semantics(
      label: level.semanticsLabel,
      liveRegion: widget.animate, // 상태 변경 시 스크린 리더 알림
      button: widget.onTap != null,
      child: widget.onTap != null
          ? GestureDetector(
              onTap: widget.onTap,
              child: animatedBadge,
            )
          : animatedBadge,
    );
  }

  Widget _buildBadge(StatusLevel level) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 250),
      curve: Curves.easeInOut,
      constraints: const BoxConstraints(
        minWidth: 64,
        minHeight: AppSpacing.statusBadgeHeight,
      ),
      padding: const EdgeInsets.symmetric(
        horizontal: 10,
        vertical: 4,
      ),
      decoration: BoxDecoration(
        color: level.backgroundColor,
        borderRadius: BorderRadius.circular(AppSpacing.radiusBadge),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 아이콘 (색약 접근성: 색상만으로 정보 전달 금지)
          Icon(
            level.icon,
            color: level.color,
            size: 16,
          ),
          const SizedBox(width: AppSpacing.spaceXs),
          // 레이블 텍스트
          Text(
            level.label,
            style: AppTypography.labelSmall.copyWith(
              color: level.color,
            ),
          ),
        ],
      ),
    );
  }
}
