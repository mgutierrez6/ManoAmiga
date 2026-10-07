package uy.edu.um.manoamiga.seguridad;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.modelo.Organizacion;
import uy.edu.um.manoamiga.modelo.Usuario;
import uy.edu.um.manoamiga.util.Ids;

/**
 * Verifica el JWT y RELEE al usuario en la base en cada pedido (RS5, A4).
 * El token solo lleva el id (sub) y la versión de sesión (ver): el rol, la organización, el estado y el
 * atributo judicial se toman SIEMPRE de la base, así un cambio de permisos tiene efecto en el siguiente
 * pedido. Si la versión no coincide, la sesión fue revocada y el token deja de servir al instante.
 */
@Service
public class AutenticacionService {

    private final TokenService tokens;
    private final MongoTemplate mongo;

    public AutenticacionService(TokenService tokens, MongoTemplate mongo) {
        this.tokens = tokens;
        this.mongo = mongo;
    }

    /** @param header el valor del header Authorization ("Bearer xxx") */
    public UsuarioActual cargar(String header) {
        String[] partes = header == null ? new String[0] : header.split(" ");
        if (partes.length != 2 || !"Bearer".equals(partes[0]) || partes[1].isBlank()) throw ApiError.unauthorized();

        Claims payload;
        try {
            payload = tokens.verificarAccessToken(partes[1]);
        } catch (JwtException | IllegalArgumentException e) {
            throw ApiError.unauthorized("TOKEN_INVALIDO", "La sesión venció o no es válida");
        }

        String sub = payload.getSubject();
        Usuario usuario = Ids.valido(sub) ? mongo.findById(new ObjectId(sub), Usuario.class) : null;
        if (usuario == null || !usuario.isActivo()) throw ApiError.unauthorized("TOKEN_INVALIDO", "La sesión no es válida");

        // RS5: sesión revocada (baja de coordinador u organización, cambio de contraseña, logout).
        Integer ver = payload.get("ver", Integer.class);
        if ((ver == null ? 0 : ver) != usuario.getVersionSesion()) {
            throw ApiError.unauthorized("SESION_REVOCADA", "Tu sesión fue cerrada, volvé a iniciar sesión");
        }

        // Un coordinador sin organización activa pierde el acceso inmediatamente (RS5).
        if (Usuario.COORDINADOR.equals(usuario.getRol())) {
            Organizacion org = usuario.getOrganizacionId() == null ? null : mongo.findById(usuario.getOrganizacionId(), Organizacion.class);
            if (org == null || !org.isActiva()) throw ApiError.unauthorized("ORGANIZACION_INACTIVA", "Tu organización no está activa");
        }

        return new UsuarioActual(
                Ids.str(usuario.getId()),
                usuario.getRol(),
                Ids.str(usuario.getOrganizacionId()),
                usuario.isEsJudicial(),
                usuario.getNombre());
    }
}
