package uy.edu.um.manoamiga.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import com.fasterxml.jackson.annotation.JsonInclude;

import uy.edu.um.manoamiga.modelo.Actividad;
import uy.edu.um.manoamiga.modelo.Asistencia;
import uy.edu.um.manoamiga.modelo.Auditoria;
import uy.edu.um.manoamiga.modelo.Campana;
import uy.edu.um.manoamiga.modelo.Etiqueta;
import uy.edu.um.manoamiga.modelo.Inscripcion;
import uy.edu.um.manoamiga.modelo.Organizacion;
import uy.edu.um.manoamiga.modelo.Publicacion;
import uy.edu.um.manoamiga.modelo.Usuario;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Textos;

/**
 * DTOs de respuesta (5.5): cada respuesta incluye SOLO los campos que quien consulta puede ver.
 * Nunca se devuelve una entidad "tal cual" (por ejemplo, el hash de la contraseña nunca sale).
 * Los nombres de los campos son los mismos que en la versión Node: la app no nota la diferencia.
 */
public final class Dto {

    private Dto() {}

    /* ----------------------------- Piezas comunes ----------------------------- */

    public record LatLng(double lat, double lng) {
        public static LatLng de(GeoJsonPoint p) {
            return p == null ? null : new LatLng(p.getY(), p.getX()); // GeoJSON: [lng, lat]
        }
    }

    /** Referencia a otro documento: { id, nombre } (nombre se omite si no se pudo cargar). */
    public record Ref(String id, @JsonInclude(JsonInclude.Include.NON_NULL) String nombre) {}

    public record Preferencias(String tema) {}

    private static Preferencias preferencias(Usuario u) {
        String tema = u.getPreferencias() != null && u.getPreferencias().getTema() != null ? u.getPreferencias().getTema() : "sistema";
        return new Preferencias(tema);
    }

    private static Ref ref(ObjectId id, Map<ObjectId, String> nombres) {
        if (id == null) return new Ref(null, null);
        return new Ref(id.toHexString(), nombres.get(id));
    }

    /* -------------------------------- Usuarios -------------------------------- */

    public record UsuarioPropio(String id, String nombre, String apellido, String email, String telefono, String rol,
            String organizacionId, boolean esJudicial,
            @JsonInclude(JsonInclude.Include.NON_NULL) Integer horasAsignadas, // solo si es judicial
            Preferencias preferencias) {}

    public static UsuarioPropio usuarioPropio(Usuario u) {
        return new UsuarioPropio(Ids.str(u.getId()), u.getNombre(), u.getApellido(), u.getEmail(), Textos.vacioSiNull(u.getTelefono()),
                u.getRol(), Ids.str(u.getOrganizacionId()), u.isEsJudicial(), u.isEsJudicial() ? u.getHorasAsignadas() : null, preferencias(u));
    }

    /** Lo mínimo para la sesión en la app. */
    public record UsuarioSesion(String id, String nombre, String rol, String organizacionId, boolean esJudicial, Preferencias preferencias) {}

    public static UsuarioSesion usuarioSesion(Usuario u) {
        return new UsuarioSesion(Ids.str(u.getId()), u.getNombre(), u.getRol(), Ids.str(u.getOrganizacionId()), u.isEsJudicial(), preferencias(u));
    }

    /** Usuario visto por el ADMINISTRADOR (es quien asigna roles y la condición judicial). */
    public record UsuarioParaAdmin(String id, String nombre, String apellido, String email, String rol, Ref organizacion,
            boolean esJudicial, int horasAsignadas, boolean activo) {}

    public static UsuarioParaAdmin usuarioParaAdmin(Usuario u, Organizacion org) {
        Ref o = org != null ? new Ref(Ids.str(org.getId()), org.getNombre()) : null;
        return new UsuarioParaAdmin(Ids.str(u.getId()), u.getNombre(), u.getApellido(), u.getEmail(), u.getRol(), o,
                u.isEsJudicial(), u.getHorasAsignadas(), u.isActivo());
    }

    /** Voluntario visto por un coordinador DE SU organización (RS6 permite teléfono, apellido y condición judicial). */
    public record VoluntarioParaCoordinador(String id, String nombre, String apellido, String telefono, boolean esJudicial) {}

