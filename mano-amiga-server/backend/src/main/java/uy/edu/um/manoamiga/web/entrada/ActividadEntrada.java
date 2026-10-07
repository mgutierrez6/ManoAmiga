package uy.edu.um.manoamiga.web.entrada;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Textos;

public final class ActividadEntrada {

    private ActividadEntrada() {}

    public record Ubicacion(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng) {}

    /**
     * POST /actividades. La organización NO viene en el body: se toma del coordinador autenticado.
     */
    public record Crear(
            @NotBlank @Size(min = 3, max = 100) String titulo,
            @NotBlank @Size(min = 10, max = 2000) String descripcion,
            @Size(max = 10) List<@Pattern(regexp = Ids.REGEX, message = "id inválido") String> etiquetas,
            @NotNull @Valid Ubicacion ubicacion,
            @Size(max = 200) String direccion,
            @NotNull Instant fechaInicio,
            @NotNull Instant fechaFin,
            @NotNull @Min(1) @Max(10000) Integer cupo) {

        public Crear {
            titulo = Textos.trim(titulo);
            descripcion = Textos.trim(descripcion);
            direccion = Textos.trim(direccion);
            etiquetas = etiquetas == null ? List.of() : etiquetas;
        }

        @AssertTrue(message = "fechaFin debe ser posterior a fechaInicio")
        public boolean isFechaFinPosterior() {
            return fechaInicio == null || fechaFin == null || fechaFin.isAfter(fechaInicio);
        }

        @AssertTrue(message = "las etiquetas no se pueden repetir")
        public boolean isEtiquetasUnicas() {
            return etiquetas == null || new HashSet<>(etiquetas).size() == etiquetas.size();
        }
    }

    /** PATCH /actividades/:id: todos opcionales, pero al menos uno. */
    public record Actualizar(
            @Size(min = 3, max = 100) String titulo,
            @Size(min = 10, max = 2000) String descripcion,
            @Size(max = 10) List<@Pattern(regexp = Ids.REGEX, message = "id inválido") String> etiquetas,
            @Valid Ubicacion ubicacion,
            @Size(max = 200) String direccion,
            Instant fechaInicio,
            Instant fechaFin,
            @Min(1) @Max(10000) Integer cupo) {

        public Actualizar {
            titulo = Textos.trim(titulo);
            descripcion = Textos.trim(descripcion);
            direccion = Textos.trim(direccion);
        }

        @AssertTrue(message = "las etiquetas no se pueden repetir")
        public boolean isEtiquetasUnicas() {
            return etiquetas == null || new HashSet<>(etiquetas).size() == etiquetas.size();
        }

        public boolean vacio() {
            return titulo == null && descripcion == null && etiquetas == null && ubicacion == null && direccion == null
                    && fechaInicio == null && fechaFin == null && cupo == null;
        }
    }
}
