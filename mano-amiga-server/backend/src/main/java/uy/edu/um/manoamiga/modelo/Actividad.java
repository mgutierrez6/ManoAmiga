package uy.edu.um.manoamiga.modelo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Actividad de voluntariado. La ubicación es INSTITUCIONAL (RS7), nunca la de un voluntario.
 * OJO: GeoJsonPoint guarda [longitud, latitud], igual que la versión Node.
 */
@Document("actividades")
public class Actividad {

    @Id
    private ObjectId id;
    private String titulo;
    private String descripcion;
    @Indexed(name = "organizacionId_1")
    private ObjectId organizacionId;
    private ObjectId creadaPor;
    @Indexed(name = "etiquetas_1")
    private List<ObjectId> etiquetas = new ArrayList<>();
    @GeoSpatialIndexed(name = "ubicacion_2dsphere", type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint ubicacion;
    private String direccion;
    private Instant fechaInicio;
    private Instant fechaFin;
    private int cupo = 0;
    /** Contador para reservar el cupo de forma atómica (2.2). */
    private int inscriptos = 0;
    /** publicada | cancelada | finalizada */
    private String estado = "publicada";
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public ObjectId getOrganizacionId() { return organizacionId; }
    public void setOrganizacionId(ObjectId organizacionId) { this.organizacionId = organizacionId; }
    public ObjectId getCreadaPor() { return creadaPor; }
    public void setCreadaPor(ObjectId creadaPor) { this.creadaPor = creadaPor; }
    public List<ObjectId> getEtiquetas() { return etiquetas; }
    public void setEtiquetas(List<ObjectId> etiquetas) { this.etiquetas = etiquetas; }
    public GeoJsonPoint getUbicacion() { return ubicacion; }
    public void setUbicacion(GeoJsonPoint ubicacion) { this.ubicacion = ubicacion; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public Instant getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Instant fechaInicio) { this.fechaInicio = fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
    public void setFechaFin(Instant fechaFin) { this.fechaFin = fechaFin; }
    public int getCupo() { return cupo; }
    public void setCupo(int cupo) { this.cupo = cupo; }
    public int getInscriptos() { return inscriptos; }
    public void setInscriptos(int inscriptos) { this.inscriptos = inscriptos; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
