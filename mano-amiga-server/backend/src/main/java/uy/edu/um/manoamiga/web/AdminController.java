package uy.edu.um.manoamiga.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import jakarta.validation.constraints.Size;
import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.seguridad.RequiereReauth;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.servicio.AdminService;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Pagina;
import uy.edu.um.manoamiga.web.entrada.AdminEntrada;

/**
 * Panel de administración (R7). Cadena: rol admin (matriz, RS1) -> reautenticación en las escrituras
 * (@RequiereReauth, RS9) -> validación -> service (auditoría + revocación RS5).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/organizaciones")
    public Pagina<Dto.OrganizacionDto> organizaciones(@RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return admin.listarOrganizaciones(page, limit);
    }

    @RequiereReauth
    @PostMapping("/organizaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.OrganizacionDto crearOrganizacion(@AuthenticationPrincipal UsuarioActual u, @Valid @RequestBody AdminEntrada.Organizacion body, HttpServletRequest req) {
        return admin.crearOrganizacion(u, body, Ctx.de(req));
    }

    @RequiereReauth
    @DeleteMapping("/organizaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bajaOrganizacion(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id, HttpServletRequest req) {
        admin.bajaOrganizacion(u, id, Ctx.de(req));
    }

    @GetMapping("/usuarios")
    public Pagina<Dto.UsuarioParaAdmin> usuarios(@RequestParam(required = false) @Size(max = 60) String buscar,
            @RequestParam(required = false) @Pattern(regexp = "^(voluntario|coordinador|admin)$", message = "rol inválido") String rol,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return admin.listarUsuarios(buscar, rol, page, limit);
    }

    @RequiereReauth
    @PostMapping("/coordinadores")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.UsuarioParaAdmin asignarCoordinador(@AuthenticationPrincipal UsuarioActual u, @Valid @RequestBody AdminEntrada.Coordinador body, HttpServletRequest req) {
        return admin.asignarCoordinador(u, body, Ctx.de(req));
    }

    @RequiereReauth
    @DeleteMapping("/coordinadores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarCoordinador(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id, HttpServletRequest req) {
        admin.quitarCoordinador(u, id, Ctx.de(req));
    }

    @RequiereReauth
    @PatchMapping("/voluntarios/{id}/judicial")
    public Dto.UsuarioParaAdmin judicial(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id,
            @Valid @RequestBody AdminEntrada.Judicial body, HttpServletRequest req) {
        return admin.actualizarJudicial(u, id, body, Ctx.de(req));
    }

    @GetMapping("/judiciales/horas")
    public Pagina<Dto.HorasJudicial> horasJudiciales(@RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return admin.horasJudiciales(page, limit);
    }

    @GetMapping("/auditoria")
    public Pagina<Dto.EventoAuditoria> auditoria(@RequestParam(required = false) @Size(max = 40) String accion,
            @RequestParam(required = false) @Pattern(regexp = "^(ok|fallo|denegado)$", message = "resultado inválido") String resultado,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return admin.listarAuditoria(accion, resultado, page, limit);
    }
}
