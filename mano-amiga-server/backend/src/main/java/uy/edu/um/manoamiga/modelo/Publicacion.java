package uy.edu.um.manoamiga.modelo;

import java.time.Instant;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Publicación del feed de un proyecto (actividad o campaña).
 */
@Document("publicaciones")
@CompoundIndex(name = "proyectoId_1_fecha_-1", def = "{'proyectoId': 1, 'fecha': -1}")
public class Publicacion {

    @Id
    private ObjectId id;
    private ObjectId proyectoId;
    /** actividad | campana */
    private String proyectoTipo;
    private ObjectId organizacionId;
    private ObjectId autorId;
    /** post | convocatoria */
    private String tipo = "post";
    private String contenido;
    private Instant fecha;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public ObjectId getProyectoId() { return proyectoId; }
    public void setProyectoId(ObjectId proyectoId) { this.proyectoId = proyectoId; }
    public String getProyectoTipo() { return proyectoTipo; }
    public void setProyectoTipo(String proyectoTipo) { this.proyectoTipo = proyectoTipo; }
    public ObjectId getOrganizacionId() { return organizacionId; }
    public void setOrganizacionId(ObjectId organizacionId) { this.organizacionId = organizacionId; }
    public ObjectId getAutorId() { return autorId; }
    public void setAutorId(ObjectId autorId) { this.autorId = autorId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
}
