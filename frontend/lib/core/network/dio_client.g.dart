// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'dio_client.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$dioClientHash() => r'076a3ac782373c07f0fef3e3af181a2bbec02130';

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

/// 서비스별 Dio 인스턴스를 생성하는 provider family
/// 사용 예: ref.watch(dioClientProvider(ApiService.auth))
///
/// Copied from [dioClient].
@ProviderFor(dioClient)
const dioClientProvider = DioClientFamily();

/// 서비스별 Dio 인스턴스를 생성하는 provider family
/// 사용 예: ref.watch(dioClientProvider(ApiService.auth))
///
/// Copied from [dioClient].
class DioClientFamily extends Family<Dio> {
  /// 서비스별 Dio 인스턴스를 생성하는 provider family
  /// 사용 예: ref.watch(dioClientProvider(ApiService.auth))
  ///
  /// Copied from [dioClient].
  const DioClientFamily();

  /// 서비스별 Dio 인스턴스를 생성하는 provider family
  /// 사용 예: ref.watch(dioClientProvider(ApiService.auth))
  ///
  /// Copied from [dioClient].
  DioClientProvider call(
    ApiService service,
  ) {
    return DioClientProvider(
      service,
    );
  }

  @override
  DioClientProvider getProviderOverride(
    covariant DioClientProvider provider,
  ) {
    return call(
      provider.service,
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
  String? get name => r'dioClientProvider';
}

/// 서비스별 Dio 인스턴스를 생성하는 provider family
/// 사용 예: ref.watch(dioClientProvider(ApiService.auth))
///
/// Copied from [dioClient].
class DioClientProvider extends AutoDisposeProvider<Dio> {
  /// 서비스별 Dio 인스턴스를 생성하는 provider family
  /// 사용 예: ref.watch(dioClientProvider(ApiService.auth))
  ///
  /// Copied from [dioClient].
  DioClientProvider(
    ApiService service,
  ) : this._internal(
          (ref) => dioClient(
            ref as DioClientRef,
            service,
          ),
          from: dioClientProvider,
          name: r'dioClientProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$dioClientHash,
          dependencies: DioClientFamily._dependencies,
          allTransitiveDependencies: DioClientFamily._allTransitiveDependencies,
          service: service,
        );

  DioClientProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.service,
  }) : super.internal();

  final ApiService service;

  @override
  Override overrideWith(
    Dio Function(DioClientRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: DioClientProvider._internal(
        (ref) => create(ref as DioClientRef),
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        service: service,
      ),
    );
  }

  @override
  AutoDisposeProviderElement<Dio> createElement() {
    return _DioClientProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is DioClientProvider && other.service == service;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, service.hashCode);

    return _SystemHash.finish(hash);
  }
}

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
mixin DioClientRef on AutoDisposeProviderRef<Dio> {
  /// The parameter `service` of this provider.
  ApiService get service;
}

class _DioClientProviderElement extends AutoDisposeProviderElement<Dio>
    with DioClientRef {
  _DioClientProviderElement(super.provider);

  @override
  ApiService get service => (origin as DioClientProvider).service;
}
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
