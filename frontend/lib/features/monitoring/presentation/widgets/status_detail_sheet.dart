import 'package:flutter/material.dart';

import '../../../../core/constants/app_spacing.dart';
import '../../../../shared/models/status_level.dart';
import '../../../../shared/widgets/status_badge.dart';

/// 상태 상세 바텀시트 (UFR-MNTR-040)
/// 라우트 없이 오버레이 방식으로 표시한다.
///
/// 사용 예시:
/// ```dart
/// StatusDetailSheet.show(context, placeId: 'place-123');
/// ```
///
/// TODO: MonitorStatusProvider 연결
/// TODO: 실제 영업상태/혼잡도/날씨/이동시간 데이터 표시
/// TODO: "대안 보기" 버튼 → AlternativeCardPage 이동 (주의/위험 상태)
class StatusDetailSheet extends StatelessWidget {
  const StatusDetailSheet({
    super.key,
    required this.placeId,
    required this.placeName,
    required this.level,
  });

  final String placeId;
  final String placeName;
  final StatusLevel level;

  /// 바텀시트 표시 헬퍼
  static Future<void> show(
    BuildContext context, {
    required String placeId,
    required String placeName,
    required StatusLevel level,
  }) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      enableDrag: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppSpacing.radiusBottomSheet),
        ),
      ),
      builder: (_) => StatusDetailSheet(
        placeId: placeId,
        placeName: placeName,
        level: level,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.5,
      minChildSize: 0.4,
      maxChildSize: 0.9,
      expand: false,
      builder: (context, scrollController) {
        return SingleChildScrollView(
          controller: scrollController,
          child: Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.spaceBase,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 핸들바
                Center(
                  child: Container(
                    margin: const EdgeInsets.only(top: 12, bottom: 20),
                    width: AppSpacing.bottomSheetHandleWidth,
                    height: AppSpacing.bottomSheetHandleHeight,
                    decoration: BoxDecoration(
                      color: const Color(0xFF555555),
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
                // 장소명 + 상태 배지
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Text(
                        placeName,
                        style: Theme.of(context).textTheme.displaySmall,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    const SizedBox(width: 8),
                    StatusBadge(level: level),
                  ],
                ),
                const SizedBox(height: 20),
                // TODO: 상세 항목 (영업상태, 혼잡도, 날씨, 이동시간)
                Text(
                  '상태 상세 정보\n(TODO: 구현 예정)',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
                const SizedBox(height: 24),
                // 대안 보기 버튼 (주의/위험 시 표시)
                if (level == StatusLevel.caution ||
                    level == StatusLevel.danger)
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      onPressed: () {
                        Navigator.pop(context);
                        // TODO: AlternativeCardPage 이동
                      },
                      child: const Text('대안 보기'),
                    ),
                  ),
                const SizedBox(height: 32),
              ],
            ),
          ),
        );
      },
    );
  }
}
