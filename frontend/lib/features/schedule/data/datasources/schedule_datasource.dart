import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../core/network/dio_client.dart';
import '../../domain/models/trip_model.dart';

part 'schedule_datasource.g.dart';

/// Schedule Service DataSource
/// Prism Mock: http://localhost:4011
/// api-mapping.md SCHD 서비스 엔드포인트 기반
@riverpod
ScheduleDataSource scheduleDataSource(Ref ref) {
  final dio = ref.watch(dioClientProvider);
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

  /// POST /trips/{tripId}/schedule
  /// 장소 추가
  Future<ScheduleItem> addScheduleItem(
    String tripId,
    AddScheduleItemRequest request,
  ) async {
    final response = await dio.post<Map<String, dynamic>>(
      '/trips/$tripId/schedule',
      data: request.toJson(),
    );
    return ScheduleItem.fromJson(response.data!);
  }

  /// DELETE /trips/{tripId}/schedule/{scheduleItemId}
  /// 장소 삭제
  Future<void> deleteScheduleItem(
    String tripId,
    String scheduleItemId,
  ) async {
    await dio.delete<void>('/trips/$tripId/schedule/$scheduleItemId');
  }

  /// PUT /trips/{tripId}/schedule/reorder
  /// 장소 순서 변경
  Future<void> reorderScheduleItems(
    String tripId,
    List<String> orderedItemIds,
  ) async {
    await dio.put<void>(
      '/trips/$tripId/schedule/reorder',
      data: {'item_ids': orderedItemIds},
    );
  }
}