    public static VoluntarioParaCoordinador voluntarioParaCoordinador(Usuario u) {
        if (u == null) return null;
        return new VoluntarioParaCoordinador(Ids.str(u.getId()), u.getNombre(), u.getApellido(), Textos.vacioSiNull(u.getTelefono()), u.isEsJudicial());
    }

    /** Sesión nueva (login, refresh, cambio de contraseña). */
    public record Sesion(String accessToken, String refreshToken, UsuarioSesion usuario) {}

    /* ---------------------------- Organizaciones ---------------------------- */

    public record OrganizacionDto(String id, String nombre, String contacto, boolean convenioOSLA, boolean activa, long coordinadores) {}

    public static OrganizacionDto organizacion(Organizacion o, long coordinadores) {
        return new OrganizacionDto(Ids.str(o.getId()), o.getNombre(), Textos.vacioSiNull(o.getContacto()), o.isConvenioOSLA(), o.isActiva(), coordinadores);
    }

    /* ------------------------------- Auditoría ------------------------------- */

    public record UsuarioRef(String id, String nombre, String email) {}

    /** Entrada de auditoría vista por el admin. Nunca incluye contraseñas ni tokens (no se guardan). */
    public record EventoAuditoria(String id, Instant fecha, String accion, String resultado, String recurso, String recursoId,
            String ip, String detalle, UsuarioRef usuario) {}

    public static EventoAuditoria eventoAuditoria(Auditoria e, Usuario u) {
        UsuarioRef ref = u != null ? new UsuarioRef(Ids.str(u.getId()), u.getNombre(), u.getEmail()) : null;
        return new EventoAuditoria(Ids.str(e.getId()), e.getFecha(), e.getAccion(), e.getResultado(), e.getRecurso(), e.getRecursoId(),
                e.getIp(), e.getDetalle(), ref);
    }

    /** Historial de accesos del propio usuario (R8). */
    public record Acceso(String id, Instant fecha, String accion, String resultado, String ip, String dispositivo) {}

    public static Acceso acceso(Auditoria e) {
        return new Acceso(Ids.str(e.getId()), e.getFecha(), e.getAccion(), e.getResultado(), e.getIp(), e.getDetalle());
    }

    /* ------------------------------- Actividades ------------------------------ */

    public record EtiquetaDto(String id, String nombre, boolean sensible) {}

    public static EtiquetaDto etiqueta(Etiqueta e) {
        return new EtiquetaDto(Ids.str(e.getId()), e.getNombre(), e.isSensible());
    }

    public record ActividadDto(String id, String titulo, String descripcion, Ref organizacion, String organizacionId, List<Ref> etiquetas,
            LatLng ubicacion, String direccion, Instant fechaInicio, Instant fechaFin, int cupo, int inscriptos, int cuposLibres, String estado) {}

