// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'payment_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$subscriptionPlansHash() => r'b39ca21b4a252b274127a307e71dc5ba94209d93';

/// 구독 플랜 목록 Provider
///
/// Copied from [subscriptionPlans].
@ProviderFor(subscriptionPlans)
final subscriptionPlansProvider =
    AutoDisposeFutureProvider<List<SubscriptionPlan>>.internal(
  subscriptionPlans,
  name: r'subscriptionPlansProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$subscriptionPlansHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
typedef SubscriptionPlansRef
    = AutoDisposeFutureProviderRef<List<SubscriptionPlan>>;
String _$subscriptionStatusHash() =>
    r'f644c1dff6c36626bd9b98a73a6790800a79009a';

/// 구독 상태 Provider
///
/// Copied from [subscriptionStatus].
@ProviderFor(subscriptionStatus)
final subscriptionStatusProvider =
    AutoDisposeFutureProvider<SubscriptionStatus>.internal(
  subscriptionStatus,
  name: r'subscriptionStatusProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$subscriptionStatusHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
typedef SubscriptionStatusRef
    = AutoDisposeFutureProviderRef<SubscriptionStatus>;
String _$purchaseNotifierHash() => r'e1fe3df08750db6ba906338f0c8047a3c6c8dea0';

/// 구독 구매 Notifier
///
/// Copied from [PurchaseNotifier].
@ProviderFor(PurchaseNotifier)
final purchaseNotifierProvider = AutoDisposeNotifierProvider<PurchaseNotifier,
    AsyncValue<SubscriptionStatus?>>.internal(
  PurchaseNotifier.new,
  name: r'purchaseNotifierProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$purchaseNotifierHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$PurchaseNotifier
    = AutoDisposeNotifier<AsyncValue<SubscriptionStatus?>>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
