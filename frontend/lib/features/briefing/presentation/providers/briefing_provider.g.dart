// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'briefing_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$briefingListHash() => r'9cd90c717fa83451d185751476143d09e99b3128';

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

/// 브리핑 목록 Provider
///
/// Copied from [briefingList].
@ProviderFor(briefingList)
const briefingListProvider = BriefingListFamily();

/// 브리핑 목록 Provider
///
/// Copied from [briefingList].
class BriefingListFamily extends Family<AsyncValue<List<Briefing>>> {
  /// 브리핑 목록 Provider
  ///
  /// Copied from [briefingList].
  const BriefingListFamily();

  /// 브리핑 목록 Provider
  ///
  /// Copied from [briefingList].
  BriefingListProvider call({
    String? tripId,
  }) {
    return BriefingListProvider(
      tripId: tripId,
    );
  }

  @override
  BriefingListProvider getProviderOverride(
    covariant BriefingListProvider provider,
  ) {
    return call(
      tripId: provider.tripId,
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
  String? get name => r'briefingListProvider';
}

/// 브리핑 목록 Provider
///
/// Copied from [briefingList].
class BriefingListProvider extends AutoDisposeFutureProvider<List<Briefing>> {
  /// 브리핑 목록 Provider
  ///
  /// Copied from [briefingList].
  BriefingListProvider({
    String? tripId,
  }) : this._internal(
          (ref) => briefingList(
            ref as BriefingListRef,
            tripId: tripId,
          ),
          from: briefingListProvider,
          name: r'briefingListProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$briefingListHash,
          dependencies: BriefingListFamily._dependencies,
          allTransitiveDependencies:
              BriefingListFamily._allTransitiveDependencies,
          tripId: tripId,
        );

  BriefingListProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.tripId,
  }) : super.internal();

  final String? tripId;

  @override
  Override overrideWith(
    FutureOr<List<Briefing>> Function(BriefingListRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: BriefingListProvider._internal(
        (ref) => create(ref as BriefingListRef),
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        tripId: tripId,
      ),
    );
  }

  @override
  AutoDisposeFutureProviderElement<List<Briefing>> createElement() {
    return _BriefingListProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is BriefingListProvider && other.tripId == tripId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, tripId.hashCode);

    return _SystemHash.finish(hash);
  }
}

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
mixin BriefingListRef on AutoDisposeFutureProviderRef<List<Briefing>> {
  /// The parameter `tripId` of this provider.
  String? get tripId;
}

class _BriefingListProviderElement
    extends AutoDisposeFutureProviderElement<List<Briefing>>
    with BriefingListRef {
  _BriefingListProviderElement(super.provider);

  @override
  String? get tripId => (origin as BriefingListProvider).tripId;
}

String _$briefingDetailHash() => r'6b173a3b653aba7de6597b84d09be9a174a2745e';

/// 브리핑 상세 Provider
///
/// Copied from [briefingDetail].
@ProviderFor(briefingDetail)
const briefingDetailProvider = BriefingDetailFamily();

/// 브리핑 상세 Provider
///
/// Copied from [briefingDetail].
class BriefingDetailFamily extends Family<AsyncValue<Briefing>> {
  /// 브리핑 상세 Provider
  ///
  /// Copied from [briefingDetail].
  const BriefingDetailFamily();

  /// 브리핑 상세 Provider
  ///
  /// Copied from [briefingDetail].
  BriefingDetailProvider call(
    String briefingId,
  ) {
    return BriefingDetailProvider(
      briefingId,
    );
  }

  @override
  BriefingDetailProvider getProviderOverride(
    covariant BriefingDetailProvider provider,
  ) {
    return call(
      provider.briefingId,
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
  String? get name => r'briefingDetailProvider';
}

/// 브리핑 상세 Provider
///
/// Copied from [briefingDetail].
class BriefingDetailProvider extends AutoDisposeFutureProvider<Briefing> {
  /// 브리핑 상세 Provider
  ///
  /// Copied from [briefingDetail].
  BriefingDetailProvider(
    String briefingId,
  ) : this._internal(
          (ref) => briefingDetail(
            ref as BriefingDetailRef,
            briefingId,
          ),
          from: briefingDetailProvider,
          name: r'briefingDetailProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$briefingDetailHash,
          dependencies: BriefingDetailFamily._dependencies,
          allTransitiveDependencies:
              BriefingDetailFamily._allTransitiveDependencies,
          briefingId: briefingId,
        );

  BriefingDetailProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.briefingId,
  }) : super.internal();

