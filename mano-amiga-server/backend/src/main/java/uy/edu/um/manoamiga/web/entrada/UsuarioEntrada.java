package uy.edu.um.manoamiga.web.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uy.edu.um.manoamiga.util.Textos;

public final class UsuarioEntrada {

    private UsuarioEntrada() {}

    public record PushToken(@NotBlank @Size(max = 200) String token) {
        public PushToken {
            token = Textos.trim(token);
        }
    }

    public record Preferencias(@Pattern(regexp = "^(sistema|claro|oscuro)$", message = "tema inválido") String tema) {}

    /**
     * PATCH /usuarios/me (R8): SOLO datos de contacto y el tema. Rol, email, condición judicial, horas y
     * organización no están acá, así que no se pueden cambiar (mass assignment).
     */
    public record Perfil(
            @Size(min = 1, max = 60) String nombre,
            @Size(min = 1, max = 60) String apellido,
            @Pattern(regexp = "^$|^\\+?[0-9 ]{6,20}$", message = "teléfono inválido") String telefono,
            @Valid Preferencias preferencias) {
        public Perfil {
            nombre = Textos.trim(nombre);
            apellido = Textos.trim(apellido);
            telefono = Textos.trim(telefono);
        }

        public boolean vacio() {
            return nombre == null && apellido == null && telefono == null && (preferencias == null || preferencias.tema() == null);
        }
    }

    public record CambiarPassword(@NotNull @Size(max = 200) String actual, @NotNull @Size(min = 8, max = 64) String nueva) {}
}
