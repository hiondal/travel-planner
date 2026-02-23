import 'package:flutter/material.dart';

/// 여행 일정 생성 페이지
/// - 여행명, 기간, 도시 입력
/// - CitySelectSheet (바텀시트) 연동
/// - 첫 일정 등록 시 /schedule/new/permission 이동
///
/// TODO: TripCreateProvider 연결
/// TODO: CitySelectSheet 바텀시트 구현
/// TODO: DatePickerSheet 바텀시트 구현
class TripCreatePage extends StatelessWidget {
  const TripCreatePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('새 여행 만들기')),
      body: const Center(
        child: Text('여행 생성 폼\n(TODO: 구현 예정)'),
      ),
    );
  }
}
