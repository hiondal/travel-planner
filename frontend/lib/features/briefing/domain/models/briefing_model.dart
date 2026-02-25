import '../../../../shared/models/status_level.dart';

/// 브리핑 아이템 모델 (BRIF 서비스)
/// 목록/상세 두 API 응답을 모두 파싱할 수 있도록 nullable 처리.
class Briefing {
  const Briefing({
    required this.briefingId,
    required this.briefingType,
    required this.placeName,
    required this.createdAt,
    required this.isExpired,
    this.placeId,
    this.summary,
    this.overallStatus,
    this.isRead = false,
    this.content,
  });

  final String briefingId;
  final String briefingType; // 'SAFE' | 'CAUTION' | 'EXPIRED'
  final String placeName;
  final DateTime createdAt;
  final bool isExpired;
  final String? placeId;
  final String? summary;
  final StatusLevel? overallStatus;
  final bool isRead;
  final BriefingContent? content;

  factory Briefing.fromJson(Map<String, dynamic> json) {
    // content 내부에서 summary 추출 (상세 응답용)
    final contentMap = json['content'] as Map<String, dynamic>?;
    final summary = contentMap?['summary'] as String? ??
        json['summary'] as String?;

    return Briefing(
      briefingId: json['briefing_id'] as String,
      briefingType: (json['type'] as String? ??
          json['briefing_type'] as String? ?? 'SAFE').toUpperCase(),
      placeName: json['place_name'] as String? ?? '',
      placeId: json['place_id'] as String?,
      createdAt: DateTime.parse(json['created_at'] as String),
      isExpired: json['expired'] as bool? ?? false,
      summary: summary,
      overallStatus: StatusLevel.fromString(
          json['overall_status'] as String? ?? json['type'] as String? ?? 'unknown'),
      isRead: json['is_read'] as bool? ?? false,
      content: contentMap != null
          ? BriefingContent.fromJson(contentMap)
          : null,
    );
  }
}

/// 브리핑 상세 컨텐츠 (상세 API에서만 제공)
class BriefingContent {
  const BriefingContent({
    required this.summary,
    this.businessStatus,
    this.congestion,
    this.weather,
    this.travelTime,
  });

  final String summary;
  final String? businessStatus;
  final String? congestion;
  final String? weather;
  final Map<String, dynamic>? travelTime;

  factory BriefingContent.fromJson(Map<String, dynamic> json) {
    return BriefingContent(
      summary: json['summary'] as String? ?? '',
      businessStatus: json['business_status'] as String?,
      congestion: json['congestion'] as String?,
      weather: json['weather'] as String?,
      travelTime: json['travel_time'] as Map<String, dynamic>?,
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
// 변경: rating nullable 처리 — 백엔드 AlternativeCardDto.rating은 Float (래퍼 타입, nullable)
class Alternative {
  const Alternative({
    required this.alternativeId,
    required this.placeId,
    required this.placeName,
    required this.category,
    this.rating,
    required this.reviewCount,
    required this.reason,
    required this.distanceKm,
    required this.estimatedMinutes,
    this.rank,
    this.imageUrl,
    this.status,
  });

  final String alternativeId;
  final String placeId;
  final String placeName;
  final String category;
  // 변경: non-nullable double → double? (백엔드 nullable 반영)
  final double? rating;
  final int reviewCount;
  final String reason;
  final double distanceKm;
  final int estimatedMinutes;
  // 추가: rank 필드 (int, 1~3) — 백엔드 AlternativeCardDto.rank 반영
  final int? rank;
  final String? imageUrl;
  final StatusLevel? status;

  factory Alternative.fromJson(Map<String, dynamic> json) {
    // Backend sends distance_m (meters), convert to km
    final distanceM = json['distance_m'] as num?;
    final distanceKm = json['distance_km'] as num? ??
        (distanceM != null ? distanceM / 1000.0 : 0);
    // Backend sends walking_minutes inside travel_time
    final travelTime = json['travel_time'] as Map<String, dynamic>?;
    final walkingMin = travelTime?['walking_minutes'] as int?;

    return Alternative(
      alternativeId: json['alternative_id'] as String? ??
          json['alt_id'] as String? ?? '',
      placeId: json['place_id'] as String,
      placeName: json['place_name'] as String? ??
          json['name'] as String? ?? '',
      category: json['category'] as String? ?? '',
      // 변경: nullable — 백엔드가 null을 반환할 수 있음
      rating: (json['rating'] as num?)?.toDouble(),
      reviewCount: json['review_count'] as int? ?? 0,
      reason: json['reason'] as String? ?? '',
      distanceKm: distanceKm.toDouble(),
      estimatedMinutes: json['estimated_minutes'] as int? ?? walkingMin ?? 0,
      rank: json['rank'] as int?,
      imageUrl: json['image_url'] as String?,
      status: json['status'] != null || json['status_label'] != null
          ? StatusLevel.fromString(
              json['status'] as String? ?? json['status_label'] as String? ?? 'unknown')
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
