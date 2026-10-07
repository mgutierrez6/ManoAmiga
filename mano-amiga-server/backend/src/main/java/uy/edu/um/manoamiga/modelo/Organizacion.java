package uy.edu.um.manoamiga.modelo;

import java.time.Instant;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Organización social. Si se da de baja, sus coordinadores pierden el acceso en el siguiente pedido (RS5).
 */
@Document("organizaciones")
public class Organizacion {

    @Id
    private ObjectId id;
    @Indexed(name = "nombre_1", unique = true)
    private String nombre;
    private String contacto;
    private boolean convenioOSLA = false;
    private boolean activa = true;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }
    public boolean isConvenioOSLA() { return convenioOSLA; }
    public void setConvenioOSLA(boolean convenioOSLA) { this.convenioOSLA = convenioOSLA; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
