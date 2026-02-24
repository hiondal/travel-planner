// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'app_user_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$appUserHash() => r'8a32b6ae82f0e37230a282bac8403dbb6aaade4c';

/// 전역 앱 사용자 상태
/// 라우터 가드, 구독 티어 분기 등 앱 전역에서 참조한다.
///
/// Copied from [AppUser].
@ProviderFor(AppUser)
final appUserProvider = NotifierProvider<AppUser, AppUserState>.internal(
  AppUser.new,
  name: r'appUserProvider',
  debugGetCreateSourceHash:
      const bool.fromEnvironment('dart.vm.product') ? null : _$appUserHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$AppUser = Notifier<AppUserState>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
