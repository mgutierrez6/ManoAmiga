package uy.edu.um.manoamiga.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.servicio.ComunidadService;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Pagina;
import uy.edu.um.manoamiga.web.entrada.ComunidadEntrada;

/** Campañas y feed de proyectos. Los GET son públicos (RS8: están en la lista blanca de la matriz). */
@RestController
@RequestMapping("/api/v1")
public class ComunidadController {

    private final ComunidadService comunidad;

    public ComunidadController(ComunidadService comunidad) {
        this.comunidad = comunidad;
    }

    @GetMapping("/campanas")
    public Pagina<Dto.CampanaDto> campanas(@RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return comunidad.listarCampanas(page, limit);
    }

    @PostMapping("/campanas")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.CampanaDto crearCampana(@AuthenticationPrincipal UsuarioActual u, @Valid @RequestBody ComunidadEntrada.Campana body, HttpServletRequest req) {
        return comunidad.crearCampana(u, body, Ctx.de(req));
    }

    @GetMapping("/proyectos/{id}/publicaciones")
    public Pagina<Dto.PublicacionDto> publicaciones(@PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return comunidad.listarPublicaciones(id, page, limit);
    }

    @PostMapping("/proyectos/{id}/publicaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.PublicacionDto publicar(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id,
            @Valid @RequestBody ComunidadEntrada.Publicacion body, HttpServletRequest req) {
        return comunidad.crearPublicacion(u, id, body, Ctx.de(req));
    }
}
