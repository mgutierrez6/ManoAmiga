package uy.edu.um.manoamiga.error;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

/** Arma el cuerpo { "error": { code, message, details? } } y lo escribe (también desde los filtros de seguridad). */
public final class RespuestaError {

    private static final ObjectMapper JSON = new ObjectMapper();

    private RespuestaError() {}

    public static Map<String, Object> cuerpo(ApiError e) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", e.getCode());
        error.put("message", e.getMessage());
        if (e.getDetails() != null && !e.getDetails().isEmpty()) error.put("details", e.getDetails());
        return Map.of("error", error);
    }

    public static void escribir(HttpServletResponse response, ApiError e) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(e.getStatus());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.writeValueAsString(cuerpo(e)));
    }
}
