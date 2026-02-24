// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'place_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$placeDetailHash() => r'6dfa68574990dd85af8e6e78d14ef37fc90aad8a';

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

/// 장소 상세 Provider
///
/// Copied from [placeDetail].
@ProviderFor(placeDetail)
const placeDetailProvider = PlaceDetailFamily();

/// 장소 상세 Provider
///
/// Copied from [placeDetail].
class PlaceDetailFamily extends Family<AsyncValue<Place>> {
  /// 장소 상세 Provider
  ///
  /// Copied from [placeDetail].
  const PlaceDetailFamily();

  /// 장소 상세 Provider
  ///
  /// Copied from [placeDetail].
  PlaceDetailProvider call(
    String placeId,
  ) {
    return PlaceDetailProvider(
      placeId,
    );
  }

  @override
  PlaceDetailProvider getProviderOverride(
    covariant PlaceDetailProvider provider,
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
  String? get name => r'placeDetailProvider';
}

/// 장소 상세 Provider
///
/// Copied from [placeDetail].
class PlaceDetailProvider extends AutoDisposeFutureProvider<Place> {
  /// 장소 상세 Provider
  ///
  /// Copied from [placeDetail].
  PlaceDetailProvider(
    String placeId,
  ) : this._internal(
          (ref) => placeDetail(
            ref as PlaceDetailRef,
            placeId,
          ),
          from: placeDetailProvider,
          name: r'placeDetailProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$placeDetailHash,
          dependencies: PlaceDetailFamily._dependencies,
          allTransitiveDependencies:
              PlaceDetailFamily._allTransitiveDependencies,
          placeId: placeId,
        );

  PlaceDetailProvider._internal(
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
    FutureOr<Place> Function(PlaceDetailRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: PlaceDetailProvider._internal(
        (ref) => create(ref as PlaceDetailRef),
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
  AutoDisposeFutureProviderElement<Place> createElement() {
    return _PlaceDetailProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is PlaceDetailProvider && other.placeId == placeId;
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
mixin PlaceDetailRef on AutoDisposeFutureProviderRef<Place> {
  /// The parameter `placeId` of this provider.
  String get placeId;
}

class _PlaceDetailProviderElement
    extends AutoDisposeFutureProviderElement<Place> with PlaceDetailRef {
  _PlaceDetailProviderElement(super.provider);

  @override
  String get placeId => (origin as PlaceDetailProvider).placeId;
}

String _$placeSearchHash() => r'6591be13e3dfc497a0334b65acdca70634f01562';

/// 장소 검색 상태 Notifier
///
/// Copied from [PlaceSearch].
@ProviderFor(PlaceSearch)
final placeSearchProvider =
    AutoDisposeNotifierProvider<PlaceSearch, AsyncValue<List<Place>>>.internal(
  PlaceSearch.new,
  name: r'placeSearchProvider',
  debugGetCreateSourceHash:
      const bool.fromEnvironment('dart.vm.product') ? null : _$placeSearchHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$PlaceSearch = AutoDisposeNotifier<AsyncValue<List<Place>>>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
