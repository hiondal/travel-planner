import 'dart:js_interop';

@JS('tpStorageSet')
external void _set(JSString key, JSString value);

@JS('tpStorageGet')
external JSString _get(JSString key);

@JS('tpStorageRemove')
external void _remove(JSString key);

/// Web implementation — uses localStorage via JS interop.
void webTokenStoreWrite(String key, String value) {
  _set(key.toJS, value.toJS);
}

String? webTokenStoreRead(String key) {
  final val = _get(key.toJS).toDart;
  return val.isEmpty ? null : val;
}

void webTokenStoreDelete(String key) {
  _remove(key.toJS);
}

void webTokenStoreClear(List<String> keys) {
  for (final key in keys) {
    _remove(key.toJS);
  }
}
