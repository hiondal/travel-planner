import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../data/repositories/schedule_repository.dart';
import '../../domain/models/trip_model.dart';

part 'trip_provider.g.dart';

/// 여행 목록 Provider
@riverpod
Future<List<Trip>> tripList(Ref ref) async {
  return ref.watch(scheduleRepositoryProvider).getTrips();
}

/// 여행 생성 Notifier
@riverpod
class TripCreateNotifier extends _$TripCreateNotifier {
  @override
  AsyncValue<Trip?> build() => const AsyncValue.data(null);

  Future<Trip?> createTrip({
    required String tripName,
    required String city,
    required DateTime startDate,
    required DateTime endDate,
  }) async {
    state = const AsyncValue.loading();
    final result = await AsyncValue.guard(() async {
      final trip = await ref.read(scheduleRepositoryProvider).createTrip(
            tripName: tripName,
            city: city,
            startDate: startDate,
            endDate: endDate,
          );
      // 목록 캐시 무효화 → Prism stateless 대응
      ref.invalidate(tripListProvider);
      return trip;
    });
    state = result;
    return result.valueOrNull;
  }
}

/// 일정표 Provider (tripId + targetDate)
@riverpod
Future<List<ScheduleItem>> schedule(
  Ref ref,
  String tripId, {
  DateTime? targetDate,
}) async {
  return ref.watch(scheduleRepositoryProvider).getSchedule(
        tripId,
        targetDate: targetDate,
      );
}

/// 장소 삭제 Notifier
@riverpod
class ScheduleItemDeleteNotifier extends _$ScheduleItemDeleteNotifier {
  @override
  AsyncValue<void> build() => const AsyncValue.data(null);

  Future<void> delete({
    required String tripId,
    required String scheduleItemId,
  }) async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await ref.read(scheduleRepositoryProvider).deleteScheduleItem(
            tripId,
            scheduleItemId,
          );
    });
  }
}

/// 여행 삭제 Notifier
@riverpod
class TripDeleteNotifier extends _$TripDeleteNotifier {
  @override
  AsyncValue<void> build() => const AsyncValue.data(null);

  Future<bool> deleteTrip(String tripId) async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await ref.read(scheduleRepositoryProvider).deleteTrip(tripId);
      ref.invalidate(tripListProvider);
    });
    return !state.hasError;
  }
}

/// 장소 추가 Notifier
@riverpod
class ScheduleItemAddNotifier extends _$ScheduleItemAddNotifier {
  @override
  AsyncValue<ScheduleItem?> build() => const AsyncValue.data(null);

  Future<ScheduleItem?> addItem({
    required String tripId,
    required String placeId,
    required DateTime scheduledAt,
    required int durationMinutes,
    bool force = false,
  }) async {
    state = const AsyncValue.loading();
    final result = await AsyncValue.guard(() async {
      final item = await ref.read(scheduleRepositoryProvider).addScheduleItem(
            tripId: tripId,
            placeId: placeId,
            scheduledAt: scheduledAt,
            durationMinutes: durationMinutes,
            force: force,
          );
      final targetDate = DateTime(scheduledAt.year, scheduledAt.month, scheduledAt.day);
      ref.invalidate(scheduleProvider(tripId, targetDate: targetDate));
      return item;
    });
    state = result;
    return result.valueOrNull;
  }
}
