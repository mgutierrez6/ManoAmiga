package uy.edu.um.manoamiga.seguridad;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.error.RespuestaError;
import uy.edu.um.manoamiga.servicio.AuditoriaService;

/**
 * RS9 · Control adicional del panel de administración. Corre DESPUÉS de la matriz (el rol ya se verificó) y
 * ANTES de leer y validar el body. Si roban el access token del admin, no alcanza para dar de alta o de baja
 * nada: hace falta la contraseña de nuevo. Los intentos sin reauth quedan en la auditoría.
 */
@Component
public class ReauthInterceptor implements HandlerInterceptor {

    private final TokenService tokens;
    private final AuditoriaService auditoria;

    public ReauthInterceptor(TokenService tokens, AuditoriaService auditoria) {
        this.tokens = tokens;
        this.auditoria = auditoria;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm) || !hm.hasMethodAnnotation(RequiereReauth.class)) return true;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UsuarioActual usuario = auth != null && auth.getPrincipal() instanceof UsuarioActual u ? u : null;
        if (usuario == null) {
            RespuestaError.escribir(res, ApiError.unauthorized());
            return false;
        }

        String token = req.getHeader("X-Reauth-Token");
        boolean valido = false;
        if (token != null && !token.isBlank()) {
            try {
                Claims c = tokens.verificarReauthToken(token);
                valido = usuario.id().equals(c.getSubject()); // el token tiene que ser de ESTE admin
            } catch (RuntimeException e) {
                valido = false;
            }
        }
        if (!valido) {
            Object patron = req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String recurso = req.getMethod() + " " + (patron != null ? patron.toString().replaceAll("\\{(\\w+)}", ":$1") : req.getRequestURI());
            auditoria.registrar(usuario.id(), "reauth_requerida", recurso, null, "denegado", req.getRemoteAddr(), null);
            RespuestaError.escribir(res, ApiError.forbidden("REAUTH_REQUERIDA", "Por seguridad, confirmá tu contraseña para hacer esta acción"));
            return false;
        }
        return true;
    }
}
