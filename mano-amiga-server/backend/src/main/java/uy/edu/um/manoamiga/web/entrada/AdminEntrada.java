package uy.edu.um.manoamiga.web.entrada;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Textos;

public final class AdminEntrada {

    private AdminEntrada() {}

    public record Organizacion(
            @NotBlank @Size(min = 2, max = 100) String nombre,
            @Size(max = 120) String contacto,
            Boolean convenioOSLA) {
        public Organizacion {
            nombre = Textos.trim(nombre);
            contacto = Textos.trim(contacto);
            convenioOSLA = convenioOSLA != null && convenioOSLA;
        }
    }

    public record Coordinador(
            @NotNull @Pattern(regexp = Ids.REGEX, message = "id inválido") String usuarioId,
            @NotNull @Pattern(regexp = Ids.REGEX, message = "id inválido") String organizacionId) {}

    /** esJudicial = true exige horasAsignadas; false no las admite (se borran). */
    public record Judicial(@NotNull Boolean esJudicial, @Min(0) @Max(2000) Integer horasAsignadas) {

        @AssertTrue(message = "horasAsignadas es obligatorio si esJudicial es true, y no se admite si es false")
        public boolean isHorasCoherentes() {
            if (esJudicial == null) return true;
            return esJudicial ? horasAsignadas != null : horasAsignadas == null;
        }
    }
}
