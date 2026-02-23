import 'package:flutter/material.dart';

/// 방문 시간 지정 페이지
/// - 장소 방문 시작 시간 / 체류 시간 선택
///
/// TODO: 시간 선택 UI 구현
/// TODO: 일정에 장소 추가 처리
class PlaceTimePickerPage extends StatelessWidget {
  const PlaceTimePickerPage({
    super.key,
    required this.tripId,
    this.placeId,
  });

  final String tripId;
  final String? placeId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('시간 지정')),
      body: Center(
        child: Text(
          '방문 시간 지정\ntripId: $tripId\nplaceId: $placeId\n(TODO: 구현 예정)',
        ),
      ),
    );
  }
}
