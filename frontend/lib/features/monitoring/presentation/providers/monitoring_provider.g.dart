// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'monitoring_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$placeStatusHash() => r'b6525332ef702027a3c52030534a4ce42066445f';

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

/// 장소 상태 Provider
///
/// Copied from [placeStatus].
@ProviderFor(placeStatus)
const placeStatusProvider = PlaceStatusFamily();

/// 장소 상태 Provider
///
/// Copied from [placeStatus].
class PlaceStatusFamily extends Family<AsyncValue<PlaceStatus>> {
  /// 장소 상태 Provider
  ///
  /// Copied from [placeStatus].
  const PlaceStatusFamily();

  /// 장소 상태 Provider
  ///
  /// Copied from [placeStatus].
  PlaceStatusProvider call(
    String placeId,
  ) {
    return PlaceStatusProvider(
      placeId,
    );
  }

  @override
  PlaceStatusProvider getProviderOverride(
    covariant PlaceStatusProvider provider,
  ) {
    return call(
      provider.placeId,
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
  String? get name => r'placeStatusProvider';
}

/// 장소 상태 Provider
///
/// Copied from [placeStatus].
class PlaceStatusProvider extends AutoDisposeFutureProvider<PlaceStatus> {
  /// 장소 상태 Provider
  ///
  /// Copied from [placeStatus].
  PlaceStatusProvider(
    String placeId,
  ) : this._internal(
          (ref) => placeStatus(
            ref as PlaceStatusRef,
            placeId,
          ),
          from: placeStatusProvider,
          name: r'placeStatusProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$placeStatusHash,
          dependencies: PlaceStatusFamily._dependencies,
          allTransitiveDependencies:
              PlaceStatusFamily._allTransitiveDependencies,
          placeId: placeId,
        );

  PlaceStatusProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.placeId,
  }) : super.internal();

  final String placeId;

  @override
  Override overrideWith(
    FutureOr<PlaceStatus> Function(PlaceStatusRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: PlaceStatusProvider._internal(
        (ref) => create(ref as PlaceStatusRef),
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        placeId: placeId,
      ),
    );
  }

  @override
  AutoDisposeFutureProviderElement<PlaceStatus> createElement() {
    return _PlaceStatusProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is PlaceStatusProvider && other.placeId == placeId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, placeId.hashCode);

    return _SystemHash.finish(hash);
  }
}

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
mixin PlaceStatusRef on AutoDisposeFutureProviderRef<PlaceStatus> {
  /// The parameter `placeId` of this provider.
  String get placeId;
}

class _PlaceStatusProviderElement
    extends AutoDisposeFutureProviderElement<PlaceStatus> with PlaceStatusRef {
  _PlaceStatusProviderElement(super.provider);

  @override
  String get placeId => (origin as PlaceStatusProvider).placeId;
}

String _$tripStatusHash() => r'ed34e18084eefde1470beeaab4d5b3fe69330e27';

/// 여행 장소 배지 목록 Provider
///
/// Copied from [tripStatus].
@ProviderFor(tripStatus)
const tripStatusProvider = TripStatusFamily();

/// 여행 장소 배지 목록 Provider
///
/// Copied from [tripStatus].
class TripStatusFamily extends Family<AsyncValue<Map<String, dynamic>>> {
  /// 여행 장소 배지 목록 Provider
  ///
  /// Copied from [tripStatus].
  const TripStatusFamily();

  /// 여행 장소 배지 목록 Provider
  ///
  /// Copied from [tripStatus].
  TripStatusProvider call(
    List<String> placeIds,
  ) {
    return TripStatusProvider(
      placeIds,
    );
  }

  @override
  TripStatusProvider getProviderOverride(
    covariant TripStatusProvider provider,
  ) {
    return call(
      provider.placeIds,
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
  String? get name => r'tripStatusProvider';
}

/// 여행 장소 배지 목록 Provider
///
/// Copied from [tripStatus].
class TripStatusProvider
    extends AutoDisposeFutureProvider<Map<String, dynamic>> {
  /// 여행 장소 배지 목록 Provider
  ///
  /// Copied from [tripStatus].
  TripStatusProvider(
    List<String> placeIds,
  ) : this._internal(
          (ref) => tripStatus(
            ref as TripStatusRef,
            placeIds,
          ),
          from: tripStatusProvider,
          name: r'tripStatusProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$tripStatusHash,
          dependencies: TripStatusFamily._dependencies,
          allTransitiveDependencies:
              TripStatusFamily._allTransitiveDependencies,
          placeIds: placeIds,
        );

  TripStatusProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.placeIds,
  }) : super.internal();

  final List<String> placeIds;

  @override
  Override overrideWith(
    FutureOr<Map<String, dynamic>> Function(TripStatusRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: TripStatusProvider._internal(
        (ref) => create(ref as TripStatusRef),
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        placeIds: placeIds,
      ),
    );
  }

  @override
  AutoDisposeFutureProviderElement<Map<String, dynamic>> createElement() {
    return _TripStatusProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is TripStatusProvider && other.placeIds == placeIds;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, placeIds.hashCode);

    return _SystemHash.finish(hash);
  }
}

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
mixin TripStatusRef on AutoDisposeFutureProviderRef<Map<String, dynamic>> {
  /// The parameter `placeIds` of this provider.
  List<String> get placeIds;
}

class _TripStatusProviderElement
    extends AutoDisposeFutureProviderElement<Map<String, dynamic>>
    with TripStatusRef {
  _TripStatusProviderElement(super.provider);

  @override
  List<String> get placeIds => (origin as TripStatusProvider).placeIds;
}
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
