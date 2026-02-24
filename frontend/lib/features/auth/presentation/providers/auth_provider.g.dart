// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'auth_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$socialLoginNotifierHash() =>
    r'8ecf4b7de2ec36b1321610b8c6a3211635d65720';

/// 소셜 로그인 AsyncNotifier
///
/// Copied from [SocialLoginNotifier].
@ProviderFor(SocialLoginNotifier)
final socialLoginNotifierProvider =
    AutoDisposeNotifierProvider<SocialLoginNotifier, AsyncValue<void>>.internal(
  SocialLoginNotifier.new,
  name: r'socialLoginNotifierProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$socialLoginNotifierHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$SocialLoginNotifier = AutoDisposeNotifier<AsyncValue<void>>;
String _$logoutNotifierHash() => r'b270531997e4cbb0915652aaf2e22af9c62bfc18';

/// 로그아웃 AsyncNotifier
///
/// Copied from [LogoutNotifier].
@ProviderFor(LogoutNotifier)
final logoutNotifierProvider =
    AutoDisposeNotifierProvider<LogoutNotifier, AsyncValue<void>>.internal(
  LogoutNotifier.new,
  name: r'logoutNotifierProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$logoutNotifierHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$LogoutNotifier = AutoDisposeNotifier<AsyncValue<void>>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
