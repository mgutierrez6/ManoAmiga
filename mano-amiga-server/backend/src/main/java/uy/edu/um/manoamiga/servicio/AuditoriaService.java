package uy.edu.um.manoamiga.servicio;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import uy.edu.um.manoamiga.modelo.Auditoria;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Textos;

/**
 * Registro de auditoría (5.9): quién hizo qué, cuándo y desde dónde.
 * Nunca lanza: si la auditoría falla no debe romper la operación del usuario, pero queda en el log.
 * Nunca se guardan contraseñas, tokens ni datos personales completos.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final MongoTemplate mongo;

    public AuditoriaService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /**
     * @param resultado ok | fallo | denegado
     */
    public void registrar(String usuarioId, String accion, String recurso, Object recursoId, String resultado, String ip, String detalle) {
        try {
            Auditoria a = new Auditoria();
            a.setUsuarioId(Ids.valido(usuarioId) ? Ids.oid(usuarioId) : null);
            a.setAccion(accion);
            a.setRecurso(recurso);
            a.setRecursoId(recursoId == null ? null : (recursoId instanceof org.bson.types.ObjectId o ? o.toHexString() : String.valueOf(recursoId)));
            a.setResultado(resultado);
            a.setIp(ip);
            a.setDetalle(Textos.recortar(detalle, 300));
            a.setFecha(Instant.now().truncatedTo(ChronoUnit.MILLIS));
            mongo.insert(a);
        } catch (RuntimeException e) {
            log.error("No se pudo registrar auditoría ({}): {}", accion, e.getMessage());
        }
    }
}
