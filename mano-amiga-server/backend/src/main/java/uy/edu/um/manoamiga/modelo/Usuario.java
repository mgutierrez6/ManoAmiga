package uy.edu.um.manoamiga.modelo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Usuario de la app. El rol y la organización se releen de la base en CADA pedido (RS5).
 * El documento es igual al que guarda la versión Node (colección "usuarios").
 */
@Document("usuarios")
public class Usuario {

    @Id
    private ObjectId id;
    private String nombre;
    private String apellido;
    @Indexed(name = "email_1", unique = true)
    private String email;
    /** Hash bcrypt. Nunca sale en una respuesta: los DTO no lo incluyen. */
    private String passwordHash;
    private String telefono;
    /** voluntario | coordinador | admin */
    private String rol = "voluntario";
    private ObjectId organizacionId;
    /** Atributo ABAC: NO es un rol aparte. Solo lo cambia el administrador (R7). */
    private boolean esJudicial = false;
    private int horasAsignadas = 0;
    private boolean activo = true;
    private Preferencias preferencias = new Preferencias();
    private List<String> pushTokens = new ArrayList<>();
    /** RS5: se incrementa al revocar; los access tokens con otra versión dejan de valer al instante. */
    private int versionSesion = 0;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public ObjectId getOrganizacionId() { return organizacionId; }
    public void setOrganizacionId(ObjectId organizacionId) { this.organizacionId = organizacionId; }
    public boolean isEsJudicial() { return esJudicial; }
    public void setEsJudicial(boolean esJudicial) { this.esJudicial = esJudicial; }
    public int getHorasAsignadas() { return horasAsignadas; }
    public void setHorasAsignadas(int horasAsignadas) { this.horasAsignadas = horasAsignadas; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Preferencias getPreferencias() { return preferencias; }
    public void setPreferencias(Preferencias preferencias) { this.preferencias = preferencias; }
    public List<String> getPushTokens() { return pushTokens; }
    public void setPushTokens(List<String> pushTokens) { this.pushTokens = pushTokens; }
    public int getVersionSesion() { return versionSesion; }
    public void setVersionSesion(int versionSesion) { this.versionSesion = versionSesion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static final String VOLUNTARIO = "voluntario";
    public static final String COORDINADOR = "coordinador";
    public static final String ADMIN = "admin";

    /** Preferencias del perfil (R8). */
    public static class Preferencias {
        private String tema = "sistema";
        public String getTema() { return tema; }
        public void setTema(String tema) { this.tema = tema; }
    }
}
