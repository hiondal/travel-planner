import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../domain/models/monitoring_model.dart';
import '../datasources/monitoring_datasource.dart';

part 'monitoring_repository.g.dart';

@riverpod
MonitoringRepository monitoringRepository(Ref ref) {
  return MonitoringRepository(
    dataSource: ref.watch(monitoringDataSourceProvider),
  );
}

class MonitoringRepository {
  MonitoringRepository({required this.dataSource});

  final MonitoringDataSource dataSource;

  Future<PlaceStatus> getPlaceStatus(String placeId) async {
    return dataSource.getPlaceStatus(placeId);
  }

  Future<Map<String, dynamic>> getBadges(List<String> placeIds) async {
    return dataSource.getBadges(placeIds);
  }
}
