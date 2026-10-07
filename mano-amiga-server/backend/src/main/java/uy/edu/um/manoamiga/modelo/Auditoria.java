package uy.edu.um.manoamiga.modelo;

import java.time.Instant;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Quién hizo qué, cuándo y desde dónde (5.9). Nunca contraseñas, tokens ni datos personales completos.
 */
@Document("auditoria")
@CompoundIndex(name = "usuarioId_1_fecha_-1", def = "{'usuarioId': 1, 'fecha': -1}")
public class Auditoria {

    @Id
    private ObjectId id;
    private ObjectId usuarioId;
    private String accion;
    private String recurso;
    private String recursoId;
    /** ok | fallo | denegado */
    private String resultado;
    private String ip;
    private String detalle;
    private Instant fecha;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public ObjectId getUsuarioId() { return usuarioId; }
    public void setUsuarioId(ObjectId usuarioId) { this.usuarioId = usuarioId; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getRecurso() { return recurso; }
    public void setRecurso(String recurso) { this.recurso = recurso; }
    public String getRecursoId() { return recursoId; }
    public void setRecursoId(String recursoId) { this.recursoId = recursoId; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
}
