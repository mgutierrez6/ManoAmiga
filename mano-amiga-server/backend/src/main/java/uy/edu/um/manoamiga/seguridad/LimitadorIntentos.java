package uy.edu.um.manoamiga.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import uy.edu.um.manoamiga.error.ApiError;

/**
 * Límite de intentos FALLIDOS por clave en una ventana de tiempo (5.1 / RS9).
 * Ej.: 5 logins fallidos cada 15 minutos por IP + email. Los intentos exitosos no cuentan.
 * Vive en memoria (alcanza con una sola instancia de la API; con varias habría que usar Redis).
 */
public class LimitadorIntentos {

    private record Ventana(Instant inicio, int fallos) {}

    private final int maximo;
    private final Duration ventana;
    private final Map<String, Ventana> intentos = new ConcurrentHashMap<>();

    public LimitadorIntentos(int maximo, Duration ventana) {
        this.maximo = maximo;
        this.ventana = ventana;
    }

    /** Lanza 429 si la clave ya agotó sus intentos. */
    public void verificar(String clave) {
        Ventana v = intentos.get(clave);
        if (v == null) return;
        if (v.inicio().plus(ventana).isBefore(Instant.now())) {
            intentos.remove(clave);
            return;
        }
        if (v.fallos() >= maximo) throw ApiError.demasiadosIntentos();
    }

    public void registrarFallo(String clave) {
        Instant ahora = Instant.now();
        intentos.compute(clave, (k, v) -> v == null || v.inicio().plus(ventana).isBefore(ahora)
                ? new Ventana(ahora, 1)
                : new Ventana(v.inicio(), v.fallos() + 1));
        if (intentos.size() > 100_000) intentos.clear(); // protección básica de memoria
    }

    /** Solo para los tests. */
    public void reiniciar() {
        intentos.clear();
    }
}
