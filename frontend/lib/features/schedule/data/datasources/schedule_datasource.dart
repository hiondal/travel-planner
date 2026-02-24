import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/network/dio_client.dart';
import '../../domain/models/trip_model.dart';

part 'schedule_datasource.g.dart';

/// Schedule Service DataSource
// 변경: Prism Mock(http://localhost:4010) → SCHD 서비스(http://localhost:8082/api/v1)
// dioClientProvider(ApiService.schedule) 주입으로 포트 분리
@riverpod
ScheduleDataSource scheduleDataSource(Ref ref) {
  final dio = ref.watch(dioClientProvider(ApiService.schedule));
  return ScheduleDataSource(dio: dio);
}

class ScheduleDataSource {
  ScheduleDataSource({required this.dio});

  final Dio dio;

  /// GET /trips
  /// 여행 목록 조회
  Future<TripListResponse> getTrips() async {
    final response = await dio.get<Map<String, dynamic>>('/trips');
    return TripListResponse.fromJson(response.data!);
  }

  /// POST /trips
  /// 여행 생성
  Future<Trip> createTrip(TripCreateRequest request) async {
    final response = await dio.post<Map<String, dynamic>>(
      '/trips',
      data: request.toJson(),
    );
    return Trip.fromJson(response.data!);
  }

  /// DELETE /trips/{tripId}
  /// 여행 삭제
  Future<void> deleteTrip(String tripId) async {
    await dio.delete<void>('/trips/$tripId');
  }

  /// GET /trips/{tripId}/schedule
  /// 일정표 조회 (날짜별)
  Future<ScheduleResponse> getSchedule(
    String tripId, {
    DateTime? targetDate,
  }) async {
    final queryParams = targetDate != null
        ? {'date': targetDate.toIso8601String().substring(0, 10)}
        : null;
    final response = await dio.get<Map<String, dynamic>>(
      '/trips/$tripId/schedule',
      queryParameters: queryParams,
    );
    return ScheduleResponse.fromJson(response.data!);
  }

  /// POST /trips/{tripId}/schedule-items
  /// 장소 추가 (백엔드: SCHD-04)
  Future<ScheduleItem> addScheduleItem(
    String tripId,
    AddScheduleItemRequest request,
  ) async {
    final response = await dio.post<Map<String, dynamic>>(
      '/trips/$tripId/schedule-items',
      data: request.toJson(),
    );
    return ScheduleItem.fromJson(response.data!);
  }

  /// DELETE /trips/{tripId}/schedule-items/{scheduleItemId}
  /// 장소 삭제 (백엔드: SCHD-05)
  Future<void> deleteScheduleItem(
    String tripId,
    String scheduleItemId,
  ) async {
    await dio.delete<void>('/trips/$tripId/schedule-items/$scheduleItemId');
  }

  /// PUT /trips/{tripId}/schedule-items/{itemId}/replace
  /// 장소 교체 (백엔드: SCHD-06)
  /// 주의: 백엔드는 순서 변경(reorder) 엔드포인트를 지원하지 않음
  /// 장소 교체는 /schedule-items/{itemId}/replace 로 처리
  Future<Map<String, dynamic>> replaceScheduleItem(
    String tripId,
    String itemId,
    String newPlaceId,
  ) async {
    final response = await dio.put<Map<String, dynamic>>(
      '/trips/$tripId/schedule-items/$itemId/replace',
      data: {'new_place_id': newPlaceId},
    );
    return response.data!;
  }
}
