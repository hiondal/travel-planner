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

  /// GET /places/search?keyword={keyword}&city={city}
  /// 장소 키워드 검색 (백엔드: PLCE-01)
  /// 주의: 백엔드는 keyword(필수), city(필수) 파라미터만 지원
  /// category 파라미터는 백엔드 미지원 → 제거
  Future<PlaceSearchResponse> searchPlaces({
    required String keyword,
    required String city,
  }) async {
    final response = await dio.get<Map<String, dynamic>>(
      '/places/search',
      queryParameters: {
        'keyword': keyword,
        'city': city,
      },
    );
    return PlaceSearchResponse.fromJson(response.data!);
  }

  /// GET /places/{placeId}
  /// 장소 상세 조회 (백엔드: PLCE-02)
  Future<Place> getPlace(String placeId) async {
    final response = await dio.get<Map<String, dynamic>>(
      '/places/$placeId',
    );
    return Place.fromJson(response.data!);
  }

  /// GET /places/nearby?lat={lat}&lng={lng}&category={category}&radius={radius}
  /// 주변 장소 검색 (백엔드: PLCE-03)
  /// radius: 1000, 2000, 3000 중 하나
  Future<Map<String, dynamic>> searchNearbyPlaces({
    required double lat,
    required double lng,
    required String category,
    required int radius,
  }) async {
    final response = await dio.get<Map<String, dynamic>>(
      '/places/nearby',
      queryParameters: {
        'lat': lat,
        'lng': lng,
        'category': category,
        'radius': radius,
      },
    );
    return response.data!;
  }
}
