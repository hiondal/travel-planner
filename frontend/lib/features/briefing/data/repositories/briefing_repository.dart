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
    final response = await briefingDataSource.getBriefings(tripId: tripId);
    return response.briefings;
  }

  Future<Briefing> getBriefing(String briefingId) async {
    return briefingDataSource.getBriefing(briefingId);
  }

  Future<List<Alternative>> getAlternatives(String briefingId) async {
    final response = await alternativeDataSource.getAlternatives(briefingId);
    return response.alternatives;
  }

  Future<Map<String, dynamic>> applyAlternative({
    required String alternativeId,
    required String tripId,
    required String scheduleItemId,
  }) async {
    return alternativeDataSource.applyAlternative(
      alternativeId,
      tripId,
      scheduleItemId,
    );
  }
}