  final String briefingId;

  @override
  Override overrideWith(
    FutureOr<Briefing> Function(BriefingDetailRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: BriefingDetailProvider._internal(
        (ref) => create(ref as BriefingDetailRef),
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        briefingId: briefingId,
      ),
    );
  }

  @override
  AutoDisposeFutureProviderElement<Briefing> createElement() {
    return _BriefingDetailProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is BriefingDetailProvider && other.briefingId == briefingId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, briefingId.hashCode);

    return _SystemHash.finish(hash);
  }
}

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
mixin BriefingDetailRef on AutoDisposeFutureProviderRef<Briefing> {
  /// The parameter `briefingId` of this provider.
  String get briefingId;
}

class _BriefingDetailProviderElement
    extends AutoDisposeFutureProviderElement<Briefing> with BriefingDetailRef {
  _BriefingDetailProviderElement(super.provider);

  @override
  String get briefingId => (origin as BriefingDetailProvider).briefingId;
}

String _$alternativeListHash() => r'994bca36e1fe1d7802c664f19e900d15055bcb3d';

/// 대안 목록 Provider
///
/// Copied from [alternativeList].
@ProviderFor(alternativeList)
const alternativeListProvider = AlternativeListFamily();

/// 대안 목록 Provider
///
/// Copied from [alternativeList].
class AlternativeListFamily extends Family<AsyncValue<List<Alternative>>> {
  /// 대안 목록 Provider
  ///
  /// Copied from [alternativeList].
  const AlternativeListFamily();

  /// 대안 목록 Provider
  ///
  /// Copied from [alternativeList].
  AlternativeListProvider call(
    String briefingId,
  ) {
    return AlternativeListProvider(
      briefingId,
    );
  }

  @override
  AlternativeListProvider getProviderOverride(
    covariant AlternativeListProvider provider,
  ) {
    return call(
      provider.briefingId,
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
  String? get name => r'alternativeListProvider';
}

/// 대안 목록 Provider
///
/// Copied from [alternativeList].
class AlternativeListProvider
    extends AutoDisposeFutureProvider<List<Alternative>> {
  /// 대안 목록 Provider
  ///
  /// Copied from [alternativeList].
  AlternativeListProvider(
    String briefingId,
  ) : this._internal(
          (ref) => alternativeList(
            ref as AlternativeListRef,
            briefingId,
          ),
          from: alternativeListProvider,
          name: r'alternativeListProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$alternativeListHash,
          dependencies: AlternativeListFamily._dependencies,
          allTransitiveDependencies:
              AlternativeListFamily._allTransitiveDependencies,
          briefingId: briefingId,
        );

  AlternativeListProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.briefingId,
  }) : super.internal();

  final String briefingId;

  @override
  Override overrideWith(
    FutureOr<List<Alternative>> Function(AlternativeListRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: AlternativeListProvider._internal(
        (ref) => create(ref as AlternativeListRef),
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        briefingId: briefingId,
      ),
    );
  }

  @override
  AutoDisposeFutureProviderElement<List<Alternative>> createElement() {
    return _AlternativeListProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is AlternativeListProvider && other.briefingId == briefingId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, briefingId.hashCode);

    return _SystemHash.finish(hash);
  }
}

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
mixin AlternativeListRef on AutoDisposeFutureProviderRef<List<Alternative>> {
  /// The parameter `briefingId` of this provider.
  String get briefingId;
}

class _AlternativeListProviderElement
    extends AutoDisposeFutureProviderElement<List<Alternative>>
    with AlternativeListRef {
  _AlternativeListProviderElement(super.provider);

  @override
  String get briefingId => (origin as AlternativeListProvider).briefingId;
}

String _$applyAlternativeNotifierHash() =>
    r'412254c01d399cc456398151a6bbe647a11163b8';

/// 대안 적용 Notifier
///
/// Copied from [ApplyAlternativeNotifier].
@ProviderFor(ApplyAlternativeNotifier)
final applyAlternativeNotifierProvider = AutoDisposeNotifierProvider<
    ApplyAlternativeNotifier, AsyncValue<Map<String, dynamic>?>>.internal(
  ApplyAlternativeNotifier.new,
  name: r'applyAlternativeNotifierProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$applyAlternativeNotifierHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$ApplyAlternativeNotifier
    = AutoDisposeNotifier<AsyncValue<Map<String, dynamic>?>>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