    /**
     * @param orgs      nombres de organizaciones por id (lo que en Node hacía populate)
     * @param etiquetas nombres de etiquetas por id
     */
    public static ActividadDto actividad(Actividad a, Map<ObjectId, String> orgs, Map<ObjectId, String> etiquetas) {
        Ref org = ref(a.getOrganizacionId(), orgs);
        List<Ref> tags = a.getEtiquetas() == null ? List.of() : a.getEtiquetas().stream().map(t -> ref(t, etiquetas)).toList();
        return new ActividadDto(Ids.str(a.getId()), a.getTitulo(), a.getDescripcion(), org, org.id(), tags, LatLng.de(a.getUbicacion()),
                Textos.vacioSiNull(a.getDireccion()), a.getFechaInicio(), a.getFechaFin(), a.getCupo(), a.getInscriptos(),
                Math.max(0, a.getCupo() - a.getInscriptos()), a.getEstado());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ActividadResumen(String id, String titulo, Instant fechaInicio, Instant fechaFin, String direccion, String estado) {}

    public static ActividadResumen actividadResumen(ObjectId id, Actividad a) {
        if (a == null) return new ActividadResumen(Ids.str(id), null, null, null, null, null);
        return new ActividadResumen(Ids.str(a.getId()), a.getTitulo(), a.getFechaInicio(), a.getFechaFin(), Textos.vacioSiNull(a.getDireccion()), a.getEstado());
    }

    /* ------------------------------ Inscripciones ----------------------------- */

    /** Respuesta corta al inscribirse o al cambiar de estado. */
    public record InscripcionCorta(String id, String actividadId, String estado) {}

    /** Inscripción vista por el propio voluntario. */
    public record InscripcionPropia(String id, String actividadId, ActividadResumen actividad, String estado, Instant fecha) {}

    public static InscripcionPropia inscripcionPropia(Inscripcion i, Actividad a) {
        return new InscripcionPropia(Ids.str(i.getId()), Ids.str(i.getActividadId()), actividadResumen(i.getActividadId(), a), i.getEstado(), i.getFecha());
    }

    /** Inscripción vista por el coordinador de la organización. */
    public record InscripcionParaCoordinador(String id, String actividadId, String estado, Instant fecha, VoluntarioParaCoordinador voluntario) {}

    public static InscripcionParaCoordinador inscripcionParaCoordinador(Inscripcion i, Usuario v) {
        return new InscripcionParaCoordinador(Ids.str(i.getId()), Ids.str(i.getActividadId()), i.getEstado(), i.getFecha(), voluntarioParaCoordinador(v));
    }

    /* ------------------------------- Asistencias ------------------------------ */

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VoluntarioRef(String id, String nombre, String apellido) {}

    public record AsistenciaDto(String id, String inscripcionId, String actividadId, VoluntarioRef voluntario, Instant checkIn, Instant checkOut,
            double horas, String metodo, String estado, Instant validadaEn) {}

    public static AsistenciaDto asistencia(Asistencia a, Usuario v) {
        VoluntarioRef ref = v != null ? new VoluntarioRef(Ids.str(v.getId()), v.getNombre(), v.getApellido()) : new VoluntarioRef(Ids.str(a.getVoluntarioId()), null, null);
        return new AsistenciaDto(Ids.str(a.getId()), Ids.str(a.getInscripcionId()), Ids.str(a.getActividadId()), ref, a.getCheckIn(), a.getCheckOut(),
                a.getHoras(), a.getMetodo(), a.getEstado(), a.getValidadaEn());
    }

    public record Horas(int horasAsignadas, double horasCumplidas, double horasRestantes) {}

    public record Qr(String qr, Instant expiraEn) {}

    /* ------------------------------- Comunidad ------------------------------- */

    public record PuntoDto(String id, String nombre, String direccion, LatLng ubicacion) {}

    public record CampanaDto(String id, String titulo, String descripcion, String objetivo, Ref organizacion, List<PuntoDto> puntos,
            Instant fechaInicio, Instant fechaFin, String estado) {}

    public static CampanaDto campana(Campana c, Map<ObjectId, String> orgs) {
        List<PuntoDto> puntos = c.getPuntos() == null ? List.of() : c.getPuntos().stream()
                .map(p -> new PuntoDto(Ids.str(p.getId()), p.getNombre(), Textos.vacioSiNull(p.getDireccion()), LatLng.de(p.getUbicacion())))
                .toList();
        return new CampanaDto(Ids.str(c.getId()), c.getTitulo(), c.getDescripcion(), Textos.vacioSiNull(c.getObjetivo()),
                ref(c.getOrganizacionId(), orgs), puntos, c.getFechaInicio(), c.getFechaFin(), c.getEstado());
    }

    public record Autor(String nombre) {}

    /** Publicación pública: el autor se muestra solo con su nombre (sin apellido ni contacto). */
    public record PublicacionDto(String id, String proyectoId, String proyectoTipo, String tipo, String contenido, Autor autor, Instant fecha) {}

    public static PublicacionDto publicacion(Publicacion p, Usuario autor) {
        return new PublicacionDto(Ids.str(p.getId()), Ids.str(p.getProyectoId()), p.getProyectoTipo(), p.getTipo(), p.getContenido(),
                autor != null ? new Autor(autor.getNombre()) : null, p.getFecha());
    }

    /* ---------------------------------- Admin --------------------------------- */

    public record HorasJudicial(String id, String nombre, String apellido, String email, int horasAsignadas, double horasCumplidas, double horasRestantes) {}

    /* ---------------------------------- Mapa ---------------------------------- */

    /** Punto del mapa (RS7): solo datos institucionales, nunca de personas. */
    public record PuntoActividad(String tipo, String id, String titulo, String organizacion, List<String> etiquetas, String direccion,
            LatLng ubicacion, Instant fechaInicio, Instant fechaFin, int cuposLibres) {}

    public record PuntoAcopio(String tipo, String id, String campanaId, String titulo, String campana, String organizacion, String direccion,
            LatLng ubicacion, Instant fechaFin) {}

    public record Lista<T>(List<T> data) {}
}
