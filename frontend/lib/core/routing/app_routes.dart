/// ia.md 기반 라우트 경로/이름 상수
/// go_router에서 사용하는 path와 name을 한 곳에서 관리한다.
abstract final class AppRoutes {
  AppRoutes._();

  // ---------------------------------------------------------------------------
  // 인증/온보딩 라우트
  // ---------------------------------------------------------------------------

  static const String splashPath = '/splash';
  static const String splashName = 'splash';

  static const String loginPath = '/auth/login';
  static const String loginName = 'login';

  static const String onboardingPath = '/auth/onboarding';
  static const String onboardingName = 'onboarding';

  // ---------------------------------------------------------------------------
  // TAB 1: 일정 라우트
  // ---------------------------------------------------------------------------

  static const String tripListPath = '/schedule';
  static const String tripListName = 'tripList';

  static const String tripCreatePath = '/schedule/new';
  static const String tripCreateName = 'tripCreate';

  static const String permissionPath = '/schedule/new/permission';
  static const String permissionName = 'permission';

  static const String scheduleDetailPath = '/schedule/:tripId';
  static const String scheduleDetailName = 'scheduleDetail';

  static const String placeSearchPath = '/schedule/:tripId/place/search';
  static const String placeSearchName = 'placeSearch';

  static const String placeTimePickerPath =
      '/schedule/:tripId/place/search/time';
  static const String placeTimePickerName = 'placeTimePicker';

  static const String scheduleChangeResultPath = '/schedule/:tripId/result';
  static const String scheduleChangeResultName = 'scheduleChangeResult';

  // ---------------------------------------------------------------------------
  // TAB 2: 브리핑 라우트
  // ---------------------------------------------------------------------------

  static const String briefingListPath = '/briefing';
  static const String briefingListName = 'briefingList';

  static const String briefingDetailPath = '/briefing/:briefingId';
  static const String briefingDetailName = 'briefingDetail';

  static const String alternativeCardPath =
      '/briefing/alternative/:briefingId';
  static const String alternativeCardName = 'alternativeCard';

  static const String paywallPath = '/briefing/paywall';
  static const String paywallName = 'paywall';

  // ---------------------------------------------------------------------------
  // TAB 3: 마이페이지 라우트
  // ---------------------------------------------------------------------------

  static const String profilePath = '/profile';
  static const String profileName = 'profile';

  static const String subscriptionPath = '/profile/subscription';
  static const String subscriptionName = 'subscription';

  static const String notificationSettingsPath = '/profile/notifications';
  static const String notificationSettingsName = 'notificationSettings';

  static const String locationConsentPath = '/profile/location';
  static const String locationConsentName = 'locationConsent';

  // ---------------------------------------------------------------------------
  // 결제 라우트 (탭 외부 전역)
  // ---------------------------------------------------------------------------

  static const String paymentCheckoutPath = '/payment/checkout';
  static const String paymentCheckoutName = 'paymentCheckout';

  static const String paymentSuccessPath = '/payment/success';
  static const String paymentSuccessName = 'paymentSuccess';

  // ---------------------------------------------------------------------------
  // 헬퍼: path params 치환
  // ---------------------------------------------------------------------------

  /// /schedule/:tripId → /schedule/abc123
  static String scheduleDetail(String tripId) => '/schedule/$tripId';

  /// /schedule/:tripId/place/search → /schedule/abc123/place/search
  static String placeSearch(String tripId) =>
      '/schedule/$tripId/place/search';

  /// /schedule/:tripId/place/search/time
  static String placeTimePicker(String tripId) =>
      '/schedule/$tripId/place/search/time';

  /// /schedule/:tripId/result
  static String scheduleChangeResult(String tripId) =>
      '/schedule/$tripId/result';

  /// /briefing/:briefingId
  static String briefingDetail(String briefingId) =>
      '/briefing/$briefingId';

  /// /briefing/alternative/:briefingId
  static String alternativeCard(String briefingId) =>
      '/briefing/alternative/$briefingId';
}
