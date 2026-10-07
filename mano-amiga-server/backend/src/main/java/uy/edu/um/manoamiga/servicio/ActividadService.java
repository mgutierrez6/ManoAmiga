package uy.edu.um.manoamiga.servicio;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.modelo.Actividad;
import uy.edu.um.manoamiga.modelo.Etiqueta;
import uy.edu.um.manoamiga.modelo.Usuario;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Pagina;
import uy.edu.um.manoamiga.web.entrada.ActividadEntrada;

/** Actividades (R2 / R9 en la tabla de la profe). */
@Service
public class ActividadService {

    private static final double RADIO_TIERRA_KM = 6378.1;

    private final MongoTemplate mongo;
    private final PermisosService permisos;
    private final AuditoriaService auditoria;
    private final PoblarService poblar;

    public ActividadService(MongoTemplate mongo, PermisosService permisos, AuditoriaService auditoria, PoblarService poblar) {
        this.mongo = mongo;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.poblar = poblar;
    }

    /** GeoJSON: [longitud, latitud]. */
    static GeoJsonPoint aPunto(ActividadEntrada.Ubicacion u) {
        return new GeoJsonPoint(u.lng(), u.lat());
    }

    private void validarEtiquetas(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        long existentes = mongo.count(new Query(Criteria.where("_id").in(Ids.oids(ids))), Etiqueta.class);
        if (existentes != ids.size()) throw ApiError.badRequest("ETIQUETA_INEXISTENTE", "Alguna etiqueta no existe");
    }

    /** Arma los DTOs cargando en bloque los nombres de organizaciones y etiquetas (populate). */
    List<Dto.ActividadDto> aDtos(List<Actividad> lista) {
        Map<ObjectId, String> orgs = poblar.nombresOrganizaciones(lista.stream().map(Actividad::getOrganizacionId).toList());
        Map<ObjectId, String> tags = poblar.nombresEtiquetas(lista.stream().flatMap(a -> a.getEtiquetas().stream()).toList());
        return lista.stream().map(a -> Dto.actividad(a, orgs, tags)).toList();
    }

    /**
     * Listado con filtro ABAC DENTRO de la consulta (5.3): las actividades sensibles nunca salen del servidor
     * para un voluntario judicial.
     */
    public Pagina<Dto.ActividadDto> listar(UsuarioActual usuario, String etiqueta, Double lat, Double lng, double radio, boolean mias, int page, int limit) {
        List<Criteria> condiciones = new ArrayList<>();

        if (mias) {
            // Coordinador: todas las actividades de SU organización (también canceladas o finalizadas).
            if (!usuario.es(Usuario.COORDINADOR)) {
                throw ApiError.forbidden("ROL_NO_PERMITIDO", "Solo los coordinadores pueden ver las actividades de su organización");
            }
            condiciones.add(Criteria.where("organizacionId").is(Ids.oid(usuario.organizacionId())));
        } else {
            condiciones.add(Criteria.where("estado").is("publicada").and("fechaFin").gte(Instant.now()));
        }

        if (etiqueta != null) condiciones.add(Criteria.where("etiquetas").is(Ids.oid(etiqueta)));

        if (usuario.esJudicial()) {
            condiciones.add(Criteria.where("etiquetas").nin(permisos.idsEtiquetasSensibles()));
        }

        if (lat != null && lng != null) {
            // $geoWithin + $centerSphere: radio en radianes = km / radio de la Tierra.
            condiciones.add(Criteria.where("ubicacion").withinSphere(new Circle(new Point(lng, lat), radio / RADIO_TIERRA_KM)));
        }

        Criteria filtro = new Criteria().andOperator(condiciones);
        Query q = new Query(filtro).with(Sort.by(Sort.Direction.ASC, "fechaInicio")).skip(Pagina.saltear(page, limit)).limit(limit);
        List<Actividad> lista = mongo.find(q, Actividad.class);
        long total = mongo.count(new Query(filtro), Actividad.class);
        return new Pagina<>(aDtos(lista), page, limit, total);
    }

    public Dto.ActividadDto obtener(UsuarioActual usuario, String id, Ctx ctx) {
        Actividad act = mongo.findById(Ids.oid(id), Actividad.class);
        if (act == null) throw ApiError.notFound("ACTIVIDAD_NO_ENCONTRADA", "La actividad no existe");

        boolean esDeSuOrg = usuario.es(Usuario.COORDINADOR) && Ids.str(act.getOrganizacionId()).equals(usuario.organizacionId());
        if (!"publicada".equals(act.getEstado()) && !esDeSuOrg) throw ApiError.notFound("ACTIVIDAD_NO_ENCONTRADA", "La actividad no existe");

        permisos.assertABAC(usuario, act.getId(), act.getEtiquetas(), ctx);
        return aDtos(List.of(act)).get(0);
    }

