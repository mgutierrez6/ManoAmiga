package uy.edu.um.manoamiga.seguridad;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import uy.edu.um.manoamiga.config.Configuracion;

/**
 * Tokens de sesión (5.2). Son compatibles con los de la versión Node (mismo algoritmo, claims y secretos).
 *
 * - Access token (JWT HS256, 15 min): SOLO lleva el id del usuario (sub) y la versión de sesión (ver).
 *   El rol NO va en el token: se relee de la base en cada pedido (RS5).
 * - Refresh token: 64 bytes aleatorios (opaco, no es un JWT). En la base se guarda solo su HMAC-SHA256.
 * - Reauth token (RS9, 5 min): otra audiencia, así no sirve como access token (ni al revés).
 */
@Service
public class TokenService {

    public static final String AUDIENCIA_REAUTH = "mano-amiga-reauth";
    public static final long REAUTH_MINUTOS = 5;
    private static final String ALGORITMO = "HS256";

    /** Reauth: el token y cuándo vence (la app lo usa para no pedir la contraseña de nuevo antes de tiempo). */
    public record Reauth(String reauthToken, Instant expiraEn) {}

    private final SecretKey claveJwt;
    private final byte[] secretoRefresh;
    private final SecureRandom azar = new SecureRandom();

    public TokenService(Configuracion config) {
        this.claveJwt = Keys.hmacShaKeyFor(config.jwtSecret());
        this.secretoRefresh = config.jwtRefreshSecret();
    }

    public String firmarAccessToken(String usuarioId, int versionSesion) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuarioId)
                .claim("typ", "access")
                .claim("ver", versionSesion)
                .issuer(Configuracion.JWT_ISSUER)
                .audience().add(Configuracion.JWT_AUDIENCE).and()
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(Configuracion.ACCESS_TOKEN_MINUTOS, ChronoUnit.MINUTES)))
                .signWith(claveJwt, Jwts.SIG.HS256)
                .compact();
    }

    /** Verifica firma, algoritmo, emisor, audiencia y vencimiento. Lanza JwtException si algo no cierra. */
    public Claims verificarAccessToken(String token) {
        Claims c = verificar(token, claveJwt, Configuracion.JWT_AUDIENCE);
        if (!"access".equals(c.get("typ", String.class)) || c.getSubject() == null) throw new JwtException("tipo inválido");
        return c;
    }

    public Reauth firmarReauthToken(String usuarioId) {
        Instant ahora = Instant.now();
        Instant vence = ahora.plus(REAUTH_MINUTOS, ChronoUnit.MINUTES);
        String token = Jwts.builder()
                .subject(usuarioId)
                .claim("typ", "reauth")
                .issuer(Configuracion.JWT_ISSUER)
                .audience().add(AUDIENCIA_REAUTH).and()
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(vence))
                .signWith(claveJwt, Jwts.SIG.HS256)
                .compact();
        return new Reauth(token, vence.truncatedTo(ChronoUnit.MILLIS));
    }

    public Claims verificarReauthToken(String token) {
        Claims c = verificar(token, claveJwt, AUDIENCIA_REAUTH);
        if (!"reauth".equals(c.get("typ", String.class))) throw new JwtException("tipo inválido");
        return c;
    }

    /**
     * Verificación común. Fijamos el algoritmo (HS256): un token con "alg: none" o con otro algoritmo
     * se rechaza aunque la firma sea válida (evita los ataques de confusión de algoritmo).
     */
    static Claims verificar(String token, SecretKey clave, String audiencia) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(Configuracion.JWT_ISSUER)
                .requireAudience(audiencia)
                .build()
                .parseSignedClaims(token);
        if (!ALGORITMO.equals(jws.getHeader().getAlgorithm())) throw new JwtException("algoritmo no permitido");
        return jws.getPayload();
    }

    public String generarRefreshToken() {
        byte[] bytes = new byte[64];
        azar.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String hashRefreshToken(String token) {
        return hmacHex(secretoRefresh, token);
    }

    static String hmacHex(byte[] secreto, String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("No se pudo calcular el HMAC", e);
        }
    }
}
