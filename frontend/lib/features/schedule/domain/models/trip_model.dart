import '../../../../shared/models/status_level.dart';

/// 여행 목록 아이템 모델 (SCHD 서비스)
class Trip {
  const Trip({
    required this.tripId,
    required this.tripName,
    required this.city,
    required this.startDate,
    required this.endDate,
    required this.status,
    this.thumbnailUrl,
    this.totalPlaces = 0,
  });

  final String tripId;
  final String tripName;
  final String city;
  final DateTime startDate;
  final DateTime endDate;
  final StatusLevel status;
  final String? thumbnailUrl;
  final int totalPlaces;

  factory Trip.fromJson(Map<String, dynamic> json) {
    return Trip(
      tripId: json['trip_id'] as String,
      tripName: (json['trip_name'] ?? json['name']) as String,
      city: json['city'] as String,
      startDate: DateTime.parse(json['start_date'] as String),
      endDate: DateTime.parse(json['end_date'] as String),
      status: StatusLevel.fromString(json['status'] as String? ?? 'unknown'),
      thumbnailUrl: json['thumbnail_url'] as String?,
      totalPlaces: json['total_places'] as int? ?? 0,
    );
  }
}

/// 여행 목록 응답 모델
class TripListResponse {
  const TripListResponse({required this.trips});

  final List<Trip> trips;

  factory TripListResponse.fromJson(Map<String, dynamic> json) {
    final list = json['trips'] as List<dynamic>? ?? [];
    return TripListResponse(
      trips: list
          .map((e) => Trip.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

/// 여행 생성 요청 모델
class TripCreateRequest {
  const TripCreateRequest({
    required this.tripName,
    required this.city,
    required this.startDate,
    required this.endDate,
  });

  final String tripName;
  final String city;
  final DateTime startDate;
  final DateTime endDate;

  Map<String, dynamic> toJson() => {
        'name': tripName,
        'city': city,
        'start_date': startDate.toIso8601String().substring(0, 10),
        'end_date': endDate.toIso8601String().substring(0, 10),
      };
}

/// 일정 아이템 모델 (타임라인 항목)
class ScheduleItem {
  const ScheduleItem({
    required this.scheduleItemId,
    required this.placeId,
    required this.placeName,
    this.category = '',
    required this.scheduledAt,
    this.durationMinutes = 60,
    this.status = StatusLevel.unknown,
    this.thumbnailUrl,
    this.address,
    this.lat,
    this.lng,
  });

  final String scheduleItemId;
  final String placeId;
  final String placeName;
  final String category;
  final DateTime scheduledAt;
  final int durationMinutes;
  final StatusLevel status;
  final String? thumbnailUrl;
  final String? address;
  final double? lat;
  final double? lng;

  factory ScheduleItem.fromJson(Map<String, dynamic> json) {
    // 백엔드 ScheduleItemResponse/ScheduleItemSummary 필드:
    // visit_datetime, order, outside_business_hours
    // category / duration_minutes / status 는 응답에 없으므로 기본값 사용
    final visitDatetimeStr =
        (json['visit_datetime'] ?? json['scheduled_at']) as String?;
    return ScheduleItem(
      scheduleItemId: json['schedule_item_id'] as String,
      placeId: json['place_id'] as String,
      placeName: json['place_name'] as String,
      category: json['category'] as String? ?? '',
      scheduledAt: visitDatetimeStr != null
          ? DateTime.parse(visitDatetimeStr)
          : DateTime.now(),
      durationMinutes: json['duration_minutes'] as int? ?? 60,
      status: StatusLevel.fromString(json['status'] as String? ?? 'unknown'),
      thumbnailUrl: json['thumbnail_url'] as String?,
      address: json['address'] as String?,
      lat: (json['lat'] as num?)?.toDouble(),
      lng: (json['lng'] as num?)?.toDouble(),
    );
  }
}

/// 일정표 응답 모델
class ScheduleResponse {
  const ScheduleResponse({
    required this.tripId,
    required this.scheduleItems,
  });

  final String tripId;
  final List<ScheduleItem> scheduleItems;

  factory ScheduleResponse.fromJson(Map<String, dynamic> json) {
    // 백엔드 ScheduleResponse 필드: trip_id, name, city, schedule_items
    final list =
        (json['schedule_items'] ?? json['items']) as List<dynamic>? ?? [];
    return ScheduleResponse(
      tripId: json['trip_id'] as String,
      scheduleItems: list
          .map((e) => ScheduleItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

/// 영업시간 외 경고 예외
class BusinessHoursWarningException implements Exception {
  const BusinessHoursWarningException({
    required this.message,
    required this.businessHours,
  });

  final String message;
  final String businessHours;

  @override
  String toString() => message;
}

/// 장소 추가 요청 모델
class AddScheduleItemRequest {
  const AddScheduleItemRequest({
    required this.placeId,
    required this.scheduledAt,
    required this.durationMinutes,
    this.force = false,
  });

  final String placeId;
  final DateTime scheduledAt;
  final int durationMinutes;
  final bool force;

  Map<String, dynamic> toJson() => {
        'place_id': placeId,
        'visit_datetime': scheduledAt.toIso8601String(),
        'timezone': _formatTimezone(scheduledAt),
        'force': force,
      };

  static String _formatTimezone(DateTime dt) {
    final offset = dt.timeZoneOffset;
    final h = offset.inHours.abs().toString().padLeft(2, '0');
    final m = (offset.inMinutes.abs() % 60).toString().padLeft(2, '0');
    return offset.isNegative ? '-$h:$m' : '+$h:$m';
  }
}
