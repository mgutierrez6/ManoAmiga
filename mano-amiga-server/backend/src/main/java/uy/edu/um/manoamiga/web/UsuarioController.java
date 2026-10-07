package uy.edu.um.manoamiga.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.servicio.AsistenciaService;
import uy.edu.um.manoamiga.servicio.AuthService;
import uy.edu.um.manoamiga.servicio.InscripcionService;
import uy.edu.um.manoamiga.servicio.UsuarioService;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Pagina;
import uy.edu.um.manoamiga.web.entrada.UsuarioEntrada;

/** /api/v1/usuarios/me/...: todas toman el id del TOKEN, nunca de la URL (RS2). */
@RestController
@RequestMapping("/api/v1/usuarios/me")
public class UsuarioController {

    private final UsuarioService usuarios;
    private final AuthService auth;
    private final InscripcionService inscripciones;
    private final AsistenciaService asistencias;

    public UsuarioController(UsuarioService usuarios, AuthService auth, InscripcionService inscripciones, AsistenciaService asistencias) {
        this.usuarios = usuarios;
        this.auth = auth;
        this.inscripciones = inscripciones;
        this.asistencias = asistencias;
    }

    @GetMapping
    public Dto.UsuarioPropio me(@AuthenticationPrincipal UsuarioActual u) {
        return usuarios.obtenerPerfil(u);
    }

    @PatchMapping
    public Dto.UsuarioPropio actualizar(@AuthenticationPrincipal UsuarioActual u, @Valid @RequestBody UsuarioEntrada.Perfil body, HttpServletRequest req) {
        if (body.vacio()) throw ApiError.validacion(java.util.List.of(new ApiError.Detalle("body", "tiene que incluir al menos un campo")));
        return usuarios.actualizarPerfil(u, body, Ctx.de(req));
    }

    /** Devuelve una sesión nueva: las demás sesiones (otros dispositivos) quedan cerradas (RS5). */
    @PatchMapping("/password")
    public Dto.Sesion cambiarPassword(@AuthenticationPrincipal UsuarioActual u, @Valid @RequestBody UsuarioEntrada.CambiarPassword body, HttpServletRequest req) {
        return auth.cambiarPassword(u, body.actual(), body.nueva(), Ctx.de(req));
    }

    @GetMapping("/accesos")
    public Pagina<Dto.Acceso> accesos(@AuthenticationPrincipal UsuarioActual u,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return usuarios.accesos(u, page, limit);
    }

    @PostMapping("/push-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pushToken(@AuthenticationPrincipal UsuarioActual u, @Valid @RequestBody UsuarioEntrada.PushToken body) {
        usuarios.registrarPushToken(u, body.token());
    }

    @GetMapping("/inscripciones")
    public Pagina<Dto.InscripcionPropia> misInscripciones(@AuthenticationPrincipal UsuarioActual u,
            @RequestParam(required = false) @Pattern(regexp = "^(pendiente|aceptada|rechazada|cancelada)$", message = "estado inválido") String estado,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return inscripciones.misInscripciones(u, estado, page, limit);
    }

    @GetMapping("/horas")
    public Dto.Horas misHoras(@AuthenticationPrincipal UsuarioActual u) {
        return asistencias.misHoras(u);
    }
}
