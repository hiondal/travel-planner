import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../core/network/dio_client.dart';
import '../../domain/models/briefing_model.dart';

part 'alternative_datasource.g.dart';

/// Alternative Service DataSource
/// Prism Mock: http://localhost:4015
@riverpod
AlternativeDataSource alternativeDataSource(Ref ref) {
  final dio = ref.watch(dioClientProvider);
  return AlternativeDataSource(dio: dio);
}

class AlternativeDataSource {
  AlternativeDataSource({required this.dio});

  final Dio dio;

  /// GET /alternatives
  /// 대안 장소 목록 조회 (브리핑 기반)
  Future<AlternativeListResponse> getAlternatives(String briefingId) async {
    final response = await dio.get<Map<String, dynamic>>(
      '/alternatives',
      queryParameters: {'briefing_id': briefingId},
    );
    return AlternativeListResponse.fromJson(response.data!);
  }

  /// POST /alternatives/{alternativeId}/apply
  /// 대안 장소 적용 (일정 교체)
  Future<Map<String, dynamic>> applyAlternative(
    String alternativeId,
    String tripId,
    String scheduleItemId,
  ) async {
    final response = await dio.post<Map<String, dynamic>>(
      '/alternatives/$alternativeId/apply',
      data: {
        'trip_id': tripId,
        'schedule_item_id': scheduleItemId,
      },
    );
    return response.data!;
  }
}
