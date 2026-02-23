import 'package:flutter/material.dart';

/// 브리핑 상세 페이지 (UFR-BRIF-050)
/// 3가지 변형 표시:
/// - 시나리오 1: 안심 브리핑 (모든 항목 정상)
/// - 시나리오 2: 주의 브리핑 (주의/위험 항목 존재 → 대안 보기 CTA)
/// - 시나리오 3: 만료 브리핑 (여행 종료 후)
///
/// Push 알림 딥링크 진입 지원: /briefing/:briefingId
///
/// TODO: BriefingDetailProvider 연결
/// TODO: BriefingCard 위젯 구현
/// TODO: BriefingDetailRow 위젯 구현 (영업상태/혼잡도/날씨/이동시간)
class BriefingDetailPage extends StatelessWidget {
  const BriefingDetailPage({super.key, required this.briefingId});

  final String briefingId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('브리핑 상세')),
      body: Center(
        child: Text(
          '브리핑 상세\nbriefingId: $briefingId\n(TODO: 구현 예정)',
        ),
      ),
    );
  }
}
