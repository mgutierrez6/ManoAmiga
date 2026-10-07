package uy.edu.um.manoamiga.seguridad;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.error.RespuestaError;

/**
 * Límite de tamaño del body: 100 KB (5.4), igual que la versión Node y el front-service.
 * Evita que alguien mande un JSON gigante para consumir memoria del servidor.
 */
@Component
public class LimiteTamanoFiltro extends OncePerRequestFilter {

    public static final long MAXIMO = 100 * 1024;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        if (req.getContentLengthLong() > MAXIMO) {
            RespuestaError.escribir(res, new ApiError(413, "DEMASIADO_GRANDE", "El pedido es demasiado grande"));
            return;
        }
        chain.doFilter(req, res);
    }
}
