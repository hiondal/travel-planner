import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../domain/models/place_model.dart';
import '../datasources/place_datasource.dart';

part 'place_repository.g.dart';

@riverpod
PlaceRepository placeRepository(Ref ref) {
  return PlaceRepository(
    dataSource: ref.watch(placeDataSourceProvider),
  );
}

class PlaceRepository {
  PlaceRepository({required this.dataSource});

  final PlaceDataSource dataSource;

  Future<List<Place>> searchPlaces({
    required String keyword,
    String? city,
    String? category,
  }) async {
    final response = await dataSource.searchPlaces(
      keyword: keyword,
      city: city,
      category: category,
    );
    return response.places;
  }

  Future<Place> getPlace(String placeId) async {
    return dataSource.getPlace(placeId);
  }
}
