/// Stub for non-web platforms.
/// All operations are no-ops; flutter_secure_storage handles storage on mobile.
void webTokenStoreWrite(String key, String value) {}

String? webTokenStoreRead(String key) => null;

void webTokenStoreDelete(String key) {}

void webTokenStoreClear(List<String> keys) {}
