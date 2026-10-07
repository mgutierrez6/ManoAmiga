package uy.edu.um.manoamiga.servicio;

import java.util.List;
import java.util.Objects;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.modelo.Etiqueta;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Ids;

/**
 * Controles de acceso a nivel de recurso (los que la matriz por rol no puede ver).
 */
@Service
public class PermisosService {

    private final MongoTemplate mongo;
    private final AuditoriaService auditoria;

    public PermisosService(MongoTemplate mongo, AuditoriaService auditoria) {
        this.mongo = mongo;
        this.auditoria = auditoria;
    }

    /**
     * RS3 · Aislamiento entre organizaciones (A2, CWE-862).
     * El coordinador solo opera sobre recursos de SU organización. La organización del usuario viene de la
     * base (AutenticacionService), nunca del body ni de la URL.
     */
    public void assertMismaOrganizacion(UsuarioActual usuario, ObjectId orgRecurso, String recurso, ObjectId recursoId, Ctx ctx) {
        String org = Ids.str(orgRecurso);
        if (usuario.organizacionId() == null || org == null || !org.equals(usuario.organizacionId())) {
            auditoria.registrar(usuario.id(), "acceso_denegado_organizacion", recurso, recursoId, "denegado", ctx.ip(), null);
            throw ApiError.forbidden("OTRA_ORGANIZACION", "El recurso pertenece a otra organización");
        }
    }

    /** RS2 · Titularidad (A1, IDOR / CWE-639): el voluntario solo accede a lo suyo. */
    public void assertTitular(UsuarioActual usuario, ObjectId idDueno, String recurso, String recursoId, Ctx ctx) {
        if (!Objects.equals(Ids.str(idDueno), usuario.id())) {
            auditoria.registrar(usuario.id(), "acceso_denegado_titularidad", recurso, recursoId, "denegado", ctx.ip(), null);
            throw ApiError.forbidden("NO_ES_TITULAR", "El recurso no te pertenece");
        }
    }

    /** Ids de las etiquetas marcadas como sensibles (base de la regla ABAC). */
    public List<ObjectId> idsEtiquetasSensibles() {
        Query q = new Query(Criteria.where("sensible").is(true));
        q.fields().include("_id");
        return mongo.find(q, Etiqueta.class).stream().map(Etiqueta::getId).toList();
    }

    /** ABAC · Un voluntario judicial no accede a actividades con alguna etiqueta sensible. */
    public boolean puedeVerActividad(UsuarioActual usuario, List<ObjectId> etiquetasActividad) {
        if (usuario == null || !usuario.esJudicial()) return true;
        List<ObjectId> sensibles = idsEtiquetasSensibles();
        return etiquetasActividad == null || etiquetasActividad.stream().noneMatch(sensibles::contains);
    }

    public void assertABAC(UsuarioActual usuario, ObjectId actividadId, List<ObjectId> etiquetasActividad, Ctx ctx) {
        if (!puedeVerActividad(usuario, etiquetasActividad)) {
            auditoria.registrar(usuario.id(), "acceso_denegado_abac", "actividad", actividadId, "denegado", ctx.ip(), null);
            throw ApiError.forbidden("ACTIVIDAD_RESTRINGIDA", "No podés acceder a esta actividad");
        }
    }
}
