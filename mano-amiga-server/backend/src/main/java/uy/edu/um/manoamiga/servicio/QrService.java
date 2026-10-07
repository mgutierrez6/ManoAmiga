package uy.edu.um.manoamiga.servicio;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import uy.edu.um.manoamiga.config.Configuracion;
import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.error.ApiError;

/**
 * QR de asistencia (2.3, 5.8): un código firmado con QR_SECRET (distinto del de las sesiones) que incluye
 * la actividad y vence a los 60 segundos. No se puede falsificar ni reutilizar para otra actividad.
 */
@Service
public class QrService {

    private static final String AUDIENCIA_QR = "mano-amiga-qr";

    private final SecretKey clave;

    public QrService(Configuracion config) {
        this.clave = Keys.hmacShaKeyFor(config.qrSecret());
    }

    public Dto.Qr generar(String actividadId) {
        Instant ahora = Instant.now();
        Instant vence = ahora.plusSeconds(Configuracion.QR_TTL_SEGUNDOS);
        String token = Jwts.builder()
                .claim("act", actividadId)
                .claim("typ", "qr")
                .issuer(Configuracion.JWT_ISSUER)
                .audience().single(AUDIENCIA_QR)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(vence))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
        return new Dto.Qr(token, vence.truncatedTo(ChronoUnit.MILLIS));
    }

    /** Verifica firma, vencimiento y que el QR sea de ESTA actividad. */
    public void verificar(String token, String actividadId) {
        Claims c;
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(Configuracion.JWT_ISSUER)
                    .requireAudience(AUDIENCIA_QR)
                    .build()
                    .parseSignedClaims(token);
            if (!"HS256".equals(jws.getHeader().getAlgorithm())) throw new JwtException("algoritmo no permitido");
            c = jws.getPayload();
        } catch (ExpiredJwtException e) {
            throw ApiError.badRequest("QR_VENCIDO", "El código QR venció, escaneá el nuevo");
        } catch (JwtException | IllegalArgumentException e) {
            throw ApiError.badRequest("QR_INVALIDO", "El código QR no es válido");
        }
        if (!"qr".equals(c.get("typ", String.class)) || !actividadId.equals(c.get("act", String.class))) {
            throw ApiError.badRequest("QR_INVALIDO", "El código QR no corresponde a esta actividad");
        }
    }
}
