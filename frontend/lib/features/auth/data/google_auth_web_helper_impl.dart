import 'dart:js_interop';

@JS('triggerGoogleSignIn')
external JSPromise<JSString> _triggerGoogleSignIn(JSString clientId);

/// Web implementation — calls GIS initTokenClient directly via JS interop.
/// Returns an access_token (ya29.xxx) on success, null on cancel/failure.
Future<String?> triggerGoogleSignInWeb(String clientId) async {
  try {
    final result = await _triggerGoogleSignIn(clientId.toJS).toDart;
    return result.toDart;
  } catch (_) {
    return null;
  }
}
