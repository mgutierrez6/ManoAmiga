package uy.edu.um.manoamiga.servicio;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.modelo.Actividad;
import uy.edu.um.manoamiga.modelo.Asistencia;
import uy.edu.um.manoamiga.modelo.Inscripcion;
import uy.edu.um.manoamiga.modelo.Usuario;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Numeros;

/** Asistencia con QR o manual, validación y horas (R4 / R13, 5.8). */
@Service
public class AsistenciaService {

    /** Ventana en la que se puede generar/escanear el QR: desde 2 h antes del inicio hasta 2 h después del fin. */
    private static final Duration MARGEN = Duration.ofHours(2);

    private final MongoTemplate mongo;
    private final PermisosService permisos;
    private final AuditoriaService auditoria;
    private final QrService qr;
    private final NotificacionService notificaciones;
    private final PoblarService poblar;

    public AsistenciaService(MongoTemplate mongo, PermisosService permisos, AuditoriaService auditoria, QrService qr,
            NotificacionService notificaciones, PoblarService poblar) {
        this.mongo = mongo;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.qr = qr;
        this.notificaciones = notificaciones;
        this.poblar = poblar;
    }

    private static Instant ahora() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private static double horasEntre(Instant a, Instant b) {
        return Duration.between(a, b).toMillis() / 3_600_000.0;
    }

    private Actividad actividadDeMiOrg(UsuarioActual usuario, String actividadId, Ctx ctx) {
        Actividad act = mongo.findById(Ids.oid(actividadId), Actividad.class);
        if (act == null) throw ApiError.notFound("ACTIVIDAD_NO_ENCONTRADA", "La actividad no existe");
        permisos.assertMismaOrganizacion(usuario, act.getOrganizacionId(), "actividad", act.getId(), ctx); // RS3
        return act;
    }

    private static void assertEnHorario(Actividad act) {
        Instant ahora = Instant.now();
        if (ahora.isBefore(act.getFechaInicio().minus(MARGEN)) || ahora.isAfter(act.getFechaFin().plus(MARGEN))) {
            throw ApiError.conflict("FUERA_DE_HORARIO", "El registro de asistencia solo está disponible cerca del horario de la actividad");
        }
    }

    private Dto.AsistenciaDto dto(Asistencia a) {
        return Dto.asistencia(a, null);
    }

    /** GET /actividades/:id/qr — coordinador de la org. La app lo renueva cada ~50 s. */
    public Dto.Qr generarQR(UsuarioActual usuario, String actividadId, Ctx ctx) {
        Actividad act = actividadDeMiOrg(usuario, actividadId, ctx);
        if (!"publicada".equals(act.getEstado())) throw ApiError.conflict("ACTIVIDAD_NO_ACTIVA", "La actividad no está activa");
        assertEnHorario(act);
        return qr.generar(Ids.str(act.getId()));
    }

    /** POST /actividades/:id/asistencias — el voluntario escanea el QR (check-in). */
    public Dto.AsistenciaDto registrarConQR(UsuarioActual usuario, String actividadId, String codigo, Ctx ctx) {
        qr.verificar(codigo, actividadId); // firma + vencimiento + actividad correcta

        Actividad act = mongo.findById(Ids.oid(actividadId), Actividad.class);
        if (act == null || !"publicada".equals(act.getEstado())) throw ApiError.notFound("ACTIVIDAD_NO_ENCONTRADA", "La actividad no existe");
        assertEnHorario(act);

        Inscripcion ins = mongo.findOne(new Query(Criteria.where("actividadId").is(act.getId()).and("voluntarioId").is(Ids.oid(usuario.id()))), Inscripcion.class);
        if (ins == null || !"aceptada".equals(ins.getEstado())) {
            throw ApiError.forbidden("INSCRIPCION_NO_ACEPTADA", "Solo pueden registrar asistencia las personas aceptadas");
        }

        Asistencia a = new Asistencia();
        a.setInscripcionId(ins.getId());
        a.setActividadId(act.getId());
        a.setVoluntarioId(Ids.oid(usuario.id()));
        a.setOrganizacionId(act.getOrganizacionId());
        a.setCheckIn(ahora());
        a.setMetodo("qr");
        try {
            a = mongo.insert(a);
        } catch (DuplicateKeyException e) {
            throw ApiError.conflict("ASISTENCIA_YA_REGISTRADA", "Ya registraste tu asistencia en esta actividad");
        }
        auditoria.registrar(usuario.id(), "asistencia_checkin", "asistencia", a.getId(), "ok", ctx.ip(), null);
        return dto(a);
    }

    /** POST /inscripciones/:id/asistencia-manual — el coordinador registra y valida sin QR (R4: "QR o manual"). */
    public Dto.AsistenciaDto registrarManual(UsuarioActual usuario, String inscripcionId, Double horasPedidas, Ctx ctx) {
        Inscripcion ins = mongo.findById(Ids.oid(inscripcionId), Inscripcion.class);
        if (ins == null) throw ApiError.notFound("INSCRIPCION_NO_ENCONTRADA", "La inscripción no existe");
        permisos.assertMismaOrganizacion(usuario, ins.getOrganizacionId(), "inscripcion", ins.getId(), ctx); // RS3
        if (!"aceptada".equals(ins.getEstado())) throw ApiError.conflict("INSCRIPCION_NO_ACEPTADA", "La inscripción no está aceptada");

        Actividad act = mongo.findById(ins.getActividadId(), Actividad.class);
        double duracion = horasEntre(act.getFechaInicio(), act.getFechaFin());
        double horas = Numeros.redondear(horasPedidas != null ? horasPedidas : duracion);
        if (horas > duracion) {
            throw ApiError.badRequest("HORAS_INVALIDAS", "No puede superar la duración de la actividad (" + Numeros.redondear(duracion) + " h)");
        }

        Asistencia a = new Asistencia();
        a.setInscripcionId(ins.getId());
        a.setActividadId(ins.getActividadId());
        a.setVoluntarioId(ins.getVoluntarioId());
        a.setOrganizacionId(ins.getOrganizacionId());
        a.setCheckIn(act.getFechaInicio());
        a.setCheckOut(act.getFechaInicio().plusMillis(Math.round(horas * 3_600_000)));
        a.setHoras(horas);
        a.setMetodo("manual");
        a.setEstado("validada");
        a.setValidadaPor(Ids.oid(usuario.id()));
        a.setValidadaEn(ahora());
        try {
            a = mongo.insert(a);
        } catch (DuplicateKeyException e) {
            throw ApiError.conflict("ASISTENCIA_YA_REGISTRADA", "Esta persona ya tiene una asistencia registrada");
        }
        auditoria.registrar(usuario.id(), "asistencia_manual", "asistencia", a.getId(), "ok", ctx.ip(), horas + " h");
        return dto(a);
    }

