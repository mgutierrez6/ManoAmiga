package uy.edu.um.manoamiga.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Datos de contexto que los services necesitan para la auditoría, sin pasarles el request (6.3).
 * La IP es la del cliente real: Tomcat la toma de X-Forwarded-For solo si el pedido viene del front-service
 * (server.forward-headers-strategy=native).
 */
public record Ctx(String ip, String dispositivo) {

    public static Ctx de(HttpServletRequest req) {
        String ua = req.getHeader("User-Agent");
        return new Ctx(req.getRemoteAddr(), ua == null ? "" : ua);
    }

    /** Dispositivo recortado (nunca se guarda un User-Agent gigante). */
    public String dispositivo(int max) {
        return dispositivo.length() > max ? dispositivo.substring(0, max) : dispositivo;
    }
}
