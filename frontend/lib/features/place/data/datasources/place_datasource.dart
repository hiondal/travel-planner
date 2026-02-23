import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../core/network/dio_client.dart';
import '../../domain/models/place_model.dart';

part 'place_datasource.g.dart';

/// Place Service DataSource
/// Prism Mock: http://localhost:4012
@riverpod
PlaceDataSource placeDataSource(Ref ref) {
  final dio = ref.watch(dioClientProvider);
  return PlaceDataSource(dio: dio);
}

class PlaceDataSource {
  PlaceDataSource({required this.dio});

  final Dio dio;

  /// GET /places/search
  /// 장소 키워드 검색
  Future<PlaceSearchResponse> searchPlaces({
    required String keyword,
    String? city,
    String? category,
  }) async {
    final response = await dio.get<Map<String, dynamic>>(
      '/places/search',
      queryParameters: {
        'keyword': keyword,
        if (city != null) 'city': city,
        if (category != null) 'category': category,
      },
    );
    return PlaceSearchResponse.fromJson(response.data!);
  }

  /// GET /places/{placeId}
  /// 장소 상세 조회
  Future<Place> getPlace(String placeId) async {
    final response = await dio.get<Map<String, dynamic>>(
      '/places/$placeId',
    );
    return Place.fromJson(response.data!);
  }
}
