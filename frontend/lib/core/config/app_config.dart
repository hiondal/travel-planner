/// 앱 환경 설정
/// 환경별(dev/mock/staging/prod) 분기를 담당한다.
class AppConfig {
  AppConfig._();

  /// 현재 실행 환경
  static AppEnvironment _environment = AppEnvironment.dev;

  static AppEnvironment get environment => _environment;

  /// 환경을 설정한다. main.dart에서 앱 시작 시 1회 호출한다.
  static void setEnvironment(AppEnvironment env) {
    _environment = env;
  }

  // 변경: 단일 apiBaseUrl → 서비스별 URL 맵으로 전환
  // staging/prod는 API Gateway 단일 URL 사용 예정 (추후 구현)

  /// Auth 서비스 기본 URL (port 8081)
  static String get authBaseUrl => _serviceBaseUrl(ApiService.auth);

  /// Schedule 서비스 기본 URL (port 8082)
  static String get scheduleBaseUrl => _serviceBaseUrl(ApiService.schedule);

  /// Place 서비스 기본 URL (port 8083)
  static String get placeBaseUrl => _serviceBaseUrl(ApiService.place);

  /// Monitor 서비스 기본 URL (port 8084)
  static String get monitorBaseUrl => _serviceBaseUrl(ApiService.monitor);

  /// Briefing 서비스 기본 URL (port 8085)
  static String get briefingBaseUrl => _serviceBaseUrl(ApiService.briefing);

  /// Alternative 서비스 기본 URL (port 8086)
  static String get alternativeBaseUrl => _serviceBaseUrl(ApiService.alternative);

  /// Payment 서비스 기본 URL (port 8087)
  static String get paymentBaseUrl => _serviceBaseUrl(ApiService.payment);

  /// 서비스 식별자로 기본 URL을 반환한다.
  static String serviceUrl(ApiService service) => _serviceBaseUrl(service);

  /// 개발 환경 여부
  static bool get isDev =>
      _environment == AppEnvironment.dev ||
      _environment == AppEnvironment.mock;

  /// 프로덕션 환경 여부
  static bool get isProd => _environment == AppEnvironment.prod;

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

  // 변경: 환경별 서비스 URL 분기 내부 메서드
  // dev: 각 Spring Boot 서비스 포트로 직접 연결
  // mock: Prism Mock 서버 단일 포트 연결 (4010)
  // staging/prod: API Gateway 단일 URL (추후 구현 예정)
  static String _serviceBaseUrl(ApiService service) {
    switch (_environment) {
      case AppEnvironment.dev:
        return _devServiceUrls[service]!;
      case AppEnvironment.mock:
        return _mockServiceUrls[service]!;
      case AppEnvironment.staging:
        return 'https://api-staging.travel-planner.app/api/v1';
      case AppEnvironment.prod:
        return 'https://api.travel-planner.app/api/v1';
    }
  }

  // 변경: dev 환경 서비스별 실제 백엔드 URL
  // PLCE baseUrl은 /api/v1 까지만 설정 — datasource가 /places/search 경로를 가지므로
  // /api/v1/places 로 설정하면 /api/v1/places/places/search 이중 경로 발생
  // PAY baseUrl도 동일한 이유로 /api/v1 까지만 설정
  static const Map<ApiService, String> _devServiceUrls = {
    ApiService.auth: 'http://localhost:8081/api/v1',
    ApiService.schedule: 'http://localhost:8082/api/v1',
    ApiService.place: 'http://localhost:8083/api/v1',
    ApiService.monitor: 'http://localhost:8084/api/v1',
    ApiService.briefing: 'http://localhost:8085/api/v1',
    ApiService.alternative: 'http://localhost:8086/api/v1',
    ApiService.payment: 'http://localhost:8087/api/v1',
  };

  // 변경: mock 환경 — Prism Mock 서버 단일 URL (Mock으로 쉽게 복귀 가능하도록 유지)
  static const Map<ApiService, String> _mockServiceUrls = {
    ApiService.auth: 'http://localhost:4010',
    ApiService.schedule: 'http://localhost:4010',
    ApiService.place: 'http://localhost:4010',
    ApiService.monitor: 'http://localhost:4010',
    ApiService.briefing: 'http://localhost:4010',
    ApiService.alternative: 'http://localhost:4010',
    ApiService.payment: 'http://localhost:4010',
  };
}

/// 서비스 식별자
/// 각 마이크로서비스를 구분하는 열거형
enum ApiService {
  auth,
  schedule,
  place,
  monitor,
  briefing,
  alternative,
  payment,
}

/// 앱 실행 환경 열거형
// 변경: mock 환경 추가 — Prism Mock 서버 연결 시 사용 (dev와 분리)
enum AppEnvironment {
  dev,
  mock,
  staging,
  prod,
}