    /** GET /actividades/:id/asistencias — coordinador de la org. */
    public List<Dto.AsistenciaDto> listarPorActividad(UsuarioActual usuario, String actividadId, Ctx ctx) {
        Actividad act = actividadDeMiOrg(usuario, actividadId, ctx);
        List<Asistencia> lista = mongo.find(new Query(Criteria.where("actividadId").is(act.getId())).with(Sort.by(Sort.Direction.ASC, "checkIn")), Asistencia.class);
        Map<ObjectId, Usuario> vols = poblar.usuarios(lista.stream().map(Asistencia::getVoluntarioId).toList());
        return lista.stream().map(a -> Dto.asistencia(a, vols.get(a.getVoluntarioId()))).toList();
    }

    /** PATCH /asistencias/:id/validar — registra la salida, calcula las horas y deja constancia (5.8). */
    public Dto.AsistenciaDto validar(UsuarioActual usuario, String asistenciaId, Ctx ctx) {
        Asistencia a = mongo.findById(Ids.oid(asistenciaId), Asistencia.class);
        if (a == null) throw ApiError.notFound("ASISTENCIA_NO_ENCONTRADA", "La asistencia no existe");
        permisos.assertMismaOrganizacion(usuario, a.getOrganizacionId(), "asistencia", a.getId(), ctx); // RS3
        if (!"abierta".equals(a.getEstado())) throw ApiError.conflict("ASISTENCIA_YA_VALIDADA", "La asistencia ya fue validada");

        Actividad act = mongo.findById(a.getActividadId(), Actividad.class);
        Instant inicio = a.getCheckIn().isAfter(act.getFechaInicio()) ? a.getCheckIn() : act.getFechaInicio();
        Instant ahora = ahora();
        Instant salida = ahora.isBefore(act.getFechaFin()) ? ahora : act.getFechaFin();
        double horas = Numeros.redondear(Math.max(0, horasEntre(inicio, salida)));

        // Condicionado a que siga "abierta": dos validaciones simultáneas no cuentan las horas dos veces.
        var r = mongo.updateFirst(
                new Query(Criteria.where("_id").is(a.getId()).and("estado").is("abierta")),
                new Update().set("checkOut", salida).set("horas", horas).set("estado", "validada")
                        .set("validadaPor", Ids.oid(usuario.id())).set("validadaEn", ahora).set("updatedAt", ahora),
                Asistencia.class);
        if (r.getModifiedCount() == 0) throw ApiError.conflict("ASISTENCIA_YA_VALIDADA", "La asistencia ya fue validada");
        a.setCheckOut(salida);
        a.setHoras(horas);
        a.setEstado("validada");
        a.setValidadaEn(ahora);

        auditoria.registrar(usuario.id(), "asistencia_validar", "asistencia", a.getId(), "ok", ctx.ip(), horas + " h");
        notificaciones.notificar(List.of(a.getVoluntarioId()), "Asistencia validada", "Se validaron " + horas + " horas en \"" + act.getTitulo() + "\"",
                Map.of("asistenciaId", asistenciaId));
        return dto(a);
    }

    /** Suma de horas VALIDADAS por voluntario (las horas se calculan, no se guardan: 4.2). */
    public Map<ObjectId, Double> horasValidadas(List<ObjectId> voluntarios) {
        if (voluntarios.isEmpty()) return Map.of();
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("voluntarioId").in(voluntarios).and("estado").is("validada")),
                Aggregation.group("voluntarioId").sum("horas").as("total"));
        Map<ObjectId, Double> res = new java.util.HashMap<>();
        for (Document d : mongo.aggregate(agg, "asistencias", Document.class).getMappedResults()) {
            Object total = d.get("total");
            res.put(d.getObjectId("_id"), total instanceof Number n ? n.doubleValue() : 0.0);
        }
        return res;
    }

    /** GET /usuarios/me/horas — solo voluntario judicial. */
    public Dto.Horas misHoras(UsuarioActual usuario) {
        if (!usuario.esJudicial()) throw ApiError.forbidden("NO_ES_JUDICIAL", "Este resumen es solo para voluntariado judicial");
        Usuario u = mongo.findById(Ids.oid(usuario.id()), Usuario.class);
        double cumplidas = Numeros.redondear(horasValidadas(List.of(Ids.oid(usuario.id()))).getOrDefault(Ids.oid(usuario.id()), 0.0));
        int asignadas = u != null ? u.getHorasAsignadas() : 0;
        return new Dto.Horas(asignadas, cumplidas, Numeros.redondear(Math.max(0, asignadas - cumplidas)));
    }
}
