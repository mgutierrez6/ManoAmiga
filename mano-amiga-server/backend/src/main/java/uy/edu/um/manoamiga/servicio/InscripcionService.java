package uy.edu.um.manoamiga.servicio;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.modelo.Actividad;
import uy.edu.um.manoamiga.modelo.Asistencia;
import uy.edu.um.manoamiga.modelo.Inscripcion;
import uy.edu.um.manoamiga.modelo.Organizacion;
import uy.edu.um.manoamiga.modelo.Usuario;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Numeros;
import uy.edu.um.manoamiga.util.Pagina;

/** Inscripciones (R3 / R10) y gestión de participantes (R4 / R13). */
@Service
public class InscripcionService {

    private static final List<String> ACTIVAS = List.of("pendiente", "aceptada");

    private final MongoTemplate mongo;
    private final PermisosService permisos;
    private final AuditoriaService auditoria;
    private final NotificacionService notificaciones;
    private final PoblarService poblar;

    public InscripcionService(MongoTemplate mongo, PermisosService permisos, AuditoriaService auditoria,
            NotificacionService notificaciones, PoblarService poblar) {
        this.mongo = mongo;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.notificaciones = notificaciones;
        this.poblar = poblar;
    }

    private static Instant ahora() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /**
     * Reserva ATÓMICA del cupo (2.2): una sola operación en la base que suma 1 a "inscriptos" solo si
     * todavía es menor que "cupo". Si llegan 10 pedidos por el último lugar a la vez, entra uno solo.
     */
    private void reservarCupo(ObjectId actividadId) {
        Query q = new BasicQuery(new Document("_id", actividadId)
                .append("estado", "publicada")
                .append("$expr", new Document("$lt", List.of("$inscriptos", "$cupo"))));
        Actividad act = mongo.findAndModify(q, new Update().inc("inscriptos", 1), FindAndModifyOptions.options().returnNew(true), Actividad.class);
        if (act == null) throw ApiError.conflict("CUPO_LLENO", "No quedan cupos");
    }

    public void liberarCupo(ObjectId actividadId) {
        mongo.updateFirst(new Query(Criteria.where("_id").is(actividadId).and("inscriptos").gt(0)), new Update().inc("inscriptos", -1), Actividad.class);
    }

    private void notificarCoordinadores(ObjectId organizacionId, String titulo, String cuerpo, Map<String, String> data) {
        Query q = new Query(Criteria.where("rol").is(Usuario.COORDINADOR).and("organizacionId").is(organizacionId).and("activo").is(true));
        List<ObjectId> coords = mongo.find(q, Usuario.class).stream().map(Usuario::getId).toList();
        notificaciones.notificar(coords, titulo, cuerpo, data);
    }

    /** POST /actividades/:id/inscripciones — voluntario + ABAC + cupo. */
    public Dto.InscripcionCorta inscribir(UsuarioActual usuario, String actividadId, Ctx ctx) {
        ObjectId actId = Ids.oid(actividadId);
        ObjectId volId = Ids.oid(usuario.id());
        Actividad act = mongo.findById(actId, Actividad.class);
        if (act == null || !"publicada".equals(act.getEstado())) throw ApiError.notFound("ACTIVIDAD_NO_ENCONTRADA", "La actividad no existe");
        if (act.getFechaFin().isBefore(Instant.now())) throw ApiError.conflict("ACTIVIDAD_FINALIZADA", "La actividad ya terminó");

        permisos.assertABAC(usuario, act.getId(), act.getEtiquetas(), ctx); // un judicial no puede inscribirse a una actividad sensible

        Inscripcion previa = mongo.findOne(new Query(Criteria.where("actividadId").is(actId).and("voluntarioId").is(volId)), Inscripcion.class);
        if (previa != null && ACTIVAS.contains(previa.getEstado())) throw ApiError.conflict("YA_INSCRIPTO", "Ya estás inscripto en esta actividad");
        if (previa != null && "rechazada".equals(previa.getEstado())) throw ApiError.conflict("INSCRIPCION_RECHAZADA", "Tu inscripción a esta actividad fue rechazada");

        reservarCupo(actId);

        Inscripcion inscripcion;
        try {
            if (previa != null) {
                // Había cancelado antes: se reactiva la misma inscripción.
                mongo.updateFirst(new Query(Criteria.where("_id").is(previa.getId())),
                        new Update().set("estado", "pendiente").set("fecha", ahora()), Inscripcion.class);
                previa.setEstado("pendiente");
                inscripcion = previa;
            } else {
                Inscripcion nueva = new Inscripcion();
                nueva.setActividadId(actId);
                nueva.setVoluntarioId(volId);
                nueva.setOrganizacionId(act.getOrganizacionId());
                nueva.setFecha(ahora());
                inscripcion = mongo.insert(nueva);
            }
        } catch (DuplicateKeyException e) {
            liberarCupo(actId); // doble pedido simultáneo: devolvemos el cupo
            throw ApiError.conflict("YA_INSCRIPTO", "Ya estás inscripto en esta actividad");
        } catch (RuntimeException e) {
            liberarCupo(actId);
            throw e;
        }

        auditoria.registrar(usuario.id(), "inscripcion_crear", "inscripcion", inscripcion.getId(), "ok", ctx.ip(), null);
        notificarCoordinadores(act.getOrganizacionId(), "Nueva inscripción", "Hay una nueva inscripción en \"" + act.getTitulo() + "\"",
                Map.of("actividadId", actividadId));
        return new Dto.InscripcionCorta(Ids.str(inscripcion.getId()), actividadId, inscripcion.getEstado());
    }

