import '../../../../shared/models/status_level.dart';

/// 장소 상태 상세 모델 (MNTR 서비스)
class PlaceStatus {
  const PlaceStatus({
    required this.placeId,
    required this.placeName,
    required this.overallStatus,
    required this.weather,
    required this.crowding,
    required this.businessHours,
    required this.traffic,
    required this.updatedAt,
    this.reason,
  });

  final String placeId;
  final String placeName;
  final StatusLevel overallStatus;
  final StatusDetail weather;
  final StatusDetail crowding;
  final StatusDetail businessHours;
  final StatusDetail traffic;
  final DateTime updatedAt;
  final String? reason;

  factory PlaceStatus.fromJson(Map<String, dynamic> json) {
    return PlaceStatus(
      placeId: json['place_id'] as String,
      placeName: json['place_name'] as String? ?? '',
      overallStatus:
          StatusLevel.fromString(json['overall_status'] as String? ?? 'unknown'),
      weather: StatusDetail.fromJson(
          json['weather'] as Map<String, dynamic>? ?? {}),
      crowding: StatusDetail.fromJson(
          json['crowding'] as Map<String, dynamic>? ?? {}),
      businessHours: StatusDetail.fromJson(
          json['business_hours'] as Map<String, dynamic>? ?? {}),
      traffic: StatusDetail.fromJson(
          json['traffic'] as Map<String, dynamic>? ?? {}),
      updatedAt: json['updated_at'] != null
          ? DateTime.parse(json['updated_at'] as String)
          : DateTime.now(),
      reason: json['reason'] as String?,
    );
  }
}

/// 개별 상태 세부 항목
class StatusDetail {
  const StatusDetail({
    required this.status,
    required this.label,
    this.message,
    this.value,
  });

  final StatusLevel status;
  final String label;
  final String? message;
  final String? value;

  factory StatusDetail.fromJson(Map<String, dynamic> json) {
    return StatusDetail(
      status: StatusLevel.fromString(json['status'] as String? ?? 'unknown'),
      label: json['label'] as String? ?? '',
      message: json['message'] as String?,
      value: json['value'] as String?,
    );
  }
}
