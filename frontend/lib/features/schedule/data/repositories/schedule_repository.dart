import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../domain/models/trip_model.dart';
import '../datasources/schedule_datasource.dart';

part 'schedule_repository.g.dart';

@riverpod
ScheduleRepository scheduleRepository(Ref ref) {
  return ScheduleRepository(
    dataSource: ref.watch(scheduleDataSourceProvider),
  );
}

class ScheduleRepository {
  ScheduleRepository({required this.dataSource});

  final ScheduleDataSource dataSource;

  Future<List<Trip>> getTrips() async {
    final response = await dataSource.getTrips();
    return response.trips;
  }

  Future<Trip> createTrip({
    required String tripName,
    required String city,
    required DateTime startDate,
    required DateTime endDate,
  }) async {
    return dataSource.createTrip(
      TripCreateRequest(
        tripName: tripName,
        city: city,
        startDate: startDate,
        endDate: endDate,
      ),
    );
  }

  Future<void> deleteTrip(String tripId) async {
    await dataSource.deleteTrip(tripId);
  }

  Future<List<ScheduleItem>> getSchedule(
    String tripId, {
    DateTime? targetDate,
  }) async {
    final response = await dataSource.getSchedule(tripId, targetDate: targetDate);
    return response.items;
  }

  Future<ScheduleItem> addScheduleItem({
    required String tripId,
    required String placeId,
    required DateTime scheduledAt,
    required int durationMinutes,
  }) async {
    return dataSource.addScheduleItem(
      tripId,
      AddScheduleItemRequest(
        placeId: placeId,
        scheduledAt: scheduledAt,
        durationMinutes: durationMinutes,
      ),
    );
  }

  Future<void> deleteScheduleItem(
    String tripId,
    String scheduleItemId,
  ) async {
    await dataSource.deleteScheduleItem(tripId, scheduleItemId);
  }

  Future<void> reorderScheduleItems(
    String tripId,
    List<String> orderedItemIds,
  ) async {
    await dataSource.reorderScheduleItems(tripId, orderedItemIds);
  }
}
