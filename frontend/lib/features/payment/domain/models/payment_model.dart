/// 구독 플랜 모델 (PAY 서비스)
class SubscriptionPlan {
  const SubscriptionPlan({
    required this.planId,
    required this.planName,
    required this.price,
    required this.currency,
    required this.billingPeriod,
    required this.features,
  });

  final String planId;
  final String planName;
  final int price;
  final String currency;
  final String billingPeriod; // 'monthly' | 'trip'
  final List<String> features;

  factory SubscriptionPlan.fromJson(Map<String, dynamic> json) {
    return SubscriptionPlan(
      planId: json['plan_id'] as String,
      planName: json['plan_name'] as String,
      price: json['price'] as int,
      currency: json['currency'] as String? ?? 'KRW',
      billingPeriod: json['billing_period'] as String? ?? 'monthly',
      features: (json['features'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList() ??
          [],
    );
  }
}

/// 구독 플랜 목록 응답
class SubscriptionPlanListResponse {
  const SubscriptionPlanListResponse({required this.plans});

  final List<SubscriptionPlan> plans;

  factory SubscriptionPlanListResponse.fromJson(Map<String, dynamic> json) {
    final list = json['plans'] as List<dynamic>? ?? [];
    return SubscriptionPlanListResponse(
      plans: list
          .map((e) => SubscriptionPlan.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

/// 구독 상태 모델
class SubscriptionStatus {
  const SubscriptionStatus({
    required this.tier,
    this.currentPlan,
    this.expiresAt,
    this.autoRenew = false,
  });

  final String tier;
  final String? currentPlan;
  final DateTime? expiresAt;
  final bool autoRenew;

  bool get isActive => tier != 'FREE';

  factory SubscriptionStatus.fromJson(Map<String, dynamic> json) {
    return SubscriptionStatus(
      tier: json['tier'] as String? ?? 'FREE',
      currentPlan: json['current_plan'] as String?,
      expiresAt: json['expires_at'] != null
          ? DateTime.parse(json['expires_at'] as String)
          : null,
      autoRenew: json['auto_renew'] as bool? ?? false,
    );
  }
}

/// 구독 구매 요청
class PurchaseRequest {
  const PurchaseRequest({
    required this.planId,
    required this.receiptData,
    required this.platform,
  });

  final String planId;
  final String receiptData;
  final String platform; // 'ios' | 'android'

  Map<String, dynamic> toJson() => {
        'plan_id': planId,
        'receipt_data': receiptData,
        'platform': platform,
      };
}