    /** GET /usuarios/me/inscripciones — el id sale del token (RS2). */
    public Pagina<Dto.InscripcionPropia> misInscripciones(UsuarioActual usuario, String estado, int page, int limit) {
        Criteria filtro = Criteria.where("voluntarioId").is(Ids.oid(usuario.id()));
        if (estado != null) filtro = filtro.and("estado").is(estado);
        Query q = new Query(filtro).with(Sort.by(Sort.Direction.DESC, "fecha")).skip(Pagina.saltear(page, limit)).limit(limit);
        List<Inscripcion> lista = mongo.find(q, Inscripcion.class);
        long total = mongo.count(new Query(filtro), Inscripcion.class);
        Map<ObjectId, Actividad> acts = poblar.actividades(lista.stream().map(Inscripcion::getActividadId).toList());
        return new Pagina<>(lista.stream().map(i -> Dto.inscripcionPropia(i, acts.get(i.getActividadId()))).toList(), page, limit, total);
    }

    /** DELETE /inscripciones/:id — solo el titular (RS2, A1). */
    public void cancelar(UsuarioActual usuario, String id, Ctx ctx) {
        Inscripcion ins = mongo.findById(Ids.oid(id), Inscripcion.class);
        if (ins == null) throw ApiError.notFound("INSCRIPCION_NO_ENCONTRADA", "La inscripción no existe");
        permisos.assertTitular(usuario, ins.getVoluntarioId(), "inscripcion", id, ctx);

        if (!ACTIVAS.contains(ins.getEstado())) throw ApiError.conflict("NO_CANCELABLE", "La inscripción no se puede cancelar");
        if (mongo.exists(new Query(Criteria.where("inscripcionId").is(ins.getId())), Asistencia.class)) {
            throw ApiError.conflict("ASISTENCIA_REGISTRADA", "Ya tenés una asistencia registrada en esta actividad");
        }

        mongo.updateFirst(new Query(Criteria.where("_id").is(ins.getId())), new Update().set("estado", "cancelada"), Inscripcion.class);
        liberarCupo(ins.getActividadId());
        auditoria.registrar(usuario.id(), "inscripcion_cancelar", "inscripcion", ins.getId(), "ok", ctx.ip(), null);
    }

    /** GET /actividades/:id/inscripciones — coordinador de la org (RS3). Devuelve contacto (RS6 lo permite). */
    public List<Dto.InscripcionParaCoordinador> listarPorActividad(UsuarioActual usuario, String actividadId, String estado, Ctx ctx) {
        Actividad act = mongo.findById(Ids.oid(actividadId), Actividad.class);
        if (act == null) throw ApiError.notFound("ACTIVIDAD_NO_ENCONTRADA", "La actividad no existe");
        permisos.assertMismaOrganizacion(usuario, act.getOrganizacionId(), "actividad", act.getId(), ctx);

        Criteria filtro = Criteria.where("actividadId").is(act.getId());
        if (estado != null) filtro = filtro.and("estado").is(estado);
        List<Inscripcion> lista = mongo.find(new Query(filtro).with(Sort.by(Sort.Direction.ASC, "fecha")), Inscripcion.class);
        Map<ObjectId, Usuario> vols = poblar.usuarios(lista.stream().map(Inscripcion::getVoluntarioId).toList());
        return lista.stream().map(i -> Dto.inscripcionParaCoordinador(i, vols.get(i.getVoluntarioId()))).toList();
    }

