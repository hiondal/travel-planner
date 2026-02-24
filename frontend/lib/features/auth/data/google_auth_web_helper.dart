// Google Auth Web Helper — conditional import entry point
// Web: GIS 직접 호출 (People API 우회), 비-Web: null 반환 (google_sign_in 패키지 사용)
export 'google_auth_web_helper_stub.dart'
    if (dart.library.html) 'google_auth_web_helper_impl.dart';
