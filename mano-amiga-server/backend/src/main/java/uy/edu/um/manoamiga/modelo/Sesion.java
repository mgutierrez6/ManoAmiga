package uy.edu.um.manoamiga.modelo;

import java.time.Instant;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Refresh token activo. Se guarda SOLO su HMAC-SHA256, nunca el token (5.2).
 * TTL: MongoDB borra sola la sesión cuando vence (expireAfterSeconds = 0).
 */
@Document("sesiones")
public class Sesion {

    @Id
    private ObjectId id;
    @Indexed(name = "userId_1")
    private ObjectId userId;
    @Indexed(name = "tokenHash_1", unique = true)
    private String tokenHash;
    /** Cadena de rotación de un mismo inicio de sesión. */
    private String familia;
    /** true = ya se usó; si vuelve a llegar, es un robo. */
    private boolean reemplazada = false;
    private String dispositivo;
    private Instant creadaEn;
    @Indexed(name = "expiraEn_1", expireAfterSeconds = 0)
    private Instant expiraEn;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public ObjectId getUserId() { return userId; }
    public void setUserId(ObjectId userId) { this.userId = userId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public String getFamilia() { return familia; }
    public void setFamilia(String familia) { this.familia = familia; }
    public boolean isReemplazada() { return reemplazada; }
    public void setReemplazada(boolean reemplazada) { this.reemplazada = reemplazada; }
    public String getDispositivo() { return dispositivo; }
    public void setDispositivo(String dispositivo) { this.dispositivo = dispositivo; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant creadaEn) { this.creadaEn = creadaEn; }
    public Instant getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }
}
