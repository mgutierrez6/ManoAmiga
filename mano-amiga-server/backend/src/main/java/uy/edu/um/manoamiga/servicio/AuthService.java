package uy.edu.um.manoamiga.servicio;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import uy.edu.um.manoamiga.config.Configuracion;
import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.modelo.Organizacion;
import uy.edu.um.manoamiga.modelo.Sesion;
import uy.edu.um.manoamiga.modelo.Usuario;
import uy.edu.um.manoamiga.seguridad.TokenService;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.web.entrada.AuthEntrada;

/** Registro, login, renovación de sesión, logout, reautenticación y cambio de contraseña (5.1, 5.2, RS5, RS9). */
@Service
public class AuthService {

    private final MongoTemplate mongo;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final AuditoriaService auditoria;

    /**
     * Hash "de mentira" para comparar cuando el email no existe: así el login tarda lo mismo exista o no
     * la cuenta, y no se puede averiguar qué emails están registrados midiendo el tiempo.
     */
    private final String hashFalso;

    public AuthService(MongoTemplate mongo, PasswordEncoder passwords, TokenService tokens, AuditoriaService auditoria) {
        this.mongo = mongo;
        this.passwords = passwords;
        this.tokens = tokens;
        this.auditoria = auditoria;
        this.hashFalso = passwords.encode("contraseña-que-no-existe");
    }

    private static ApiError credencialesInvalidas() {
        return ApiError.unauthorized("CREDENCIALES_INVALIDAS", "Email o contraseña incorrectos");
    }

    private static ApiError sesionInvalida() {
        return ApiError.unauthorized("SESION_INVALIDA", "La sesión venció, volvé a iniciar sesión");
    }

    /** Registro: el rol SIEMPRE es voluntario (5.1). Coordinadores y admins los asigna el administrador. */
    public Dto.UsuarioSesion registrar(AuthEntrada.Registro datos, Ctx ctx) {
        if (mongo.exists(new Query(Criteria.where("email").is(datos.email())), Usuario.class)) {
            throw ApiError.conflict("EMAIL_EN_USO", "Ya existe una cuenta con ese email");
        }
        Usuario u = new Usuario();
        u.setNombre(datos.nombre());
        u.setApellido(datos.apellido());
        u.setEmail(datos.email());
        u.setTelefono(datos.telefono() == null ? "" : datos.telefono());
        u.setPasswordHash(passwords.encode(datos.password()));
        u.setRol(Usuario.VOLUNTARIO); // nunca del body
        try {
            u = mongo.insert(u);
        } catch (DuplicateKeyException e) {
            throw ApiError.conflict("EMAIL_EN_USO", "Ya existe una cuenta con ese email");
        }
        auditoria.registrar(Ids.str(u.getId()), "registro", "usuario", u.getId(), "ok", ctx.ip(), null);
        return Dto.usuarioSesion(u);
    }

    /** Crea una sesión (refresh token guardado hasheado) y firma el access token con la versión vigente. */
    private Dto.Sesion crearSesion(Usuario usuario, Ctx ctx, String familia) {
        String refreshToken = tokens.generarRefreshToken();
        Sesion s = new Sesion();
        s.setUserId(usuario.getId());
        s.setTokenHash(tokens.hashRefreshToken(refreshToken));
        s.setFamilia(familia != null ? familia : UUID.randomUUID().toString());
        s.setDispositivo(ctx.dispositivo(200));
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        s.setCreadaEn(ahora);
        s.setExpiraEn(ahora.plus(Configuracion.REFRESH_TOKEN_DIAS, ChronoUnit.DAYS));
        mongo.insert(s);
        String access = tokens.firmarAccessToken(Ids.str(usuario.getId()), usuario.getVersionSesion());
        return new Dto.Sesion(access, refreshToken, Dto.usuarioSesion(usuario));
    }

    /** Login con mensaje genérico y auditoría de éxitos y fallos (5.1). */
    public Dto.Sesion login(AuthEntrada.Login datos, Ctx ctx) {
        Usuario usuario = mongo.findOne(new Query(Criteria.where("email").is(datos.email())), Usuario.class);
        boolean ok = passwords.matches(datos.password(), usuario != null ? usuario.getPasswordHash() : hashFalso);

        if (usuario == null || !ok || !usuario.isActivo()) {
            auditoria.registrar(usuario != null ? Ids.str(usuario.getId()) : null, "login", null, null, "fallo", ctx.ip(), null);
            throw credencialesInvalidas();
        }
        if (Usuario.COORDINADOR.equals(usuario.getRol())) {
            Organizacion org = usuario.getOrganizacionId() == null ? null : mongo.findById(usuario.getOrganizacionId(), Organizacion.class);
            if (org == null || !org.isActiva()) {
                auditoria.registrar(Ids.str(usuario.getId()), "login", null, null, "fallo", ctx.ip(), "organización inactiva");
                throw credencialesInvalidas();
            }
        }
        Dto.Sesion sesion = crearSesion(usuario, ctx, null);
        auditoria.registrar(Ids.str(usuario.getId()), "login", null, null, "ok", ctx.ip(), ctx.dispositivo(100));
        return sesion;
    }

