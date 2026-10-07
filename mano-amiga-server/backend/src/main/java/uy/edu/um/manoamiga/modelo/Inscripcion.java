package uy.edu.um.manoamiga.modelo;

import java.time.Instant;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Inscripción de un voluntario a una actividad. organizacionId es copia de la de la actividad (chequeo RS3).
 */
@Document("inscripciones")
@CompoundIndexes({
    // Impide inscripciones duplicadas incluso si llegan dos pedidos a la vez.
    @CompoundIndex(name = "actividadId_1_voluntarioId_1", def = "{'actividadId': 1, 'voluntarioId': 1}", unique = true),
    @CompoundIndex(name = "voluntarioId_1_fecha_-1", def = "{'voluntarioId': 1, 'fecha': -1}")
})
public class Inscripcion {

    @Id
    private ObjectId id;
    private ObjectId actividadId;
    private ObjectId voluntarioId;
    private ObjectId organizacionId;
    /** pendiente | aceptada | rechazada | cancelada */
    private String estado = "pendiente";
    private Instant fecha;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public ObjectId getActividadId() { return actividadId; }
    public void setActividadId(ObjectId actividadId) { this.actividadId = actividadId; }
    public ObjectId getVoluntarioId() { return voluntarioId; }
    public void setVoluntarioId(ObjectId voluntarioId) { this.voluntarioId = voluntarioId; }
    public ObjectId getOrganizacionId() { return organizacionId; }
    public void setOrganizacionId(ObjectId organizacionId) { this.organizacionId = organizacionId; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
