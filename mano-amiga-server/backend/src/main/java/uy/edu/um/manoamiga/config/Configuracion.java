package uy.edu.um.manoamiga.config;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Parámetros y secretos de la API (5.6 Manejo de secretos).
 * Ningún secreto está escrito en el código: vienen del .env o de las variables de entorno.
 * Si falta alguno, o es demasiado corto, la API NO arranca (mejor fallar al inicio que correr insegura).
 */
@Component
public class Configuracion {

    public static final String JWT_ISSUER = "mano-amiga-api";
    public static final String JWT_AUDIENCE = "mano-amiga-app";
    public static final long ACCESS_TOKEN_MINUTOS = 15;
    public static final long REFRESH_TOKEN_DIAS = 7;
    public static final long QR_TTL_SEGUNDOS = 60;

    private final byte[] jwtSecret;
    private final byte[] jwtRefreshSecret;
    private final byte[] qrSecret;
    private final String expoAccessToken;
    private final int bcryptCosto;
    private final boolean pushHabilitado;

    public Configuracion(
            @Value("${manoamiga.jwt-secret:}") String jwtSecret,
            @Value("${manoamiga.jwt-refresh-secret:}") String jwtRefreshSecret,
            @Value("${manoamiga.qr-secret:}") String qrSecret,
            @Value("${manoamiga.expo-access-token:}") String expoAccessToken,
            @Value("${manoamiga.bcrypt-costo:12}") int bcryptCosto,
            @Value("${manoamiga.push-habilitado:true}") boolean pushHabilitado,
            Environment env) {
        this.jwtSecret = secreto("JWT_SECRET", jwtSecret);
        this.jwtRefreshSecret = secreto("JWT_REFRESH_SECRET", jwtRefreshSecret);
        this.qrSecret = secreto("QR_SECRET", qrSecret);
        this.expoAccessToken = expoAccessToken;
        this.bcryptCosto = bcryptCosto;
        this.pushHabilitado = pushHabilitado;
        String mongo = env.getProperty("spring.data.mongodb.uri", "");
        if (mongo.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno MONGO_URI. Revisá tu archivo .env (copialo de .env.example).");
        }
    }

    // Un secreto corto se puede adivinar por fuerza bruta: exigimos al menos 32 caracteres (256 bits para HS256).
    private static byte[] secreto(String nombre, String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + nombre + ". Revisá tu archivo .env (copialo de .env.example).");
        }
        if (valor.length() < 32) {
            throw new IllegalStateException(nombre + " debe tener al menos 32 caracteres. Generalo con: openssl rand -hex 48");
        }
        // Mismos bytes que usa la versión Node (el texto en UTF-8): los tokens son compatibles entre las dos.
        return valor.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] jwtSecret() { return jwtSecret.clone(); }
    public byte[] jwtRefreshSecret() { return jwtRefreshSecret.clone(); }
    public byte[] qrSecret() { return qrSecret.clone(); }
    public String expoAccessToken() { return expoAccessToken; }
    public int bcryptCosto() { return bcryptCosto; }
    public boolean pushHabilitado() { return pushHabilitado; }
}
