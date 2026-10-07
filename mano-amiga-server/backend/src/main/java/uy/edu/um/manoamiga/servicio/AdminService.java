package uy.edu.um.manoamiga.servicio;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
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
import uy.edu.um.manoamiga.modelo.Auditoria;
import uy.edu.um.manoamiga.modelo.Campana;
import uy.edu.um.manoamiga.modelo.Organizacion;
import uy.edu.um.manoamiga.modelo.Usuario;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Numeros;
import uy.edu.um.manoamiga.util.Pagina;
import uy.edu.um.manoamiga.web.entrada.AdminEntrada;

/**
 * Panel de administración (R7 / R15). Toda acción de escritura:
 *  - la pide un admin (matriz, RS1) con reautenticación reciente (@RequiereReauth, RS9),
 *  - queda en la auditoría (RS9: permite detectar el uso indebido de credenciales de admin),
 *  - si quita permisos, revoca las sesiones al instante (RS5).
 */
@Service
public class AdminService {

    private final MongoTemplate mongo;
    private final AuditoriaService auditoria;
    private final AuthService auth;
    private final AsistenciaService asistencias;
    private final PoblarService poblar;

    public AdminService(MongoTemplate mongo, AuditoriaService auditoria, AuthService auth, AsistenciaService asistencias, PoblarService poblar) {
        this.mongo = mongo;
        this.auditoria = auditoria;
        this.auth = auth;
        this.asistencias = asistencias;
        this.poblar = poblar;
    }

    private static Instant ahora() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /* --------------------------- Organizaciones --------------------------- */

