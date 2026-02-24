import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../domain/models/briefing_model.dart';
import '../datasources/alternative_datasource.dart';
import '../datasources/briefing_datasource.dart';

part 'briefing_repository.g.dart';

@riverpod
BriefingRepository briefingRepository(Ref ref) {
  return BriefingRepository(
    briefingDataSource: ref.watch(briefingDataSourceProvider),
    alternativeDataSource: ref.watch(alternativeDataSourceProvider),
  );
}

class BriefingRepository {
  BriefingRepository({
    required this.briefingDataSource,
    required this.alternativeDataSource,
  });

  final BriefingDataSource briefingDataSource;
  final AlternativeDataSource alternativeDataSource;

  Future<List<Briefing>> getBriefings({String? tripId}) async {
    final response = await briefingDataSource.getBriefings();
    return response.briefings;
  }

  Future<Briefing> getBriefing(String briefingId) async {
    return briefingDataSource.getBriefing(briefingId);
  }

  /// 대안 검색 — briefingId로 상세를 조회한 뒤 실제 placeId로 검색
  Future<List<Alternative>> getAlternatives(String briefingId) async {
    final briefing = await briefingDataSource.getBriefing(briefingId);
    final placeId = briefing.placeId ?? briefingId;
    final response = await alternativeDataSource.searchAlternatives(
      placeId: placeId,
      category: 'restaurant',
      lat: 0.0,
      lng: 0.0,
    );
    return response.alternatives;
  }

  /// 대안 선택 및 일정 반영
  Future<Map<String, dynamic>> applyAlternative({
    required String alternativeId,
    required String tripId,
    required String scheduleItemId,
  }) async {
    return alternativeDataSource.selectAlternative(
      altId: alternativeId,
      originalPlaceId: '',
      scheduleItemId: scheduleItemId,
      tripId: tripId,
      selectedRank: 1,
    );
  }
}
