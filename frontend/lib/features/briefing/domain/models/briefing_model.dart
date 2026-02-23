import '../../../../shared/models/status_level.dart';

/// 브리핑 아이템 모델 (BRIF 서비스)
class Briefing {
  const Briefing({
    required this.briefingId,
    required this.tripId,
    required this.briefingType,
    required this.summary,
    required this.overallStatus,
    required this.createdAt,
    required this.isRead,
    this.expiresAt,
    this.details,
  });

  final String briefingId;
  final String tripId;
  final String briefingType; // 'safe' | 'caution' | 'expired'
  final String summary;
  final StatusLevel overallStatus;
  final DateTime createdAt;
  final bool isRead;
  final DateTime? expiresAt;
  final BriefingDetails? details;

  bool get isExpired =>
      expiresAt != null && DateTime.now().isAfter(expiresAt!);

  factory Briefing.fromJson(Map<String, dynamic> json) {
    return Briefing(
      briefingId: json['briefing_id'] as String,
      tripId: json['trip_id'] as String,
      briefingType: json['briefing_type'] as String? ?? 'safe',
      summary: json['summary'] as String,
      overallStatus:
          StatusLevel.fromString(json['overall_status'] as String? ?? 'unknown'),
      createdAt: DateTime.parse(json['created_at'] as String),
      isRead: json['is_read'] as bool? ?? false,
      expiresAt: json['expires_at'] != null
          ? DateTime.parse(json['expires_at'] as String)
          : null,
      details: json['details'] != null
          ? BriefingDetails.fromJson(json['details'] as Map<String, dynamic>)
          : null,
    );
  }
}

/// 브리핑 목록 응답
class BriefingListResponse {
  const BriefingListResponse({required this.briefings});

  final List<Briefing> briefings;

  factory BriefingListResponse.fromJson(Map<String, dynamic> json) {
    final list = json['briefings'] as List<dynamic>? ?? [];
    return BriefingListResponse(
      briefings: list
          .map((e) => Briefing.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

/// 브리핑 세부 상태 (4개 항목)
class BriefingDetails {
  const BriefingDetails({
    required this.weather,
    required this.crowding,
    required this.businessHours,
    required this.traffic,
  });

  final StatusItem weather;
  final StatusItem crowding;
  final StatusItem businessHours;
  final StatusItem traffic;

  factory BriefingDetails.fromJson(Map<String, dynamic> json) {
    return BriefingDetails(
      weather: StatusItem.fromJson(
          json['weather'] as Map<String, dynamic>? ?? {}),
      crowding: StatusItem.fromJson(
          json['crowding'] as Map<String, dynamic>? ?? {}),
      businessHours: StatusItem.fromJson(
          json['business_hours'] as Map<String, dynamic>? ?? {}),
      traffic: StatusItem.fromJson(
          json['traffic'] as Map<String, dynamic>? ?? {}),
    );
  }
}

/// 개별 상태 항목
class StatusItem {
  const StatusItem({
    required this.status,
    required this.message,
  });

  final StatusLevel status;
  final String message;

  factory StatusItem.fromJson(Map<String, dynamic> json) {
    return StatusItem(
      status: StatusLevel.fromString(json['status'] as String? ?? 'unknown'),
      message: json['message'] as String? ?? '',
    );
  }
}

/// 대안 장소 모델 (ALTN 서비스)
class Alternative {
  const Alternative({
    required this.alternativeId,
    required this.placeId,
    required this.placeName,
    required this.category,
    required this.rating,
    required this.reviewCount,
    required this.reason,
    required this.distanceKm,
    required this.estimatedMinutes,
    this.imageUrl,
    this.status,
  });

  final String alternativeId;
  final String placeId;
  final String placeName;
  final String category;
  final double rating;
  final int reviewCount;
  final String reason;
  final double distanceKm;
  final int estimatedMinutes;
  final String? imageUrl;
  final StatusLevel? status;

  factory Alternative.fromJson(Map<String, dynamic> json) {
    return Alternative(
      alternativeId: json['alternative_id'] as String,
      placeId: json['place_id'] as String,
      placeName: json['place_name'] as String,
      category: json['category'] as String,
      rating: (json['rating'] as num? ?? 0).toDouble(),
      reviewCount: json['review_count'] as int? ?? 0,
      reason: json['reason'] as String? ?? '',
      distanceKm: (json['distance_km'] as num? ?? 0).toDouble(),
      estimatedMinutes: json['estimated_minutes'] as int? ?? 0,
      imageUrl: json['image_url'] as String?,
      status: json['status'] != null
          ? StatusLevel.fromString(json['status'] as String)
          : null,
    );
  }
}

/// 대안 목록 응답
class AlternativeListResponse {
  const AlternativeListResponse({required this.alternatives});

  final List<Alternative> alternatives;

  factory AlternativeListResponse.fromJson(Map<String, dynamic> json) {
    final list = json['alternatives'] as List<dynamic>? ?? [];
    return AlternativeListResponse(
      alternatives: list
          .map((e) => Alternative.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