    /**
     * Renovación con ROTACIÓN (5.2): cada refresh token sirve una sola vez.
     * Si llega uno ya usado, alguien lo robó: se cierran TODAS las sesiones del usuario.
     */
    public Dto.Sesion refresh(String refreshToken, Ctx ctx) {
        String hash = tokens.hashRefreshToken(refreshToken);
        Sesion sesion = mongo.findOne(new Query(Criteria.where("tokenHash").is(hash)), Sesion.class);
        if (sesion == null || sesion.getExpiraEn().isBefore(Instant.now())) throw sesionInvalida();

        // Marcamos como usada de forma atómica: si dos pedidos llegan a la vez, solo uno gana.
        Sesion marcada = mongo.findAndModify(
                new Query(Criteria.where("_id").is(sesion.getId()).and("reemplazada").is(false)),
                new Update().set("reemplazada", true),
                Sesion.class);
        if (marcada == null) {
            revocarSesiones(List.of(sesion.getUserId()));
            auditoria.registrar(Ids.str(sesion.getUserId()), "refresh_reusado", null, null, "denegado", ctx.ip(), "Se cerraron todas las sesiones");
            throw sesionInvalida();
        }

        Usuario usuario = mongo.findById(sesion.getUserId(), Usuario.class);
        if (usuario == null || !usuario.isActivo()) {
            mongo.remove(new Query(Criteria.where("userId").is(sesion.getUserId())), Sesion.class);
            throw sesionInvalida();
        }
        return crearSesion(usuario, ctx, sesion.getFamilia());
    }

    /** Logout: se cierran todas las sesiones del usuario (5.2 · Revocación). */
    public void logout(UsuarioActual usuario, Ctx ctx) {
        revocarSesiones(List.of(Ids.oid(usuario.id())));
        auditoria.registrar(usuario.id(), "logout", null, null, "ok", ctx.ip(), null);
    }

    /**
     * RS5 · Revocación inmediata: borra los refresh tokens Y sube la versión de sesión, así los access
     * tokens ya emitidos dejan de valer en el siguiente pedido (no hay que esperar a que venzan).
     */
    public void revocarSesiones(Collection<ObjectId> usuarioIds) {
        if (usuarioIds.isEmpty()) return;
        mongo.updateMulti(new Query(Criteria.where("_id").in(usuarioIds)), new Update().inc("versionSesion", 1), Usuario.class);
        mongo.remove(new Query(Criteria.where("userId").in(usuarioIds)), Sesion.class);
    }

    /**
     * RS9 · Reautenticación del administrador: vuelve a pedir la contraseña y entrega un token de 5 minutos
     * para las acciones sensibles del panel. Los fallos quedan en la auditoría.
     */
    public TokenService.Reauth reautenticar(UsuarioActual usuario, String password, Ctx ctx) {
        Usuario u = mongo.findById(Ids.oid(usuario.id()), Usuario.class);
        boolean ok = u != null && passwords.matches(password, u.getPasswordHash());
        if (!ok) {
            auditoria.registrar(usuario.id(), "reauth", null, null, "fallo", ctx.ip(), null);
            throw ApiError.unauthorized("CREDENCIALES_INVALIDAS", "La contraseña no es correcta");
        }
        auditoria.registrar(usuario.id(), "reauth", null, null, "ok", ctx.ip(), null);
        return tokens.firmarReauthToken(usuario.id());
    }

    /**
     * Cambio de contraseña (R8): exige la actual, cierra TODAS las sesiones (incluidas las de otros
     * dispositivos) y devuelve una sesión nueva para este dispositivo.
     */
    public Dto.Sesion cambiarPassword(UsuarioActual usuario, String actual, String nueva, Ctx ctx) {
        Usuario u = mongo.findById(Ids.oid(usuario.id()), Usuario.class);
        if (u == null) throw ApiError.notFound();
        if (!passwords.matches(actual, u.getPasswordHash())) {
            auditoria.registrar(usuario.id(), "password_cambio", null, null, "fallo", ctx.ip(), null);
            throw ApiError.badRequest("PASSWORD_ACTUAL_INCORRECTA", "La contraseña actual no es correcta");
        }
        if (passwords.matches(nueva, u.getPasswordHash())) {
            throw ApiError.badRequest("PASSWORD_REPETIDA", "La nueva contraseña tiene que ser distinta de la actual");
        }
        mongo.updateFirst(new Query(Criteria.where("_id").is(u.getId())), new Update().set("passwordHash", passwords.encode(nueva)), Usuario.class);
        revocarSesiones(List.of(u.getId()));
        auditoria.registrar(usuario.id(), "password_cambio", null, null, "ok", ctx.ip(), null);

        Usuario actualizado = mongo.findById(u.getId(), Usuario.class);
        return crearSesion(actualizado, ctx, null);
    }
}