    public Dto.ActividadDto crear(UsuarioActual usuario, ActividadEntrada.Crear datos, Ctx ctx) {
        validarEtiquetas(datos.etiquetas());
        Actividad act = new Actividad();
        act.setTitulo(datos.titulo());
        act.setDescripcion(datos.descripcion());
        act.setEtiquetas(new ArrayList<>(Ids.oids(datos.etiquetas())));
        act.setUbicacion(aPunto(datos.ubicacion()));
        act.setDireccion(datos.direccion());
        act.setFechaInicio(datos.fechaInicio().truncatedTo(ChronoUnit.MILLIS));
        act.setFechaFin(datos.fechaFin().truncatedTo(ChronoUnit.MILLIS));
        act.setCupo(datos.cupo());
        act.setOrganizacionId(Ids.oid(usuario.organizacionId())); // del usuario autenticado, nunca del body
        act.setCreadaPor(Ids.oid(usuario.id()));
        act = mongo.insert(act);
        auditoria.registrar(usuario.id(), "actividad_crear", "actividad", act.getId(), "ok", ctx.ip(), null);
        return obtener(usuario, Ids.str(act.getId()), ctx);
    }

    public Dto.ActividadDto actualizar(UsuarioActual usuario, String id, ActividadEntrada.Actualizar datos, Ctx ctx) {
        if (datos.vacio()) throw ApiError.validacion(List.of(new ApiError.Detalle("body", "tiene que incluir al menos un campo")));
        Actividad act = mongo.findById(Ids.oid(id), Actividad.class);
        if (act == null) throw ApiError.notFound("ACTIVIDAD_NO_ENCONTRADA", "La actividad no existe");
        permisos.assertMismaOrganizacion(usuario, act.getOrganizacionId(), "actividad", act.getId(), ctx); // RS3
        if (!"publicada".equals(act.getEstado())) throw ApiError.conflict("ACTIVIDAD_NO_EDITABLE", "Solo se pueden editar actividades publicadas");

        if (datos.etiquetas() != null) validarEtiquetas(datos.etiquetas());
        Instant inicio = datos.fechaInicio() != null ? datos.fechaInicio() : act.getFechaInicio();
        Instant fin = datos.fechaFin() != null ? datos.fechaFin() : act.getFechaFin();
        if (!fin.isAfter(inicio)) throw ApiError.badRequest("FECHAS_INVALIDAS", "La fecha de fin debe ser posterior a la de inicio");
        if (datos.cupo() != null && datos.cupo() < act.getInscriptos()) {
            throw ApiError.conflict("CUPO_MENOR_A_INSCRIPTOS", "Ya hay " + act.getInscriptos() + " inscriptos; el cupo no puede ser menor");
        }

        // Lista blanca explícita de campos editables (mass assignment). Se actualizan SOLO esos campos
        // ($set), así no se pisa el contador de inscriptos si alguien se inscribe al mismo tiempo.
        Update u = new Update();
        if (datos.titulo() != null) u.set("titulo", datos.titulo());
        if (datos.descripcion() != null) u.set("descripcion", datos.descripcion());
        if (datos.etiquetas() != null) u.set("etiquetas", Ids.oids(datos.etiquetas()));
        if (datos.direccion() != null) u.set("direccion", datos.direccion());
        if (datos.fechaInicio() != null) u.set("fechaInicio", datos.fechaInicio().truncatedTo(ChronoUnit.MILLIS));
        if (datos.fechaFin() != null) u.set("fechaFin", datos.fechaFin().truncatedTo(ChronoUnit.MILLIS));
        if (datos.cupo() != null) u.set("cupo", datos.cupo());
        if (datos.ubicacion() != null) u.set("ubicacion", aPunto(datos.ubicacion()));
        u.set("updatedAt", Instant.now().truncatedTo(ChronoUnit.MILLIS));
        mongo.updateFirst(new Query(Criteria.where("_id").is(act.getId())), u, Actividad.class);

        auditoria.registrar(usuario.id(), "actividad_editar", "actividad", act.getId(), "ok", ctx.ip(), null);
        return obtener(usuario, id, ctx);
    }

    /** "Eliminar" = cancelar: se conserva la actividad para no perder el historial de asistencias y horas. */
    public void eliminar(UsuarioActual usuario, String id, Ctx ctx) {
        Actividad act = mongo.findById(Ids.oid(id), Actividad.class);
        if (act == null) throw ApiError.notFound("ACTIVIDAD_NO_ENCONTRADA", "La actividad no existe");
        permisos.assertMismaOrganizacion(usuario, act.getOrganizacionId(), "actividad", act.getId(), ctx); // RS3
        mongo.updateFirst(new Query(Criteria.where("_id").is(act.getId())), new Update().set("estado", "cancelada"), Actividad.class);
        auditoria.registrar(usuario.id(), "actividad_cancelar", "actividad", act.getId(), "ok", ctx.ip(), null);
    }

    public List<Dto.EtiquetaDto> listarEtiquetas() {
        return mongo.find(new Query().with(Sort.by("nombre")), Etiqueta.class).stream().map(Dto::etiqueta).toList();
    }
}
