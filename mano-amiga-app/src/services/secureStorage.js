import * as SecureStore from 'expo-secure-store';

// Los tokens se guardan cifrados en el dispositivo (Keychain en iOS, Keystore en Android) — 5.2.
// AsyncStorage NO se usa para tokens porque guarda en texto plano.
const CLAVE_REFRESH = 'manoamiga.refreshToken';

export async function guardarRefreshToken(token) {
  await SecureStore.setItemAsync(CLAVE_REFRESH, token);
}

export async function leerRefreshToken() {
  return SecureStore.getItemAsync(CLAVE_REFRESH);
}

export async function borrarRefreshToken() {
  await SecureStore.deleteItemAsync(CLAVE_REFRESH);
}
