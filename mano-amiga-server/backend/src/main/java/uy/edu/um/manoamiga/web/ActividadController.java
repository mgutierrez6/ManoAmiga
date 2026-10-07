package uy.edu.um.manoamiga.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.servicio.ActividadService;
import uy.edu.um.manoamiga.servicio.AsistenciaService;
import uy.edu.um.manoamiga.servicio.InscripcionService;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Pagina;
import uy.edu.um.manoamiga.web.entrada.ActividadEntrada;

/**
 * /api/v1/actividades (+ /etiquetas). El rol lo controla la matriz; la pertenencia a la organización (RS3),
 * la titularidad (RS2) y ABAC se controlan en los services.
 */
@RestController
@RequestMapping("/api/v1")
public class ActividadController {

    /** Cuerpo de POST /actividades/:id/asistencias. */
    public record CodigoQr(@NotBlank @Size(max = 1000) String qr) {}

    private final ActividadService actividades;
    private final InscripcionService inscripciones;
    private final AsistenciaService asistencias;

    public ActividadController(ActividadService actividades, InscripcionService inscripciones, AsistenciaService asistencias) {
        this.actividades = actividades;
        this.inscripciones = inscripciones;
        this.asistencias = asistencias;
    }

    /** Pública (RS8). */
    @GetMapping("/etiquetas")
    public List<Dto.EtiquetaDto> etiquetas() {
        return actividades.listarEtiquetas();
    }

    @GetMapping("/actividades")
    public Pagina<Dto.ActividadDto> listar(@AuthenticationPrincipal UsuarioActual u,
            @RequestParam(required = false) @Pattern(regexp = Ids.REGEX, message = "id inválido") String etiqueta,
            @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") Double lat,
            @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") Double lng,
            @RequestParam(defaultValue = "10") @DecimalMin("0.1") @DecimalMax("200") double radio,
            @RequestParam(defaultValue = "false") boolean mias,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        if ((lat == null) != (lng == null)) {
            throw ApiError.validacion(List.of(new ApiError.Detalle("lat", "lat y lng van juntos")));
        }
        return actividades.listar(u, etiqueta, lat, lng, radio, mias, page, limit);
    }

    @PostMapping("/actividades")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.ActividadDto crear(@AuthenticationPrincipal UsuarioActual u, @Valid @RequestBody ActividadEntrada.Crear body, HttpServletRequest req) {
        return actividades.crear(u, body, Ctx.de(req));
    }

    @GetMapping("/actividades/{id}")
    public Dto.ActividadDto obtener(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id, HttpServletRequest req) {
        return actividades.obtener(u, id, Ctx.de(req));
    }

    @PatchMapping("/actividades/{id}")
    public Dto.ActividadDto actualizar(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id,
            @Valid @RequestBody ActividadEntrada.Actualizar body, HttpServletRequest req) {
        return actividades.actualizar(u, id, body, Ctx.de(req));
    }

    @DeleteMapping("/actividades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id, HttpServletRequest req) {
        actividades.eliminar(u, id, Ctx.de(req));
    }

    /* ----- Inscripciones de una actividad ----- */

    @PostMapping("/actividades/{id}/inscripciones")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.InscripcionCorta inscribir(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id, HttpServletRequest req) {
        return inscripciones.inscribir(u, id, Ctx.de(req));
    }

    @GetMapping("/actividades/{id}/inscripciones")
    public List<Dto.InscripcionParaCoordinador> inscriptos(@AuthenticationPrincipal UsuarioActual u,
            @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id,
            @RequestParam(required = false) @Pattern(regexp = "^(pendiente|aceptada|rechazada|cancelada)$", message = "estado inválido") String estado,
            HttpServletRequest req) {
        return inscripciones.listarPorActividad(u, id, estado, Ctx.de(req));
    }

    /* ----- Asistencias / QR ----- */

    @GetMapping("/actividades/{id}/qr")
    public ResponseEntity<Dto.Qr> qr(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id, HttpServletRequest req) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(asistencias.generarQR(u, id, Ctx.de(req)));
    }

    @PostMapping("/actividades/{id}/asistencias")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.AsistenciaDto checkIn(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id,
            @Valid @RequestBody CodigoQr body, HttpServletRequest req) {
        return asistencias.registrarConQR(u, id, body.qr().strip(), Ctx.de(req));
    }

    @GetMapping("/actividades/{id}/asistencias")
    public List<Dto.AsistenciaDto> asistencias(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id, HttpServletRequest req) {
        return asistencias.listarPorActividad(u, id, Ctx.de(req));
    }
}
