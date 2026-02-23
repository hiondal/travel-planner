import 'package:flutter/material.dart';

/// 장소 검색 페이지
/// - 텍스트 검색 → 장소 목록 표시
/// - 장소 선택 → PlaceTimePickerPage 이동
/// - PlaceDetailSheet 바텀시트 (Should)
///
/// TODO: PlaceSearchProvider 연결
/// TODO: PlaceSearchBar 위젯 구현
/// TODO: PlaceSearchResultTile 위젯 구현
class PlaceSearchPage extends StatelessWidget {
  const PlaceSearchPage({super.key, required this.tripId});

  final String tripId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('장소 검색')),
      body: Center(
        child: Text('장소 검색\ntripId: $tripId\n(TODO: 구현 예정)'),
      ),
    );
  }
}