    /**
     * PATCH /inscripciones/:id — aceptar o rechazar (R4). Coordinador de la org (RS3).
     * Transiciones válidas: pendiente -> aceptada | rechazada ; aceptada -> rechazada (si no tiene asistencia).
     */
    public Dto.InscripcionCorta cambiarEstado(UsuarioActual usuario, String id, String nuevoEstado, Ctx ctx) {
        Inscripcion ins = mongo.findById(Ids.oid(id), Inscripcion.class);
        if (ins == null) throw ApiError.notFound("INSCRIPCION_NO_ENCONTRADA", "La inscripción no existe");
        permisos.assertMismaOrganizacion(usuario, ins.getOrganizacionId(), "inscripcion", ins.getId(), ctx);

        Map<String, List<String>> validas = Map.of("pendiente", List.of("aceptada", "rechazada"), "aceptada", List.of("rechazada"));
        if (!validas.getOrDefault(ins.getEstado(), List.of()).contains(nuevoEstado)) {
            throw ApiError.conflict("TRANSICION_INVALIDA", "No se puede pasar de " + ins.getEstado() + " a " + nuevoEstado);
        }
        if ("rechazada".equals(nuevoEstado) && mongo.exists(new Query(Criteria.where("inscripcionId").is(ins.getId())), Asistencia.class)) {
            throw ApiError.conflict("ASISTENCIA_REGISTRADA", "La persona ya tiene una asistencia registrada");
        }

        // Cambio condicionado al estado anterior: si otro coordinador lo cambió al mismo tiempo, no se pisa.
        Inscripcion actualizada = mongo.findAndModify(
                new Query(Criteria.where("_id").is(ins.getId()).and("estado").is(ins.getEstado())),
                new Update().set("estado", nuevoEstado).set("updatedAt", ahora()),
                FindAndModifyOptions.options().returnNew(true),
                Inscripcion.class);
        if (actualizada == null) throw ApiError.conflict("TRANSICION_INVALIDA", "La inscripción cambió mientras la editabas; recargá");
        if ("rechazada".equals(nuevoEstado)) liberarCupo(ins.getActividadId());

        auditoria.registrar(usuario.id(), "inscripcion_" + nuevoEstado, "inscripcion", ins.getId(), "ok", ctx.ip(), null);
        Actividad act = mongo.findById(ins.getActividadId(), Actividad.class);
        notificaciones.notificar(List.of(ins.getVoluntarioId()), "Tu inscripción cambió",
                "Tu inscripción a \"" + (act != null ? act.getTitulo() : "la actividad") + "\" fue " + nuevoEstado,
                Map.of("inscripcionId", id));

        return new Dto.InscripcionCorta(Ids.str(actualizada.getId()), Ids.str(actualizada.getActividadId()), actualizada.getEstado());
    }

    /** Datos para el certificado (sin la condición judicial: el certificado es igual para todos). */
    public record DatosCertificado(String codigo, String voluntario, String organizacion, String actividad, Instant fecha, String direccion, double horas) {}

    /**
     * Acceso: titular (RS2) o coordinador de la org (RS3). Requiere al menos una asistencia validada.
     */
    public DatosCertificado datosCertificado(UsuarioActual usuario, String id, Ctx ctx) {
        Inscripcion ins = mongo.findById(Ids.oid(id), Inscripcion.class);
        if (ins == null) throw ApiError.notFound("INSCRIPCION_NO_ENCONTRADA", "La inscripción no existe");

        if (usuario.es(Usuario.VOLUNTARIO)) {
            permisos.assertTitular(usuario, ins.getVoluntarioId(), "certificado", id, ctx);
        } else {
            permisos.assertMismaOrganizacion(usuario, ins.getOrganizacionId(), "certificado", ins.getId(), ctx);
        }

        List<Asistencia> asistencias = mongo.find(new Query(Criteria.where("inscripcionId").is(ins.getId()).and("estado").is("validada")), Asistencia.class);
        if (asistencias.isEmpty()) throw ApiError.conflict("SIN_ASISTENCIA_VALIDADA", "Todavía no hay una asistencia validada para esta inscripción");
        double horas = Numeros.redondear(asistencias.stream().mapToDouble(Asistencia::getHoras).sum());

        Actividad act = mongo.findById(ins.getActividadId(), Actividad.class);
        Usuario vol = mongo.findById(ins.getVoluntarioId(), Usuario.class);
        Organizacion org = mongo.findById(ins.getOrganizacionId(), Organizacion.class);

        auditoria.registrar(usuario.id(), "certificado_descarga", "inscripcion", ins.getId(), "ok", ctx.ip(), null);
        return new DatosCertificado(
                Ids.str(ins.getId()),
                vol != null ? vol.getNombre() + " " + vol.getApellido() : "",
                org != null ? org.getNombre() : "",
                act != null ? act.getTitulo() : "",
                act != null ? act.getFechaInicio() : null,
                act != null && act.getDireccion() != null ? act.getDireccion() : "",
                horas);
    }
}
