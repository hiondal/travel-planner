import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:travel_planner/app.dart';

void main() {
  testWidgets('App smoke test - renders without crash', (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(child: TravelPlannerApp()),
    );
    // 앱이 크래시 없이 렌더링되는지 확인
    expect(find.byType(TravelPlannerApp), findsOneWidget);
  });
}
