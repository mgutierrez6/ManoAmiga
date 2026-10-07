package uy.edu.um.manoamiga.util;

import java.util.List;
import java.util.function.Function;

/** Respuesta de los listados: { data, page, limit, total } (3.1). */
public record Pagina<T>(List<T> data, int page, int limit, long total) {

    public <R> Pagina<R> map(Function<T, R> f) {
        return new Pagina<>(data.stream().map(f).toList(), page, limit, total);
    }

    /** Cuántos documentos saltear para la página pedida. */
    public static long saltear(int page, int limit) {
        return (long) (page - 1) * limit;
    }
}
