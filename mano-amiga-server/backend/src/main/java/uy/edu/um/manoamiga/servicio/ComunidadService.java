package uy.edu.um.manoamiga.servicio;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.modelo.Actividad;
import uy.edu.um.manoamiga.modelo.Campana;
import uy.edu.um.manoamiga.modelo.Publicacion;
import uy.edu.um.manoamiga.modelo.PuntoAcopio;
import uy.edu.um.manoamiga.modelo.Usuario;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Pagina;
import uy.edu.um.manoamiga.web.entrada.ComunidadEntrada;

/** Campañas de donación y feed de proyectos (R6 / R14). */
@Service
public class ComunidadService {

    private final MongoTemplate mongo;
    private final PermisosService permisos;
    private final AuditoriaService auditoria;
    private final PoblarService poblar;

    public ComunidadService(MongoTemplate mongo, PermisosService permisos, AuditoriaService auditoria, PoblarService poblar) {
        this.mongo = mongo;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.poblar = poblar;
    }

    private List<Dto.CampanaDto> aDtos(List<Campana> lista) {
        Map<ObjectId, String> orgs = poblar.nombresOrganizaciones(lista.stream().map(Campana::getOrganizacionId).toList());
        return lista.stream().map(c -> Dto.campana(c, orgs)).toList();
    }

    /** GET /campanas — público (RS8). Solo datos institucionales. */
    public Pagina<Dto.CampanaDto> listarCampanas(int page, int limit) {
        Criteria filtro = Criteria.where("estado").is("activa").and("fechaFin").gte(Instant.now());
        Query q = new Query(filtro).with(Sort.by(Sort.Direction.ASC, "fechaFin")).skip(Pagina.saltear(page, limit)).limit(limit);
        List<Campana> lista = mongo.find(q, Campana.class);
        long total = mongo.count(new Query(filtro), Campana.class);
        return new Pagina<>(aDtos(lista), page, limit, total);
    }

    /** POST /campanas — coordinador; la organización sale del usuario autenticado. */
    public Dto.CampanaDto crearCampana(UsuarioActual usuario, ComunidadEntrada.Campana datos, Ctx ctx) {
        Campana c = new Campana();
        c.setOrganizacionId(Ids.oid(usuario.organizacionId()));
        c.setCreadaPor(Ids.oid(usuario.id()));
        c.setTitulo(datos.titulo());
        c.setDescripcion(datos.descripcion());
        c.setObjetivo(datos.objetivo());
        List<PuntoAcopio> puntos = new ArrayList<>();
        for (ComunidadEntrada.Punto p : datos.puntos()) {
            PuntoAcopio pa = new PuntoAcopio();
            pa.setId(new ObjectId());
            pa.setNombre(p.nombre());
            pa.setDireccion(p.direccion());
            pa.setUbicacion(ActividadService.aPunto(p.ubicacion()));
            puntos.add(pa);
        }
        c.setPuntos(puntos);
        c.setFechaInicio(datos.fechaInicio().truncatedTo(ChronoUnit.MILLIS));
        c.setFechaFin(datos.fechaFin().truncatedTo(ChronoUnit.MILLIS));
        c = mongo.insert(c);
        auditoria.registrar(usuario.id(), "campana_crear", "campana", c.getId(), "ok", ctx.ip(), null);
        return aDtos(List.of(c)).get(0);
    }

    /** Un "proyecto" puede ser una actividad o una campaña. */
    private record Proyecto(String tipo, ObjectId id, ObjectId organizacionId) {}

    private Proyecto buscarProyecto(String id) {
        Actividad act = mongo.findById(Ids.oid(id), Actividad.class);
        if (act != null) return new Proyecto("actividad", act.getId(), act.getOrganizacionId());
        Campana camp = mongo.findById(Ids.oid(id), Campana.class);
        if (camp != null) return new Proyecto("campana", camp.getId(), camp.getOrganizacionId());
        throw ApiError.notFound("PROYECTO_NO_ENCONTRADO", "El proyecto no existe");
    }

    /** GET /proyectos/:id/publicaciones — feed público (RS8). */
    public Pagina<Dto.PublicacionDto> listarPublicaciones(String proyectoId, int page, int limit) {
        Proyecto p = buscarProyecto(proyectoId);
        Criteria filtro = Criteria.where("proyectoId").is(p.id());
        Query q = new Query(filtro).with(Sort.by(Sort.Direction.DESC, "fecha")).skip(Pagina.saltear(page, limit)).limit(limit);
        List<Publicacion> lista = mongo.find(q, Publicacion.class);
        long total = mongo.count(new Query(filtro), Publicacion.class);
        Map<ObjectId, Usuario> autores = poblar.usuarios(lista.stream().map(Publicacion::getAutorId).toList());
        return new Pagina<>(lista.stream().map(x -> Dto.publicacion(x, autores.get(x.getAutorId()))).toList(), page, limit, total);
    }

    /** POST /proyectos/:id/publicaciones — coordinador de la org dueña del proyecto (RS3). */
    public Dto.PublicacionDto crearPublicacion(UsuarioActual usuario, String proyectoId, ComunidadEntrada.Publicacion datos, Ctx ctx) {
        Proyecto p = buscarProyecto(proyectoId);
        permisos.assertMismaOrganizacion(usuario, p.organizacionId(), p.tipo(), p.id(), ctx);

        Publicacion pub = new Publicacion();
        pub.setProyectoId(p.id());
        pub.setProyectoTipo(p.tipo());
        pub.setOrganizacionId(Ids.oid(usuario.organizacionId()));
        pub.setAutorId(Ids.oid(usuario.id()));
        pub.setTipo(datos.tipo());
        pub.setContenido(datos.contenido());
        pub.setFecha(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        pub = mongo.insert(pub);
        auditoria.registrar(usuario.id(), "publicacion_crear", "publicacion", pub.getId(), "ok", ctx.ip(), null);
        Usuario autor = mongo.findById(Ids.oid(usuario.id()), Usuario.class);
        return Dto.publicacion(pub, autor);
    }
}
