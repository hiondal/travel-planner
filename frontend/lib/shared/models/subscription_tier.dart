/// 구독 티어 열거형
enum SubscriptionTier {
  /// 무료 — 기본 기능만 제공
  free,

  /// 여행 패스 — 1회 여행 단위 구독
  tripPass,

  /// 프로 — 월 구독
  pro;

  /// 프리미엄 여부 (무료 아님)
  bool get isPremium => this != SubscriptionTier.free;

  /// 표시 이름
  String get displayName => switch (this) {
        SubscriptionTier.free => 'Free',
        SubscriptionTier.tripPass => 'Trip Pass',
        SubscriptionTier.pro => 'Pro',
      };

  /// JSON 값에서 변환
  static SubscriptionTier fromString(String value) => switch (value) {
        'TRIP_PASS' || 'trip_pass' || 'tripPass' => SubscriptionTier.tripPass,
        'PRO' || 'pro' => SubscriptionTier.pro,
        _ => SubscriptionTier.free,
      };
}
