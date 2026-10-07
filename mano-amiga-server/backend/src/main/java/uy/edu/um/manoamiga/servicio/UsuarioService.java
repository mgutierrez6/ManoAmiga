package uy.edu.um.manoamiga.servicio;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.modelo.Auditoria;
import uy.edu.um.manoamiga.modelo.Usuario;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Pagina;
import uy.edu.um.manoamiga.web.entrada.UsuarioEntrada;

/** /usuarios/me/... siempre toma el id del TOKEN, nunca de la URL (RS2). */
@Service
public class UsuarioService {

    /** Formato de los tokens de Expo Push. */
    public static final Pattern PUSH_TOKEN = Pattern.compile("^(ExponentPushToken|ExpoPushToken)\\[[A-Za-z0-9_-]+]$");

    /** Acciones que se muestran en el historial de accesos del perfil. */
    private static final List<String> ACCIONES_ACCESO = List.of("login", "logout", "password_cambio", "refresh_reusado");

    private final MongoTemplate mongo;
    private final AuditoriaService auditoria;

    public UsuarioService(MongoTemplate mongo, AuditoriaService auditoria) {
        this.mongo = mongo;
        this.auditoria = auditoria;
    }

    private Usuario buscar(UsuarioActual usuario) {
        Usuario u = mongo.findById(Ids.oid(usuario.id()), Usuario.class);
        if (u == null) throw ApiError.notFound();
        return u;
    }

    public Dto.UsuarioPropio obtenerPerfil(UsuarioActual usuario) {
        return Dto.usuarioPropio(buscar(usuario));
    }

    /** Guarda el token de notificaciones del dispositivo (como máximo 5 por usuario). */
    public void registrarPushToken(UsuarioActual usuario, String token) {
        if (!PUSH_TOKEN.matcher(token).matches()) throw ApiError.badRequest("PUSH_TOKEN_INVALIDO", "Token de notificaciones inválido");
        Query mio = new Query(Criteria.where("_id").is(Ids.oid(usuario.id())));
        mongo.updateFirst(mio, new Update().pull("pushTokens", token), Usuario.class);
        mongo.updateFirst(mio, new Update().push("pushTokens").slice(-5).each(token), Usuario.class);
    }

    /** PATCH /usuarios/me (R8): solo datos de contacto y preferencias (mass assignment). */
    public Dto.UsuarioPropio actualizarPerfil(UsuarioActual usuario, UsuarioEntrada.Perfil datos, Ctx ctx) {
        Update set = new Update();
        if (datos.nombre() != null) set.set("nombre", datos.nombre());
        if (datos.apellido() != null) set.set("apellido", datos.apellido());
        if (datos.telefono() != null) set.set("telefono", datos.telefono());
        if (datos.preferencias() != null && datos.preferencias().tema() != null) set.set("preferencias.tema", datos.preferencias().tema());

        var r = mongo.updateFirst(new Query(Criteria.where("_id").is(Ids.oid(usuario.id()))), set, Usuario.class);
        if (r.getMatchedCount() == 0) throw ApiError.notFound();
        auditoria.registrar(usuario.id(), "perfil_editar", "usuario", usuario.id(), "ok", ctx.ip(), null);
        return Dto.usuarioPropio(buscar(usuario));
    }

    /** GET /usuarios/me/accesos (R8): historial de inicios de sesión del PROPIO usuario. */
    public Pagina<Dto.Acceso> accesos(UsuarioActual usuario, int page, int limit) {
        Criteria filtro = Criteria.where("usuarioId").is(Ids.oid(usuario.id())).and("accion").in(ACCIONES_ACCESO);
        Query q = new Query(filtro).with(Sort.by(Sort.Direction.DESC, "fecha")).skip(Pagina.saltear(page, limit)).limit(limit);
        List<Auditoria> lista = mongo.find(q, Auditoria.class);
        long total = mongo.count(new Query(filtro), Auditoria.class);
        return new Pagina<>(lista.stream().map(Dto::acceso).toList(), page, limit, total);
    }
}
