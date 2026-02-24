import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../core/network/dio_client.dart';
import '../../domain/models/briefing_model.dart';

part 'briefing_datasource.g.dart';

/// Briefing Service DataSource
/// Prism Mock: http://localhost:4014
@riverpod
BriefingDataSource briefingDataSource(Ref ref) {
  final dio = ref.watch(dioClientProvider);
  return BriefingDataSource(dio: dio);
}

class BriefingDataSource {
  BriefingDataSource({required this.dio});

  final Dio dio;

  /// GET /briefings?date={date}
  /// 브리핑 목록 조회 (백엔드 파라미터: date, 형식: yyyy-MM-dd)
  /// 변경: trip_id 쿼리 파라미터 → date 쿼리 파라미터
  Future<BriefingListResponse> getBriefings({DateTime? date}) async {
    final queryParams = date != null
        ? {'date': date.toIso8601String().substring(0, 10)}
        : null;
    final response = await dio.get<Map<String, dynamic>>(
      '/briefings',
      queryParameters: queryParams,
    );
    return BriefingListResponse.fromJson(response.data!);
  }

  /// GET /briefings/{briefingId}
  /// 브리핑 상세 조회
  Future<Briefing> getBriefing(String briefingId) async {
    final response = await dio.get<Map<String, dynamic>>(
      '/briefings/$briefingId',
    );
    return Briefing.fromJson(response.data!);
  }
}
