import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../core/network/dio_client.dart';
import '../../domain/models/monitoring_model.dart';

part 'monitoring_datasource.g.dart';

/// Monitor Service DataSource
/// Prism Mock: http://localhost:4013
@riverpod
MonitoringDataSource monitoringDataSource(Ref ref) {
  final dio = ref.watch(dioClientProvider);
  return MonitoringDataSource(dio: dio);
}

class MonitoringDataSource {
  MonitoringDataSource({required this.dio});

  final Dio dio;

  /// GET /monitor/places/{placeId}/status
  /// 장소 실시간 상태 조회
  Future<PlaceStatus> getPlaceStatus(String placeId) async {
    final response = await dio.get<Map<String, dynamic>>(
      '/monitor/places/$placeId/status',
    );
    return PlaceStatus.fromJson(response.data!);
  }

  /// GET /monitor/trips/{tripId}/status
  /// 여행 전체 상태 요약 조회
  Future<Map<String, dynamic>> getTripStatus(String tripId) async {
    final response = await dio.get<Map<String, dynamic>>(
      '/monitor/trips/$tripId/status',
    );
    return response.data!;
  }
}
