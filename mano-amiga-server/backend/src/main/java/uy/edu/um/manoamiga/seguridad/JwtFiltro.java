package uy.edu.um.manoamiga.seguridad;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uy.edu.um.manoamiga.error.ApiError;

/**
 * Autenticación por JWT. Si el pedido trae "Authorization: Bearer ...", lo verifica y carga al usuario.
 *
 * Este filtro NO decide si el pedido pasa: eso lo hace la matriz (SecurityConfig).
 * - Token válido -> el pedido queda autenticado con el rol leído de la base (ROLE_VOLUNTARIO, etc.).
 * - Token inválido -> se guarda el motivo y el pedido sigue como anónimo. Si la ruta es pública (ej.: el
 *   mapa) se atiende como anónimo; si no, el EntryPoint responde 401 con ese motivo (TOKEN_INVALIDO,
 *   SESION_REVOCADA, ORGANIZACION_INACTIVA).
 *
 * No es un @Component a propósito: si lo fuera, Spring Boot lo registraría también como filtro común y
 * correría dos veces. Lo crea SecurityConfig.
 */
public class JwtFiltro extends OncePerRequestFilter {

    public static final String ATRIBUTO_ERROR = "manoamiga.errorAutenticacion";

    private final AutenticacionService autenticacion;

    public JwtFiltro(AutenticacionService autenticacion) {
        this.autenticacion = autenticacion;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null) {
            try {
                UsuarioActual usuario = autenticacion.cargar(header);
                var rol = new SimpleGrantedAuthority("ROLE_" + usuario.rol().toUpperCase(Locale.ROOT));
                var auth = new UsernamePasswordAuthenticationToken(usuario, null, List.of(rol));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (ApiError e) {
                req.setAttribute(ATRIBUTO_ERROR, e);
            }
        }
        chain.doFilter(req, res);
    }
}
