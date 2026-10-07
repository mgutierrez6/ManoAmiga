package uy.edu.um.manoamiga.web.entrada;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uy.edu.um.manoamiga.util.Textos;

public final class ComunidadEntrada {

    private ComunidadEntrada() {}

    public record Punto(
            @NotBlank @Size(min = 2, max = 100) String nombre,
            @Size(max = 200) String direccion,
            @NotNull @Valid ActividadEntrada.Ubicacion ubicacion) {
        public Punto {
            nombre = Textos.trim(nombre);
            direccion = Textos.trim(direccion);
        }
    }

    /** POST /campanas. La organización sale del coordinador autenticado. */
    public record Campana(
            @NotBlank @Size(min = 3, max = 100) String titulo,
            @NotBlank @Size(min = 10, max = 2000) String descripcion,
            @Size(max = 300) String objetivo,
            @Size(max = 20) List<@Valid @NotNull Punto> puntos,
            @NotNull Instant fechaInicio,
            @NotNull Instant fechaFin) {
        public Campana {
            titulo = Textos.trim(titulo);
            descripcion = Textos.trim(descripcion);
            objetivo = Textos.trim(objetivo);
            puntos = puntos == null ? List.of() : puntos;
        }

        @AssertTrue(message = "fechaFin debe ser posterior a fechaInicio")
        public boolean isFechaFinPosterior() {
            return fechaInicio == null || fechaFin == null || fechaFin.isAfter(fechaInicio);
        }
    }

    /** El contenido es texto plano: la app lo muestra con <Text>, que no interpreta HTML. */
    public record Publicacion(
            @Pattern(regexp = "^(post|convocatoria)$", message = "tipo inválido") String tipo,
            @NotBlank @Size(min = 1, max = 2000) String contenido) {
        public Publicacion {
            tipo = tipo == null ? "post" : tipo;
            contenido = Textos.trim(contenido);
        }
    }
}