    public Pagina<Dto.OrganizacionDto> listarOrganizaciones(int page, int limit) {
        Query q = new Query().with(Sort.by(Sort.Order.desc("activa"), Sort.Order.asc("nombre"))).skip(Pagina.saltear(page, limit)).limit(limit);
        List<Organizacion> lista = mongo.find(q, Organizacion.class);
        long total = mongo.count(new Query(), Organizacion.class);

        List<ObjectId> ids = lista.stream().map(Organizacion::getId).toList();
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("rol").is(Usuario.COORDINADOR).and("organizacionId").in(ids)),
                Aggregation.group("organizacionId").count().as("n"));
        Map<ObjectId, Long> porOrg = mongo.aggregate(agg, "usuarios", Document.class).getMappedResults().stream()
                .collect(Collectors.toMap(d -> d.getObjectId("_id"), d -> ((Number) d.get("n")).longValue()));
        return new Pagina<>(lista.stream().map(o -> Dto.organizacion(o, porOrg.getOrDefault(o.getId(), 0L))).toList(), page, limit, total);
    }

    public Dto.OrganizacionDto crearOrganizacion(UsuarioActual admin, AdminEntrada.Organizacion datos, Ctx ctx) {
        if (mongo.exists(new Query(Criteria.where("nombre").is(datos.nombre())), Organizacion.class)) {
            throw ApiError.conflict("ORGANIZACION_EXISTE", "Ya existe una organización con ese nombre");
        }
        Organizacion org = new Organizacion();
        org.setNombre(datos.nombre());
        org.setContacto(datos.contacto());
        org.setConvenioOSLA(datos.convenioOSLA());
        org.setActiva(true);
        org = mongo.insert(org);
        auditoria.registrar(admin.id(), "admin_org_alta", "organizacion", org.getId(), "ok", ctx.ip(), null);
        return Dto.organizacion(org, 0);
    }

    /**
     * Baja de organización: queda inactiva, sus coordinadores pierden el acceso al instante (RS5)
     * y sus actividades y campañas dejan de mostrarse.
     */
    public void bajaOrganizacion(UsuarioActual admin, String id, Ctx ctx) {
        Organizacion org = mongo.findById(Ids.oid(id), Organizacion.class);
        if (org == null) throw ApiError.notFound("ORGANIZACION_NO_ENCONTRADA", "La organización no existe");
        if (!org.isActiva()) throw ApiError.conflict("ORGANIZACION_INACTIVA", "La organización ya estaba dada de baja");

        mongo.updateFirst(new Query(Criteria.where("_id").is(org.getId())), new Update().set("activa", false).set("updatedAt", ahora()), Organizacion.class);

        List<ObjectId> coordinadores = mongo.find(new Query(Criteria.where("rol").is(Usuario.COORDINADOR).and("organizacionId").is(org.getId())), Usuario.class)
                .stream().map(Usuario::getId).toList();
        auth.revocarSesiones(coordinadores);
        mongo.updateMulti(new Query(Criteria.where("organizacionId").is(org.getId()).and("estado").is("publicada")), new Update().set("estado", "cancelada"), Actividad.class);
        mongo.updateMulti(new Query(Criteria.where("organizacionId").is(org.getId()).and("estado").is("activa")), new Update().set("estado", "finalizada"), Campana.class);

        auditoria.registrar(admin.id(), "admin_org_baja", "organizacion", org.getId(), "ok", ctx.ip(), coordinadores.size() + " coordinadores revocados");
    }

    /* ------------------------------ Usuarios ------------------------------ */

    public Pagina<Dto.UsuarioParaAdmin> listarUsuarios(String buscar, String rol, int page, int limit) {
        Criteria filtro = new Criteria();
        if (rol != null) filtro = Criteria.where("rol").is(rol);
        if (buscar != null && !buscar.isBlank()) {
            // Pattern.quote: el texto se busca literal (sin inyección de regex ni ReDoS).
            Pattern re = Pattern.compile(Pattern.quote(buscar.strip()), Pattern.CASE_INSENSITIVE);
            Criteria texto = new Criteria().orOperator(Criteria.where("nombre").regex(re), Criteria.where("apellido").regex(re), Criteria.where("email").regex(re));
            filtro = new Criteria().andOperator(filtro, texto);
        }
        Query q = new Query(filtro).with(Sort.by("nombre", "apellido")).skip(Pagina.saltear(page, limit)).limit(limit);
        List<Usuario> lista = mongo.find(q, Usuario.class);
        long total = mongo.count(new Query(filtro), Usuario.class);
        Map<ObjectId, Organizacion> orgs = poblar.organizaciones(lista.stream().map(Usuario::getOrganizacionId).toList());
        return new Pagina<>(lista.stream().map(u -> Dto.usuarioParaAdmin(u, orgs.get(u.getOrganizacionId()))).toList(), page, limit, total);
    }

    private Dto.UsuarioParaAdmin paraAdmin(ObjectId id) {
        Usuario u = mongo.findById(id, Usuario.class);
        Organizacion org = u.getOrganizacionId() == null ? null : mongo.findById(u.getOrganizacionId(), Organizacion.class);
        return Dto.usuarioParaAdmin(u, org);
    }

    /** POST /admin/coordinadores: convierte a un voluntario en coordinador de una organización activa. */
    public Dto.UsuarioParaAdmin asignarCoordinador(UsuarioActual admin, AdminEntrada.Coordinador datos, Ctx ctx) {
        Usuario u = mongo.findById(Ids.oid(datos.usuarioId()), Usuario.class);
        Organizacion org = mongo.findById(Ids.oid(datos.organizacionId()), Organizacion.class);
        if (u == null) throw ApiError.notFound("USUARIO_NO_ENCONTRADO", "El usuario no existe");
        if (org == null || !org.isActiva()) throw ApiError.notFound("ORGANIZACION_NO_ENCONTRADA", "La organización no existe o no está activa");
        if (!Usuario.VOLUNTARIO.equals(u.getRol())) throw ApiError.conflict("ROL_INVALIDO", "Solo se puede asignar como coordinador a un voluntario");
        if (u.isEsJudicial()) throw ApiError.conflict("ES_JUDICIAL", "Una persona del programa judicial no puede ser coordinadora");

        mongo.updateFirst(new Query(Criteria.where("_id").is(u.getId())),
                new Update().set("rol", Usuario.COORDINADOR).set("organizacionId", org.getId()).set("updatedAt", ahora()), Usuario.class);
        auditoria.registrar(admin.id(), "admin_coord_alta", "usuario", u.getId(), "ok", ctx.ip(), "org " + org.getId().toHexString());
        return paraAdmin(u.getId());
    }

    /** DELETE /admin/coordinadores/:id: vuelve a ser voluntario y pierde el acceso al instante (RS5, A4). */
    public void quitarCoordinador(UsuarioActual admin, String usuarioId, Ctx ctx) {
        Usuario u = mongo.findById(Ids.oid(usuarioId), Usuario.class);
        if (u == null) throw ApiError.notFound("USUARIO_NO_ENCONTRADO", "El usuario no existe");
        if (!Usuario.COORDINADOR.equals(u.getRol())) throw ApiError.conflict("NO_ES_COORDINADOR", "El usuario no es coordinador");

        ObjectId orgAnterior = u.getOrganizacionId();
        mongo.updateFirst(new Query(Criteria.where("_id").is(u.getId())),
                new Update().set("rol", Usuario.VOLUNTARIO).set("organizacionId", null).set("updatedAt", ahora()), Usuario.class);
        auth.revocarSesiones(List.of(u.getId()));
        auditoria.registrar(admin.id(), "admin_coord_baja", "usuario", u.getId(), "ok", ctx.ip(), "org " + Ids.str(orgAnterior));
    }

    /** PATCH /admin/voluntarios/:id/judicial: condición judicial (atributo ABAC) y horas dictadas. */
    public Dto.UsuarioParaAdmin actualizarJudicial(UsuarioActual admin, String usuarioId, AdminEntrada.Judicial datos, Ctx ctx) {
        Usuario u = mongo.findById(Ids.oid(usuarioId), Usuario.class);
        if (u == null) throw ApiError.notFound("USUARIO_NO_ENCONTRADO", "El usuario no existe");
        if (!Usuario.VOLUNTARIO.equals(u.getRol())) throw ApiError.conflict("ROL_INVALIDO", "La condición judicial solo aplica a voluntarios");

        boolean judicial = datos.esJudicial();
        mongo.updateFirst(new Query(Criteria.where("_id").is(u.getId())),
                new Update().set("esJudicial", judicial).set("horasAsignadas", judicial ? datos.horasAsignadas() : 0).set("updatedAt", ahora()), Usuario.class);
        // En la auditoría no guardamos el valor (es un dato sensible): solo que se modificó y quién lo hizo.
        auditoria.registrar(admin.id(), "admin_judicial_cambio", "usuario", u.getId(), "ok", ctx.ip(), null);
        return paraAdmin(u.getId());
    }

    /** GET /admin/judiciales/horas: horas asignadas vs. cumplidas (suma de asistencias VALIDADAS). */
    public Pagina<Dto.HorasJudicial> horasJudiciales(int page, int limit) {
        Criteria filtro = Criteria.where("esJudicial").is(true).and("rol").is(Usuario.VOLUNTARIO);
        Query q = new Query(filtro).with(Sort.by("apellido", "nombre")).skip(Pagina.saltear(page, limit)).limit(limit);
        List<Usuario> lista = mongo.find(q, Usuario.class);
        long total = mongo.count(new Query(filtro), Usuario.class);
        Map<ObjectId, Double> sumas = asistencias.horasValidadas(lista.stream().map(Usuario::getId).toList());
        return new Pagina<>(lista.stream().map(u -> {
            double cumplidas = Numeros.redondear(sumas.getOrDefault(u.getId(), 0.0));
            return new Dto.HorasJudicial(Ids.str(u.getId()), u.getNombre(), u.getApellido(), u.getEmail(), u.getHorasAsignadas(), cumplidas,
                    Numeros.redondear(Math.max(0, u.getHorasAsignadas() - cumplidas)));
        }).toList(), page, limit, total);
    }

    /** GET /admin/auditoria: últimos eventos (RS9: detectar uso indebido de credenciales). */
    public Pagina<Dto.EventoAuditoria> listarAuditoria(String accion, String resultado, int page, int limit) {
        Criteria filtro = new Criteria();
        if (accion != null) filtro = Criteria.where("accion").is(accion);
        if (resultado != null) filtro = new Criteria().andOperator(filtro, Criteria.where("resultado").is(resultado));
        Query q = new Query(filtro).with(Sort.by(Sort.Direction.DESC, "fecha")).skip(Pagina.saltear(page, limit)).limit(limit);
        List<Auditoria> lista = mongo.find(q, Auditoria.class);
        long total = mongo.count(new Query(filtro), Auditoria.class);
        Map<ObjectId, Usuario> usuarios = poblar.usuarios(lista.stream().map(Auditoria::getUsuarioId).toList());
        return new Pagina<>(lista.stream().map(e -> Dto.eventoAuditoria(e, usuarios.get(e.getUsuarioId()))).toList(), page, limit, total);
    }
}
