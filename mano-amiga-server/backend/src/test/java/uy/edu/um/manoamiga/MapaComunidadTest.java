package uy.edu.um.manoamiga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;

/** Mapa (R5 + RS7 + ABAC con sesión opcional) y comunidad: campañas y feed (R6). */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MapaComunidadTest extends BaseTest {

    static final String MONTEVIDEO = "-56.3,-34.95,-56.0,-34.8";

    Escenario s;
    String normal;
    String sensible;
    String campana;

    @BeforeAll
    void preparar() throws Exception {
        s = Escenario.armar(this);
        normal = crearActividad(s.coordA(), Map.of("titulo", "Plantación de árboles", "etiquetas", List.of(s.normal().getId().toHexString()),
                "ubicacion", Map.of("lat", -34.9, "lng", -56.16))).path("id").asText();
        sensible = crearActividad(s.coordA(), Map.of("titulo", "Apoyo escolar", "etiquetas", List.of(s.sensible().getId().toHexString()),
                "ubicacion", Map.of("lat", -34.88, "lng", -56.18))).path("id").asText();
        crearActividad(s.coordB(), Map.of("titulo", "Lejos, en Salto", "ubicacion", Map.of("lat", -31.38, "lng", -57.96)));
        MvcResult c = hacer(con(json(post("/api/v1/campanas"), Map.of(
                "titulo", "Campaña de abrigo",
                "descripcion", "Juntamos ropa de abrigo para el invierno.",
                "puntos", List.of(
                        Map.of("nombre", "Club del barrio", "direccion", "Av. Italia 1234", "ubicacion", Map.of("lat", -34.89, "lng", -56.15)),
                        Map.of("nombre", "Punto en Salto", "ubicacion", Map.of("lat", -31.39, "lng", -57.95))),
                "fechaInicio", Instant.now().toString(),
                "fechaFin", Instant.now().plus(7, ChronoUnit.DAYS).toString())), s.coordB()));
        assertThat(c.getResponse().getStatus()).isEqualTo(201);
        campana = cuerpo(c).path("id").asText();
    }

    private JsonNode puntos(String query) throws Exception {
        return cuerpo(hacer(get("/api/v1/mapa/puntos" + query)));
    }

    @Test
    @DisplayName("mapa público: sin token trae actividades y puntos de acopio")
    void publico() throws Exception {
        Set<String> tipos = new HashSet<>();
        puntos("").path("data").forEach(p -> tipos.add(p.path("tipo").asText()));
        assertThat(tipos).containsExactlyInAnyOrder("actividad", "acopio");
    }

    @Test
    @DisplayName("RS7: no expone datos personales")
    void sinDatosPersonales() throws Exception {
        String texto = hacer(get("/api/v1/mapa/puntos")).getResponse().getContentAsString();
        assertThat(texto).doesNotContainPattern("(?i)email|telefono|apellido|creadaPor|voluntario|inscripciones");
        for (JsonNode p : puntos("").path("data")) {
            List<String> claves = new ArrayList<>();
            p.fieldNames().forEachRemaining(claves::add);
            if (p.path("tipo").asText().equals("actividad")) {
                assertThat(claves).containsExactlyInAnyOrder("tipo", "id", "titulo", "organizacion", "etiquetas", "direccion", "ubicacion", "fechaInicio", "fechaFin", "cuposLibres");
            } else {
                assertThat(claves).containsExactlyInAnyOrder("tipo", "id", "campanaId", "titulo", "campana", "organizacion", "direccion", "ubicacion", "fechaFin");
            }
        }
    }

    @Test
    @DisplayName("bbox: solo lo que cae dentro; tipo y bbox inválidos → 400")
    void bbox() throws Exception {
        List<String> titulos = new ArrayList<>();
        puntos("?bbox=" + MONTEVIDEO).path("data").forEach(p -> titulos.add(p.path("titulo").asText()));
        assertThat(titulos).contains("Plantación de árboles", "Apoyo escolar", "Club del barrio");
        assertThat(titulos).doesNotContain("Lejos, en Salto", "Punto en Salto");

        puntos("?tipo=acopio").path("data").forEach(p -> assertThat(p.path("tipo").asText()).isEqualTo("acopio"));
        assertThat(status(get("/api/v1/mapa/puntos?bbox=1,2,3"))).isEqualTo(400);
        assertThat(status(get("/api/v1/mapa/puntos?bbox=10,10,5,5"))).isEqualTo(400);
        assertThat(status(get("/api/v1/mapa/puntos?tipo=otro"))).isEqualTo(400);
    }

    @Test
    @DisplayName("ABAC: con sesión de un judicial NO aparece la actividad sensible; token roto → anónimo")
    void abac() throws Exception {
        List<String> anonimo = new ArrayList<>();
        puntos("?tipo=actividad").path("data").forEach(p -> anonimo.add(p.path("id").asText()));
        assertThat(anonimo).contains(sensible);

        List<String> judicial = new ArrayList<>();
        cuerpo(hacer(getCon("/api/v1/mapa/puntos?tipo=actividad", s.judicial()))).path("data").forEach(p -> judicial.add(p.path("id").asText()));
        assertThat(judicial).contains(normal).doesNotContain(sensible);

        assertThat(status(get("/api/v1/mapa/puntos").header("Authorization", "Bearer basura"))).isEqualTo(200);
    }

    @Test
    @DisplayName("campañas: un voluntario no publica; el listado es público y sin datos personales")
    void campanas() throws Exception {
        assertThat(status(con(json(post("/api/v1/campanas"), Map.of("titulo", "x")), s.vol1()))).isEqualTo(403);
        MvcResult r = hacer(get("/api/v1/campanas"));
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(r.getResponse().getContentAsString()).doesNotContainPattern("(?i)email|telefono|apellido");
    }

    @Test
    @DisplayName("feed: el coordinador publica en su proyecto; otro coordinador no; proyecto inexistente → 404")
    void feed() throws Exception {
        assertThat(status(con(json(post("/api/v1/proyectos/" + normal + "/publicaciones"), Map.of("contenido", "¡Gracias a todos!")), s.coordA()))).isEqualTo(201);
        assertThat(status(con(json(post("/api/v1/proyectos/" + normal + "/publicaciones"), Map.of("contenido", "hackeo")), s.coordB()))).isEqualTo(403);
        assertThat(status(con(json(post("/api/v1/proyectos/" + campana + "/publicaciones"), Map.of("tipo", "convocatoria", "contenido", "Faltan camperas")), s.coordB()))).isEqualTo(201);

        JsonNode feed = cuerpo(hacer(get("/api/v1/proyectos/" + normal + "/publicaciones")));
        assertThat(feed.path("data").get(0).path("autor").path("nombre").asText()).isNotEmpty();
        assertThat(feed.path("data").get(0).path("autor").has("apellido")).isFalse();
        assertThat(status(get("/api/v1/proyectos/" + ID_FALSO + "/publicaciones"))).isEqualTo(404);
    }
}
