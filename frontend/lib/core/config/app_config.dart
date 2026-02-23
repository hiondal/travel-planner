/// 앱 환경 설정
/// 환경별(dev/staging/prod) 분기를 담당한다.
class AppConfig {
  AppConfig._();

  /// 현재 실행 환경
  static AppEnvironment _environment = AppEnvironment.dev;

  static AppEnvironment get environment => _environment;

  /// 환경을 설정한다. main.dart에서 앱 시작 시 1회 호출한다.
  static void setEnvironment(AppEnvironment env) {
    _environment = env;
  }

  /// API 베이스 URL
  /// - dev: Prism Mock 서버 (http://localhost:4010)
  /// - staging: 스테이징 서버
  /// - prod: 프로덕션 서버
  static String get apiBaseUrl {
    switch (_environment) {
      case AppEnvironment.dev:
        return 'http://localhost:4010';
      case AppEnvironment.staging:
        return 'https://api-staging.travel-planner.app';
      case AppEnvironment.prod:
        return 'https://api.travel-planner.app';
    }
  }

  /// HTTP 연결 타임아웃 (ms)
  static const int connectTimeout = 10000;

  /// HTTP 수신 타임아웃 (ms)
  static const int receiveTimeout = 15000;

  /// 앱 이름
  static const String appName = 'travel-planner';

  /// 딥링크 스킴
  static const String deepLinkScheme = 'travelplanner';

  /// 딥링크 호스트
  static const String deepLinkHost = 'app.travel-planner.com';

  /// 개발 환경 여부
  static bool get isDev => _environment == AppEnvironment.dev;

  /// 프로덕션 환경 여부
  static bool get isProd => _environment == AppEnvironment.prod;
}

/// 앱 실행 환경 열거형
enum AppEnvironment {
  dev,
  staging,
  prod,
}
