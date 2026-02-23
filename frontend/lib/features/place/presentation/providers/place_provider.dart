import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../data/repositories/place_repository.dart';
import '../../domain/models/place_model.dart';

part 'place_provider.g.dart';

/// 장소 검색 상태 Notifier
@riverpod
class PlaceSearch extends _$PlaceSearch {
  @override
  AsyncValue<List<Place>> build() => const AsyncValue.data([]);

  Future<void> search({
    required String keyword,
    String? city,
    String? category,
  }) async {
    if (keyword.trim().isEmpty) {
      state = const AsyncValue.data([]);
      return;
    }
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      return ref.read(placeRepositoryProvider).searchPlaces(
            keyword: keyword,
            city: city,
            category: category,
          );
    });
  }

  void clear() {
    state = const AsyncValue.data([]);
  }
}

/// 장소 상세 Provider
@riverpod
Future<Place> placeDetail(Ref ref, String placeId) async {
  return ref.watch(placeRepositoryProvider).getPlace(placeId);
}
