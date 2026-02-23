import 'package:flutter/material.dart';

/// 일정표 페이지 (일별 타임라인)
/// - ScheduleTimeline 위젯 표시
/// - 장소 카드 탭 시 StatusDetailSheet 오버레이
/// - 장소 스와이프 삭제 / 드래그 앤 드롭 순서 변경
///
/// TODO: ScheduleDetailProvider 연결
/// TODO: ScheduleTimeline 위젯 구현
/// TODO: StatusDetailSheet 바텀시트 연결
class ScheduleDetailPage extends StatelessWidget {
  const ScheduleDetailPage({super.key, required this.tripId});

  final String tripId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('일정표: $tripId')),
      body: Center(
        child: Text('일정 타임라인\ntripId: $tripId\n(TODO: 구현 예정)'),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // TODO: 장소 추가 → PlaceSearchPage 이동
        },
        child: const Icon(Icons.add_location_alt),
      ),
    );
  }
}
