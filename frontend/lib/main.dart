import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app.dart';
import 'core/config/app_config.dart';

/// 앱 진입점
/// - Firebase 초기화
/// - ProviderScope (Riverpod) 래핑
/// - 세로 방향 고정 (모바일)
/// - 시스템 UI 오버레이 설정 (다크 테마)
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 환경 설정 (빌드 flavor에 따라 변경)
  AppConfig.setEnvironment(AppEnvironment.dev);

  // 한국어 날짜 포맷 초기화
  await initializeDateFormatting('ko');

  // Firebase 초기화 (웹에서는 firebase_options.dart 미설정 시 스킵)
  try {
    await Firebase.initializeApp();
  } catch (e) {
    if (kDebugMode) {
      debugPrint('[main] Firebase init skipped: $e');
    }
  }

  // 세로 방향 고정
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  // 시스템 UI 오버레이 — 다크 테마에 맞춰 투명 상태바
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Colors.transparent,
      systemNavigationBarDividerColor: Colors.transparent,
      systemNavigationBarIconBrightness: Brightness.light,
    ),
  );

  runApp(
    // Riverpod 최상위 래핑
    const ProviderScope(
      child: TravelPlannerApp(),
    ),
  );
}
