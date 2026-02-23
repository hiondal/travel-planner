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
      tripName: json['trip_name'] as String,
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
        'trip_name': tripName,
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
    required this.category,
    required this.scheduledAt,
    required this.durationMinutes,
    required this.status,
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
    return ScheduleItem(
      scheduleItemId: json['schedule_item_id'] as String,
      placeId: json['place_id'] as String,
      placeName: json['place_name'] as String,
      category: json['category'] as String,
      scheduledAt: DateTime.parse(json['scheduled_at'] as String),
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
    required this.targetDate,
    required this.items,
  });

  final String tripId;
  final DateTime targetDate;
  final List<ScheduleItem> items;

  factory ScheduleResponse.fromJson(Map<String, dynamic> json) {
    final list = json['items'] as List<dynamic>? ?? [];
    return ScheduleResponse(
      tripId: json['trip_id'] as String,
      targetDate: DateTime.parse(json['target_date'] as String),
      items: list
          .map((e) => ScheduleItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

/// 장소 추가 요청 모델
class AddScheduleItemRequest {
  const AddScheduleItemRequest({
    required this.placeId,
    required this.scheduledAt,
    required this.durationMinutes,
  });

  final String placeId;
  final DateTime scheduledAt;
  final int durationMinutes;

  Map<String, dynamic> toJson() => {
        'place_id': placeId,
        'scheduled_at': scheduledAt.toIso8601String(),
        'duration_minutes': durationMinutes,
      };
}
