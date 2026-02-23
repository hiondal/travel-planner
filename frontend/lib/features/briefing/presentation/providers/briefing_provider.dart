import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../data/repositories/briefing_repository.dart';
import '../../domain/models/briefing_model.dart';

part 'briefing_provider.g.dart';

/// 브리핑 목록 Provider
@riverpod
Future<List<Briefing>> briefingList(Ref ref, {String? tripId}) async {
  return ref.watch(briefingRepositoryProvider).getBriefings(tripId: tripId);
}

/// 브리핑 상세 Provider
@riverpod
Future<Briefing> briefingDetail(Ref ref, String briefingId) async {
  return ref.watch(briefingRepositoryProvider).getBriefing(briefingId);
}

/// 대안 목록 Provider
@riverpod
Future<List<Alternative>> alternativeList(Ref ref, String briefingId) async {
  return ref.watch(briefingRepositoryProvider).getAlternatives(briefingId);
}

/// 대안 적용 Notifier
@riverpod
class ApplyAlternativeNotifier extends _$ApplyAlternativeNotifier {
  @override
  AsyncValue<Map<String, dynamic>?> build() => const AsyncValue.data(null);

  Future<Map<String, dynamic>?> apply({
    required String alternativeId,
    required String tripId,
    required String scheduleItemId,
  }) async {
    state = const AsyncValue.loading();
    final result = await AsyncValue.guard(() {
      return ref.read(briefingRepositoryProvider).applyAlternative(
            alternativeId: alternativeId,
            tripId: tripId,
            scheduleItemId: scheduleItemId,
          );
    });
    state = result;
    return result.valueOrNull;
  }
}
