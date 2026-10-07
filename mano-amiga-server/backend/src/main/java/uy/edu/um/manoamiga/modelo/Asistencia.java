package uy.edu.um.manoamiga.modelo;

import java.time.Instant;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Registro verificable de horas (5.8). Las horas cumplidas se CALCULAN sumando asistencias validadas.
 */
@Document("asistencias")
public class Asistencia {

    @Id
    private ObjectId id;
    /** Una asistencia por inscripción: el mismo QR no sirve dos veces. */
    @Indexed(name = "inscripcionId_1", unique = true)
    private ObjectId inscripcionId;
    @Indexed(name = "actividadId_1")
    private ObjectId actividadId;
    @Indexed(name = "voluntarioId_1")
    private ObjectId voluntarioId;
    private ObjectId organizacionId;
    private Instant checkIn;
    private Instant checkOut;
    private double horas = 0;
    /** qr | manual */
    private String metodo;
    /** abierta | validada */
    private String estado = "abierta";
    private ObjectId validadaPor;
    private Instant validadaEn;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public ObjectId getInscripcionId() { return inscripcionId; }
    public void setInscripcionId(ObjectId inscripcionId) { this.inscripcionId = inscripcionId; }
    public ObjectId getActividadId() { return actividadId; }
    public void setActividadId(ObjectId actividadId) { this.actividadId = actividadId; }
    public ObjectId getVoluntarioId() { return voluntarioId; }
    public void setVoluntarioId(ObjectId voluntarioId) { this.voluntarioId = voluntarioId; }
    public ObjectId getOrganizacionId() { return organizacionId; }
    public void setOrganizacionId(ObjectId organizacionId) { this.organizacionId = organizacionId; }
    public Instant getCheckIn() { return checkIn; }
    public void setCheckIn(Instant checkIn) { this.checkIn = checkIn; }
    public Instant getCheckOut() { return checkOut; }
    public void setCheckOut(Instant checkOut) { this.checkOut = checkOut; }
    public double getHoras() { return horas; }
    public void setHoras(double horas) { this.horas = horas; }
    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public ObjectId getValidadaPor() { return validadaPor; }
    public void setValidadaPor(ObjectId validadaPor) { this.validadaPor = validadaPor; }
    public Instant getValidadaEn() { return validadaEn; }
    public void setValidadaEn(Instant validadaEn) { this.validadaEn = validadaEn; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
