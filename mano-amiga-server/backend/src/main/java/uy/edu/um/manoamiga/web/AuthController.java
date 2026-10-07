package uy.edu.um.manoamiga.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.seguridad.Limitadores;
import uy.edu.um.manoamiga.seguridad.TokenService;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.servicio.AuthService;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.web.entrada.AuthEntrada;

/**
 * /api/v1/auth. Quién puede llamar a cada ruta lo decide la matriz (Matriz.java), antes de llegar acá.
 * register, login y refresh son públicos; logout es de cualquier rol; reauth solo del admin.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;
    private final Limitadores limitadores;

    public AuthController(AuthService auth, Limitadores limitadores) {
        this.auth = auth;
        this.limitadores = limitadores;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Dto.UsuarioSesion> register(@Valid @RequestBody AuthEntrada.Registro body, HttpServletRequest req) {
        return Map.of("usuario", auth.registrar(body, Ctx.de(req)));
    }

    /** Rate limit: 5 intentos fallidos cada 15 minutos por IP + email (5.1). */
    @PostMapping("/login")
    public Dto.Sesion login(@Valid @RequestBody AuthEntrada.Login body, HttpServletRequest req) {
        Ctx ctx = Ctx.de(req);
        String clave = ctx.ip() + "|" + body.email();
        limitadores.login().verificar(clave);
        try {
            return auth.login(body, ctx);
        } catch (ApiError e) {
            limitadores.login().registrarFallo(clave);
            throw e;
        }
    }

    /** Público: la credencial es el propio refresh token. */
    @PostMapping("/refresh")
    public Dto.Sesion refresh(@Valid @RequestBody AuthEntrada.Refresh body, HttpServletRequest req) {
        return auth.refresh(body.refreshToken(), Ctx.de(req));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal UsuarioActual usuario, @Valid @RequestBody(required = false) AuthEntrada.Logout body, HttpServletRequest req) {
        auth.logout(usuario, Ctx.de(req));
    }

    /** RS9: el admin vuelve a ingresar su contraseña y recibe un token de 5 minutos para el panel. */
    @PostMapping("/reauth")
    public TokenService.Reauth reauth(@AuthenticationPrincipal UsuarioActual usuario, @Valid @RequestBody AuthEntrada.Reauth body, HttpServletRequest req) {
        String clave = "reauth|" + usuario.id();
        limitadores.reauth().verificar(clave);
        try {
            return auth.reautenticar(usuario, body.password(), Ctx.de(req));
        } catch (ApiError e) {
            limitadores.reauth().registrarFallo(clave);
            throw e;
        }
    }
}
