package uy.edu.um.manoamiga.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/v1/health: para monitoreo. Público y sin datos. */
@RestController
public class SaludController {

    @GetMapping("/api/v1/health")
    public Map<String, Boolean> health() {
        return Map.of("ok", true);
    }
}
