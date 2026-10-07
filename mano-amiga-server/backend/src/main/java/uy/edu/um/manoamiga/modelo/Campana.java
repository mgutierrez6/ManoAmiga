package uy.edu.um.manoamiga.modelo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Campaña de donaciones con sus puntos de acopio (ubicaciones institucionales, RS7).
 */
@Document("campanas")
@CompoundIndex(name = "puntos.ubicacion_2dsphere", def = "{'puntos.ubicacion': '2dsphere'}")
public class Campana {

    @Id
    private ObjectId id;
    @Indexed(name = "organizacionId_1")
    private ObjectId organizacionId;
    private ObjectId creadaPor;
    private String titulo;
    private String descripcion;
    private String objetivo;
    private List<PuntoAcopio> puntos = new ArrayList<>();
    private Instant fechaInicio;
    private Instant fechaFin;
    /** activa | finalizada */
    private String estado = "activa";
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public ObjectId getOrganizacionId() { return organizacionId; }
    public void setOrganizacionId(ObjectId organizacionId) { this.organizacionId = organizacionId; }
    public ObjectId getCreadaPor() { return creadaPor; }
    public void setCreadaPor(ObjectId creadaPor) { this.creadaPor = creadaPor; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public List<PuntoAcopio> getPuntos() { return puntos; }
    public void setPuntos(List<PuntoAcopio> puntos) { this.puntos = puntos; }
    public Instant getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Instant fechaInicio) { this.fechaInicio = fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
    public void setFechaFin(Instant fechaFin) { this.fechaFin = fechaFin; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
