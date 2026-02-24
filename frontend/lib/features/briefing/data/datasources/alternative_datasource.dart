import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/network/dio_client.dart';
import '../../domain/models/briefing_model.dart';

part 'alternative_datasource.g.dart';

/// Alternative Service DataSource
// 변경: Prism Mock(http://localhost:4010) → ALTN 서비스(http://localhost:8086/api/v1)
// dioClientProvider(ApiService.alternative) 주입으로 포트 분리
@riverpod
AlternativeDataSource alternativeDataSource(Ref ref) {
  final dio = ref.watch(dioClientProvider(ApiService.alternative));
  return AlternativeDataSource(dio: dio);
}

class AlternativeDataSource {
  AlternativeDataSource({required this.dio});

  final Dio dio;

  /// POST /alternatives/search
  /// 대안 장소 검색 — 카드 3장 생성 (백엔드: ALTN 서비스)
  /// 변경: GET /alternatives → POST /alternatives/search
  Future<AlternativeListResponse> searchAlternatives({
    required String placeId,
    required String category,
    required double lat,
    required double lng,
  }) async {
    final response = await dio.post<Map<String, dynamic>>(
      '/alternatives/search',
      data: {
        'place_id': placeId,
        'category': category,
        'location': {'lat': lat, 'lng': lng},
      },
    );
    return AlternativeListResponse.fromJson(response.data!);
  }

  /// POST /alternatives/{altId}/select
  /// 대안 카드 선택 및 일정 반영 (백엔드: ALTN 서비스)
  /// 변경: POST /alternatives/{id}/apply → POST /alternatives/{altId}/select
  Future<Map<String, dynamic>> selectAlternative({
    required String altId,
    required String originalPlaceId,
    required String scheduleItemId,
    required String tripId,
    required int selectedRank,
    int? elapsedSeconds,
  }) async {
    final response = await dio.post<Map<String, dynamic>>(
      '/alternatives/$altId/select',
      data: {
        'original_place_id': originalPlaceId,
        'schedule_item_id': scheduleItemId,
        'trip_id': tripId,
        'selected_rank': selectedRank,
        if (elapsedSeconds != null) 'elapsed_seconds': elapsedSeconds,
      },
    );
    return response.data!;
  }
}
