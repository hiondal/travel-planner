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

  /// GET /badges/{placeId}/detail
  /// 장소 상태 상세 조회 (백엔드: MNTR-02)
  /// 변경: /monitor/places/{id}/status → /badges/{placeId}/detail
  Future<PlaceStatus> getPlaceStatus(String placeId) async {
    final response = await dio.get<Map<String, dynamic>>(
      '/badges/$placeId/detail',
    );
    return PlaceStatus.fromJson(response.data!);
  }

  /// GET /badges?place_ids={placeId1},{placeId2},...
  /// 장소 상태 배지 목록 일괄 조회 (백엔드: MNTR-01)
  /// 변경: /monitor/trips/{tripId}/status → /badges?place_ids=...
  Future<Map<String, dynamic>> getBadges(List<String> placeIds) async {
    final response = await dio.get<Map<String, dynamic>>(
      '/badges',
      queryParameters: {'place_ids': placeIds.join(',')},
    );
    return response.data!;
  }
}
