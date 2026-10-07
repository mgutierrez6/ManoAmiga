package uy.edu.um.manoamiga.web.entrada;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uy.edu.um.manoamiga.util.Textos;

/**
 * Cuerpos de los pedidos de /auth. Son la LISTA BLANCA de campos: lo que no está acá ("rol", "esJudicial",
 * "organizacionId"...) Jackson lo descarta (mass assignment). Los textos se recortan (trim) antes de validar.
 */
public final class AuthEntrada {

    private AuthEntrada() {}

    /** Contraseña: mínimo 8 caracteres (ASVS v5), máximo 64 (bcrypt solo usa los primeros 72 bytes). */
    public record Registro(
            @NotBlank @Size(max = 60) String nombre,
            @NotBlank @Size(max = 60) String apellido,
            @NotBlank @Email @Size(max = 120) String email,
            @NotNull @Size(min = 8, max = 64) String password,
            @Pattern(regexp = "^$|^\\+?[0-9 ]{6,20}$", message = "teléfono inválido") String telefono) {
        public Registro {
            nombre = Textos.trim(nombre);
            apellido = Textos.trim(apellido);
            email = Textos.minusculas(email);
            telefono = Textos.trim(telefono);
        }
    }

    public record Login(@NotBlank @Size(max = 120) String email, @NotNull @Size(max = 200) String password) {
        public Login {
            email = Textos.minusculas(email);
        }
    }

    public record Refresh(@NotNull @Pattern(regexp = "^[a-fA-F0-9]{128}$", message = "refresh token inválido") String refreshToken) {}

    public record Logout(@Pattern(regexp = "^[a-fA-F0-9]{128}$", message = "refresh token inválido") String refreshToken) {}

    public record Reauth(@NotNull @Size(max = 200) String password) {}
}
