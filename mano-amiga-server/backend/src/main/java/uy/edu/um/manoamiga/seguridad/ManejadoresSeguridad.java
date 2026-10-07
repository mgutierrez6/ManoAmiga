package uy.edu.um.manoamiga.seguridad;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.error.RespuestaError;
import uy.edu.um.manoamiga.servicio.AuditoriaService;

/**
 * Respuestas de la capa de seguridad con el mismo formato de error que el resto de la API.
 * - 401: sin sesión o con un token que no sirve (RS8).
 * - 403: el rol no figura en la matriz para esa ruta (RS1, RS4). Queda en la auditoría (sirve para detectar abuso).
 */
@Component
public class ManejadoresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final AuditoriaService auditoria;

    public ManejadoresSeguridad(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        Object motivo = req.getAttribute(JwtFiltro.ATRIBUTO_ERROR);
        RespuestaError.escribir(res, motivo instanceof ApiError e ? e : ApiError.unauthorized());
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioActual u) {
            Matriz.Entrada entrada = Matriz.buscar(req.getMethod(), req.getRequestURI());
            String recurso = req.getMethod() + " " + (entrada != null ? entrada.ruta() : req.getRequestURI());
            auditoria.registrar(u.id(), "acceso_denegado_rol", recurso, null, "denegado", req.getRemoteAddr(), null);
        }
        RespuestaError.escribir(res, ApiError.forbidden("ROL_NO_PERMITIDO", "Tu rol no tiene permiso para esta acción"));
    }
}
