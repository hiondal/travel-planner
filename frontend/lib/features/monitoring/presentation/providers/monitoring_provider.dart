import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../data/repositories/monitoring_repository.dart';
import '../../domain/models/monitoring_model.dart';

part 'monitoring_provider.g.dart';

/// 장소 상태 Provider
@riverpod
Future<PlaceStatus> placeStatus(Ref ref, String placeId) async {
  return ref.watch(monitoringRepositoryProvider).getPlaceStatus(placeId);
}

/// 여행 전체 상태 Provider
@riverpod
Future<Map<String, dynamic>> tripStatus(Ref ref, String tripId) async {
  return ref.watch(monitoringRepositoryProvider).getTripStatus(tripId);
}
