import '../../../../shared/models/status_level.dart';

/// 장소 상태 상세 모델 (MNTR 서비스)
/// 백엔드 응답: GET /api/v1/badges/{placeId}/detail
/// Dio 인터셉터가 data 필드를 추출하므로 fromJson은 내부 객체를 직접 수신
class PlaceStatus {
  const PlaceStatus({
    required this.placeId,
    required this.placeName,
    required this.overallStatus,
    required this.weather,
    required this.congestion,
    required this.businessStatus,
    required this.travelTime,
    required this.updatedAt,
    this.reason,
    this.showAlternativeButton = false,
  });

  final String placeId;
  final String placeName;
  final StatusLevel overallStatus;
  final StatusDetail weather;
  final StatusDetail congestion;
  final StatusDetail businessStatus;
  final StatusDetail travelTime;
  final DateTime updatedAt;
  final String? reason;
  final bool showAlternativeButton;

  factory PlaceStatus.fromJson(Map<String, dynamic> json) {
    final details =
        json['details'] as Map<String, dynamic>? ?? <String, dynamic>{};

    return PlaceStatus(
      placeId: json['place_id'] as String? ?? '',
      placeName: json['place_name'] as String? ?? '',
      overallStatus: StatusLevel.fromString(
          json['overall_status'] as String? ?? 'GREY'),
      weather: StatusDetail.fromWeatherJson(
          details['weather'] as Map<String, dynamic>? ?? {}),
      congestion: StatusDetail.fromCongestionJson(
          details['congestion'] as Map<String, dynamic>? ?? {}),
      businessStatus: StatusDetail.fromBusinessStatusJson(
          details['business_status'] as Map<String, dynamic>? ?? {}),
      travelTime: StatusDetail.fromTravelTimeJson(
          details['travel_time'] as Map<String, dynamic>? ?? {}),
      updatedAt: json['updated_at'] != null
          ? DateTime.parse(json['updated_at'] as String)
          : DateTime.now(),
      reason: json['reason'] as String?,
      showAlternativeButton:
          json['show_alternative_button'] as bool? ?? false,
    );
  }
}

/// 개별 상태 세부 항목
class StatusDetail {
  const StatusDetail({
    required this.status,
    this.message,
  });

  final StatusLevel status;

  /// UI에 표시할 부가 설명 문자열 (value 또는 계산된 문자열)
  final String? message;

  /// 날씨: status + value + precipitation_prob
  factory StatusDetail.fromWeatherJson(Map<String, dynamic> json) {
    final value = json['value'] as String?;
    final prob = json['precipitation_prob'];
    String? msg;
    if (value != null && value.isNotEmpty) {
      msg = prob != null ? '$value (강수확률 $prob%)' : value;
    }
    return StatusDetail(
      status: StatusLevel.fromString(json['status'] as String? ?? 'GREY'),
      message: msg,
    );
  }

  /// 혼잡도: status + value + is_unknown
  factory StatusDetail.fromCongestionJson(Map<String, dynamic> json) {
    final isUnknown = json['is_unknown'] as bool? ?? false;
    final value = json['value'] as String?;
    String? msg;
    if (isUnknown) {
      msg = '정보 없음';
    } else if (value != null && value.isNotEmpty) {
      msg = value;
    }
    return StatusDetail(
      status: StatusLevel.fromString(json['status'] as String? ?? 'GREY'),
      message: msg,
    );
  }

  /// 영업 상태: status + value
  factory StatusDetail.fromBusinessStatusJson(Map<String, dynamic> json) {
    return StatusDetail(
      status: StatusLevel.fromString(json['status'] as String? ?? 'GREY'),
      message: json['value'] as String?,
    );
  }

  /// 이동 시간: walking_minutes + transit_minutes + distance_m
  factory StatusDetail.fromTravelTimeJson(Map<String, dynamic> json) {
    final walking = json['walking_minutes'];
    final transit = json['transit_minutes'];
    final distanceM = json['distance_m'];
    String? msg;
    if (walking != null || transit != null) {
      final parts = <String>[];
      if (walking != null) parts.add('도보 $walking분');
      if (transit != null) parts.add('대중교통 $transit분');
      if (distanceM != null) {
        final km = (distanceM as num) / 1000.0;
        parts.add('${km.toStringAsFixed(1)}km');
      }
      msg = parts.join(' · ');
    }
    return StatusDetail(
      status: StatusLevel.fromString(json['status'] as String? ?? 'GREY'),
      message: msg,
    );
  }
}
