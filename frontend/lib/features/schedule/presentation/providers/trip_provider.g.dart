// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'trip_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$tripListHash() => r'0cbb3f92b699b21e45e5451156af44bd10d88c67';

/// 여행 목록 Provider
///
/// Copied from [tripList].
@ProviderFor(tripList)
final tripListProvider = AutoDisposeFutureProvider<List<Trip>>.internal(
  tripList,
  name: r'tripListProvider',
  debugGetCreateSourceHash:
      const bool.fromEnvironment('dart.vm.product') ? null : _$tripListHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
typedef TripListRef = AutoDisposeFutureProviderRef<List<Trip>>;
String _$scheduleHash() => r'cf6439b0220f1811f1f329ef0d46d8ada1eebcf6';

/// Copied from Dart SDK
class _SystemHash {
  _SystemHash._();

  static int combine(int hash, int value) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + value);
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x0007ffff & hash) << 10));
    return hash ^ (hash >> 6);
  }

  static int finish(int hash) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x03ffffff & hash) << 3));
    // ignore: parameter_assignments
    hash = hash ^ (hash >> 11);
    return 0x1fffffff & (hash + ((0x00003fff & hash) << 15));
  }
}

/// 일정표 Provider (tripId + targetDate)
///
/// Copied from [schedule].
@ProviderFor(schedule)
const scheduleProvider = ScheduleFamily();

/// 일정표 Provider (tripId + targetDate)
///
/// Copied from [schedule].
class ScheduleFamily extends Family<AsyncValue<List<ScheduleItem>>> {
  /// 일정표 Provider (tripId + targetDate)
  ///
  /// Copied from [schedule].
  const ScheduleFamily();

  /// 일정표 Provider (tripId + targetDate)
  ///
  /// Copied from [schedule].
  ScheduleProvider call(
    String tripId, {
    DateTime? targetDate,
  }) {
    return ScheduleProvider(
      tripId,
      targetDate: targetDate,
    );
  }

  @override
  ScheduleProvider getProviderOverride(
    covariant ScheduleProvider provider,
  ) {
    return call(
      provider.tripId,
      targetDate: provider.targetDate,
    );
  }

  static const Iterable<ProviderOrFamily>? _dependencies = null;

  @override
  Iterable<ProviderOrFamily>? get dependencies => _dependencies;

  static const Iterable<ProviderOrFamily>? _allTransitiveDependencies = null;

  @override
  Iterable<ProviderOrFamily>? get allTransitiveDependencies =>
      _allTransitiveDependencies;

  @override
  String? get name => r'scheduleProvider';
}

/// 일정표 Provider (tripId + targetDate)
///
/// Copied from [schedule].
class ScheduleProvider extends AutoDisposeFutureProvider<List<ScheduleItem>> {
  /// 일정표 Provider (tripId + targetDate)
  ///
  /// Copied from [schedule].
  ScheduleProvider(
    String tripId, {
    DateTime? targetDate,
  }) : this._internal(
          (ref) => schedule(
            ref as ScheduleRef,
            tripId,
            targetDate: targetDate,
          ),
          from: scheduleProvider,
          name: r'scheduleProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$scheduleHash,
          dependencies: ScheduleFamily._dependencies,
          allTransitiveDependencies: ScheduleFamily._allTransitiveDependencies,
          tripId: tripId,
          targetDate: targetDate,
        );

  ScheduleProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.tripId,
    required this.targetDate,
  }) : super.internal();

  final String tripId;
  final DateTime? targetDate;

  @override
  Override overrideWith(
    FutureOr<List<ScheduleItem>> Function(ScheduleRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: ScheduleProvider._internal(
        (ref) => create(ref as ScheduleRef),
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        tripId: tripId,
        targetDate: targetDate,
      ),
    );
  }

  @override
  AutoDisposeFutureProviderElement<List<ScheduleItem>> createElement() {
    return _ScheduleProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is ScheduleProvider &&
        other.tripId == tripId &&
        other.targetDate == targetDate;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, tripId.hashCode);
    hash = _SystemHash.combine(hash, targetDate.hashCode);

    return _SystemHash.finish(hash);
  }
}

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
mixin ScheduleRef on AutoDisposeFutureProviderRef<List<ScheduleItem>> {
  /// The parameter `tripId` of this provider.
  String get tripId;

  /// The parameter `targetDate` of this provider.
  DateTime? get targetDate;
}

class _ScheduleProviderElement
    extends AutoDisposeFutureProviderElement<List<ScheduleItem>>
    with ScheduleRef {
  _ScheduleProviderElement(super.provider);

  @override
  String get tripId => (origin as ScheduleProvider).tripId;
  @override
  DateTime? get targetDate => (origin as ScheduleProvider).targetDate;
}

String _$tripCreateNotifierHash() =>
    r'1de5d143a27147068edf3c8c36f049cdb3a3aff6';

/// 여행 생성 Notifier
///
/// Copied from [TripCreateNotifier].
@ProviderFor(TripCreateNotifier)
final tripCreateNotifierProvider =
    AutoDisposeNotifierProvider<TripCreateNotifier, AsyncValue<Trip?>>.internal(
  TripCreateNotifier.new,
  name: r'tripCreateNotifierProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$tripCreateNotifierHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$TripCreateNotifier = AutoDisposeNotifier<AsyncValue<Trip?>>;
String _$scheduleItemDeleteNotifierHash() =>
    r'5f6bc59fd8c5a0b71ea4dd7feeb086b45985afca';

/// 장소 삭제 Notifier
///
/// Copied from [ScheduleItemDeleteNotifier].
@ProviderFor(ScheduleItemDeleteNotifier)
final scheduleItemDeleteNotifierProvider = AutoDisposeNotifierProvider<
    ScheduleItemDeleteNotifier, AsyncValue<void>>.internal(
  ScheduleItemDeleteNotifier.new,
  name: r'scheduleItemDeleteNotifierProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$scheduleItemDeleteNotifierHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$ScheduleItemDeleteNotifier = AutoDisposeNotifier<AsyncValue<void>>;
String _$scheduleItemAddNotifierHash() =>
    r'af51b1953c76bcd35233d6ff5ffa0cf81831212b';

/// 장소 추가 Notifier
///
/// Copied from [ScheduleItemAddNotifier].
@ProviderFor(ScheduleItemAddNotifier)
final scheduleItemAddNotifierProvider = AutoDisposeNotifierProvider<
    ScheduleItemAddNotifier, AsyncValue<ScheduleItem?>>.internal(
  ScheduleItemAddNotifier.new,
  name: r'scheduleItemAddNotifierProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$scheduleItemAddNotifierHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$ScheduleItemAddNotifier
    = AutoDisposeNotifier<AsyncValue<ScheduleItem?>>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
