package uy.edu.um.manoamiga.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.servicio.AsistenciaService;
import uy.edu.um.manoamiga.servicio.CertificadoService;
import uy.edu.um.manoamiga.servicio.InscripcionService;
import uy.edu.um.manoamiga.util.Ctx;
import uy.edu.um.manoamiga.util.Ids;

/** /api/v1/inscripciones/:id y /api/v1/asistencias/:id. */
@RestController
@RequestMapping("/api/v1")
public class InscripcionController {

    public record CambioEstado(@NotNull @Pattern(regexp = "^(aceptada|rechazada)$", message = "estado inválido") String estado) {}

    public record AsistenciaManual(@DecimalMin("0.25") @DecimalMax("24") Double horas) {}

    private final InscripcionService inscripciones;
    private final AsistenciaService asistencias;
    private final CertificadoService certificados;

    public InscripcionController(InscripcionService inscripciones, AsistenciaService asistencias, CertificadoService certificados) {
        this.inscripciones = inscripciones;
        this.asistencias = asistencias;
        this.certificados = certificados;
    }

    /** Solo el titular (RS2). */
    @DeleteMapping("/inscripciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id, HttpServletRequest req) {
        inscripciones.cancelar(u, id, Ctx.de(req));
    }

    /** Coordinador de la organización (RS3). */
    @PatchMapping("/inscripciones/{id}")
    public Dto.InscripcionCorta cambiarEstado(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id,
            @Valid @RequestBody CambioEstado body, HttpServletRequest req) {
        return inscripciones.cambiarEstado(u, id, body.estado(), Ctx.de(req));
    }

    /** PDF del certificado: titular (RS2) o coordinador de la organización (RS3). */
    @GetMapping("/inscripciones/{id}/certificado")
    public ResponseEntity<byte[]> certificado(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id, HttpServletRequest req) {
        InscripcionService.DatosCertificado datos = inscripciones.datosCertificado(u, id, Ctx.de(req));
        byte[] pdf = certificados.generarPdf(datos);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificado-" + datos.codigo() + ".pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf);
    }

    /** El coordinador registra y valida la asistencia sin QR (R4: "QR o manual"). */
    @PostMapping("/inscripciones/{id}/asistencia-manual")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.AsistenciaDto asistenciaManual(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id,
            @Valid @RequestBody(required = false) AsistenciaManual body, HttpServletRequest req) {
        return asistencias.registrarManual(u, id, body == null ? null : body.horas(), Ctx.de(req));
    }

    @PatchMapping("/asistencias/{id}/validar")
    public Dto.AsistenciaDto validar(@AuthenticationPrincipal UsuarioActual u, @PathVariable @Pattern(regexp = Ids.REGEX, message = "id inválido") String id, HttpServletRequest req) {
        return asistencias.validar(u, id, Ctx.de(req));
    }
}
