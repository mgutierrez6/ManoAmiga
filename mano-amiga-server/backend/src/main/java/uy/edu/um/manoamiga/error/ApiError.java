package uy.edu.um.manoamiga.error;

import java.util.List;

/**
 * Error "esperado" que lanzan los services cuando una regla no se cumple (3.6).
 * ManejadorErrores lo transforma en { "error": { "code", "message", "details"? } }, igual que la versión Node.
 */
public class ApiError extends RuntimeException {

    /** Detalle de un error de validación: { "campo": "...", "motivo": "..." }. */
    public record Detalle(String campo, String motivo) {}

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String code;
    private final transient List<Detalle> details;

    public ApiError(int status, String code, String message, List<Detalle> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public ApiError(int status, String code, String message) {
        this(status, code, message, null);
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
    public List<Detalle> getDetails() { return details; }

    public static ApiError badRequest(String code, String message) { return new ApiError(400, code, message); }
    public static ApiError validacion(List<Detalle> details) { return new ApiError(400, "VALIDACION", "Datos inválidos", details); }
    public static ApiError unauthorized() { return new ApiError(401, "NO_AUTENTICADO", "Necesitás iniciar sesión"); }
    public static ApiError unauthorized(String code, String message) { return new ApiError(401, code, message); }
    public static ApiError forbidden(String code, String message) { return new ApiError(403, code, message); }
    public static ApiError notFound() { return new ApiError(404, "NO_ENCONTRADO", "El recurso no existe"); }
    public static ApiError notFound(String code, String message) { return new ApiError(404, code, message); }
    public static ApiError conflict(String code, String message) { return new ApiError(409, code, message); }
    public static ApiError demasiadosIntentos() { return new ApiError(429, "DEMASIADOS_INTENTOS", "Demasiados intentos. Probá de nuevo en 15 minutos."); }
}
