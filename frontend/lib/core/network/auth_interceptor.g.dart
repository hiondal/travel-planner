// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'auth_interceptor.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$authInterceptorHash() => r'55b1b08ed6398d0145fe72da3faf39e75998d3e2';

/// JWT Bearer 토큰 자동 주입 인터셉터
/// - 모든 요청 헤더에 Authorization: Bearer {accessToken} 자동 첨부
/// - 401 응답 수신 시 리프레시 토큰으로 액세스 토큰 갱신 후 재요청
/// - 토큰 갱신 실패 시 UnauthorizedException 발생 (로그아웃 처리)
///
/// Copied from [authInterceptor].
@ProviderFor(authInterceptor)
final authInterceptorProvider = AutoDisposeProvider<AuthInterceptor>.internal(
  authInterceptor,
  name: r'authInterceptorProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$authInterceptorHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
typedef AuthInterceptorRef = AutoDisposeProviderRef<AuthInterceptor>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
