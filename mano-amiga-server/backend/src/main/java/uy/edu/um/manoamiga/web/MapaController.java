package uy.edu.um.manoamiga.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Pattern;
import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.error.ApiError;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.servicio.MapaService;
import uy.edu.um.manoamiga.util.Ids;

/**
 * GET /api/v1/mapa/puntos: público (lista blanca de la matriz). Si llega con una sesión válida se aplica
 * ABAC (el principal viene cargado); si el token es inválido sigue como anónimo (nunca 401).
 */
@RestController
@RequestMapping("/api/v1/mapa")
public class MapaController {

    /** bbox = "minLng,minLat,maxLng,maxLat" (la porción de mapa que se ve en pantalla). */
    private static final String BBOX = "^-?\\d{1,3}(\\.\\d+)?,-?\\d{1,2}(\\.\\d+)?,-?\\d{1,3}(\\.\\d+)?,-?\\d{1,2}(\\.\\d+)?$";

    private final MapaService mapa;

    public MapaController(MapaService mapa) {
        this.mapa = mapa;
    }

    @GetMapping("/puntos")
    public Dto.Lista<Object> puntos(@AuthenticationPrincipal UsuarioActual usuario,
            @RequestParam(defaultValue = "todos") @Pattern(regexp = "^(todos|actividad|acopio)$", message = "tipo inválido") String tipo,
            @RequestParam(required = false) @Pattern(regexp = Ids.REGEX, message = "id inválido") String etiqueta,
            @RequestParam(required = false) @Pattern(regexp = BBOX, message = "bbox debe ser \"minLng,minLat,maxLng,maxLat\"") String bbox) {
        return mapa.puntos(usuario, tipo, etiqueta, caja(bbox));
    }

    private static MapaService.Caja caja(String bbox) {
        if (bbox == null) return null;
        String[] p = bbox.split(",");
        double minLng = Double.parseDouble(p[0]);
        double minLat = Double.parseDouble(p[1]);
        double maxLng = Double.parseDouble(p[2]);
        double maxLat = Double.parseDouble(p[3]);
        boolean enRango = Math.abs(minLng) <= 180 && Math.abs(maxLng) <= 180 && Math.abs(minLat) <= 90 && Math.abs(maxLat) <= 90;
        if (!enRango || minLng >= maxLng || minLat >= maxLat) {
            throw ApiError.validacion(List.of(new ApiError.Detalle("bbox", "bbox fuera de rango")));
        }
        return new MapaService.Caja(minLng, minLat, maxLng, maxLat);
    }
}
