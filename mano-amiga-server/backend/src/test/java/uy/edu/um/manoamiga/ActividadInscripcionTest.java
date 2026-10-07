package uy.edu.um.manoamiga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;

import uy.edu.um.manoamiga.modelo.Actividad;
import uy.edu.um.manoamiga.modelo.Auditoria;

/** Actividades (R2), inscripciones (R3), gestión de participantes (R4) + RS2, RS3 y ABAC. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActividadInscripcionTest extends BaseTest {

    Escenario s;

    @BeforeAll
    void preparar() {
        s = Escenario.armar(this);
    }

    private MvcResult inscribir(Prueba vol, String actId) throws Exception {
        return hacer(con(post("/api/v1/actividades/" + actId + "/inscripciones"), vol));
    }

    @Nested
    @DisplayName("Crear y editar actividades (R2)")
    class Crear {

        @Test
        @DisplayName("la organización se toma del token, no del body")
        void orgDelToken() throws Exception {
            JsonNode act = crearActividad(s.coordA(), Map.of("organizacionId", s.orgB().getId().toHexString()));
            assertThat(act.path("organizacionId").asText()).isEqualTo(s.orgA().getId().toHexString());
        }

        @Test
        @DisplayName("datos inválidos → 400 (fin antes que inicio, cupo 0)")
        void invalidos() throws Exception {
            String inicio = Instant.now().plus(1, ChronoUnit.DAYS).toString();
            MvcResult r = hacer(con(json(post("/api/v1/actividades"),
                    datosActividad(Map.of("fechaInicio", inicio, "fechaFin", Instant.now().toString(), "cupo", 0))), s.coordA()));
            assertThat(r.getResponse().getStatus()).isEqualTo(400);
            List<String> campos = new ArrayList<>();
            cuerpo(r).path("error").path("details").forEach(d -> campos.add(d.path("campo").asText()));
            assertThat(campos).contains("cupo", "fechaFinPosterior");
        }

        @Test
        @DisplayName("etiqueta inexistente → 400")
        void etiquetaInexistente() throws Exception {
            assertThat(status(con(json(post("/api/v1/actividades"), datosActividad(Map.of("etiquetas", List.of(ID_FALSO)))), s.coordA()))).isEqualTo(400);
        }

        @Test
        @DisplayName("el coordinador edita las actividades de su organización")
        void editar() throws Exception {
            JsonNode act = crearActividad(s.coordA());
            MvcResult r = hacer(con(json(patch("/api/v1/actividades/" + act.path("id").asText()), Map.of("titulo", "Nuevo título")), s.coordA()));
            assertThat(r.getResponse().getStatus()).isEqualTo(200);
            assertThat(cuerpo(r).path("titulo").asText()).isEqualTo("Nuevo título");
        }

        @Test
        @DisplayName(":id que no es ObjectId → 400; inexistente → 404")
        void ids() throws Exception {
            assertThat(status(getCon("/api/v1/actividades/123", s.vol1()))).isEqualTo(400);
            assertThat(status(getCon("/api/v1/actividades/" + ID_FALSO, s.vol1()))).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("RS3 · aislamiento entre organizaciones (A2)")
    class Organizaciones {

        @Test
        @DisplayName("otro coordinador NO puede editar, cancelar, ver inscriptos ni generar el QR")
        void otraOrg() throws Exception {
            String id = crearActividad(s.coordA()).path("id").asText();
            assertThat(status(con(json(patch("/api/v1/actividades/" + id), Map.of("titulo", "hackeado")), s.coordB()))).isEqualTo(403);
            assertThat(status(con(delete("/api/v1/actividades/" + id), s.coordB()))).isEqualTo(403);
            assertThat(status(getCon("/api/v1/actividades/" + id + "/inscripciones", s.coordB()))).isEqualTo(403);
            assertThat(status(getCon("/api/v1/actividades/" + id + "/qr", s.coordB()))).isEqualTo(403);
        }

        @Test
        @DisplayName("\"mias\" solo trae las de su organización; un voluntario no puede usarlo")
        void mias() throws Exception {
            crearActividad(s.coordA());
            JsonNode lista = cuerpo(hacer(getCon("/api/v1/actividades?mias=true", s.coordB())));
            lista.path("data").forEach(a -> assertThat(a.path("organizacionId").asText()).isEqualTo(s.orgB().getId().toHexString()));
            assertThat(status(getCon("/api/v1/actividades?mias=true", s.vol1()))).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("ABAC · voluntario judicial y etiquetas sensibles")
    class Abac {

        @Test
        @DisplayName("el judicial no ve, no obtiene y no se inscribe a una actividad sensible")
        void judicial() throws Exception {
            String sensible = crearActividad(s.coordA(), Map.of("etiquetas", List.of(s.sensible().getId().toHexString()))).path("id").asText();
            String normal = crearActividad(s.coordA(), Map.of("etiquetas", List.of(s.normal().getId().toHexString()))).path("id").asText();

            List<String> idsJudicial = new ArrayList<>();
            cuerpo(hacer(getCon("/api/v1/actividades?limit=50&etiqueta=" + s.sensible().getId().toHexString(), s.judicial()))).path("data").forEach(a -> idsJudicial.add(a.path("id").asText()));
            assertThat(idsJudicial).doesNotContain(sensible);

            List<String> idsComun = new ArrayList<>();
            cuerpo(hacer(getCon("/api/v1/actividades?limit=50&etiqueta=" + s.sensible().getId().toHexString(), s.vol1()))).path("data").forEach(a -> idsComun.add(a.path("id").asText()));
            assertThat(idsComun).contains(sensible);

            assertThat(status(getCon("/api/v1/actividades/" + sensible, s.judicial()))).isEqualTo(403);
            assertThat(inscribir(s.judicial(), sensible).getResponse().getStatus()).isEqualTo(403);
            assertThat(inscribir(s.judicial(), normal).getResponse().getStatus()).isEqualTo(201);
        }
    }

    @Nested
    @DisplayName("Inscripción (R3)")
    class Inscripcion {

        @Test
        @DisplayName("ok → 201 pendiente y suma un inscripto; dos veces → 409")
        void ok() throws Exception {
            String id = crearActividad(s.coordA()).path("id").asText();
            MvcResult r = inscribir(s.vol1(), id);
            assertThat(r.getResponse().getStatus()).isEqualTo(201);
            assertThat(cuerpo(r).path("estado").asText()).isEqualTo("pendiente");
            assertThat(mongo.findById(new org.bson.types.ObjectId(id), Actividad.class).getInscriptos()).isEqualTo(1);
            assertThat(inscribir(s.vol1(), id).getResponse().getStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("cupo lleno → 409 CUPO_LLENO")
        void cupoLleno() throws Exception {
            String id = crearActividad(s.coordA(), Map.of("cupo", 1)).path("id").asText();
            assertThat(inscribir(s.vol1(), id).getResponse().getStatus()).isEqualTo(201);
            MvcResult r = inscribir(s.vol2(), id);
            assertThat(r.getResponse().getStatus()).isEqualTo(409);
            assertThat(codigoError(r)).isEqualTo("CUPO_LLENO");
        }

        @Test
        @DisplayName("10 pedidos simultáneos por el último cupo: solo 1 entra")
        void concurrencia() throws Exception {
            String id = crearActividad(s.coordA(), Map.of("cupo", 1)).path("id").asText();
            List<Prueba> vols = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                vols.add(voluntario());
            }
            ExecutorService pool = Executors.newFixedThreadPool(10);
            try {
                List<Callable<Integer>> tareas = new ArrayList<>();
                for (Prueba v : vols) {
                    tareas.add(() -> inscribir(v, id).getResponse().getStatus());
                }
                int ok = 0;
                for (Future<Integer> f : pool.invokeAll(tareas)) {
                    if (f.get() == 201) {
                        ok++;
                    }
                }
                assertThat(ok).isEqualTo(1);
            } finally {
                pool.shutdown();
            }
            assertThat(mongo.findById(new org.bson.types.ObjectId(id), Actividad.class).getInscriptos()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("RS2 · un voluntario no accede a inscripciones ajenas (A1 · IDOR)")
    class Idor {

        @Test
        @DisplayName("vol2 NO puede cancelar ni descargar el certificado de vol1, y queda auditado")
        void ajena() throws Exception {
            String act = crearActividad(s.coordA()).path("id").asText();
            String ins = cuerpo(inscribir(s.vol1(), act)).path("id").asText();
            assertThat(status(con(delete("/api/v1/inscripciones/" + ins), s.vol2()))).isEqualTo(403);
            assertThat(status(getCon("/api/v1/inscripciones/" + ins + "/certificado", s.vol2()))).isEqualTo(403);
            assertThat(mongo.exists(org.springframework.data.mongodb.core.query.Query.query(
                    org.springframework.data.mongodb.core.query.Criteria.where("usuarioId").is(s.vol2().usuario().getId())
                            .and("accion").is("acceso_denegado_titularidad")), Auditoria.class)).isTrue();

            // "mis inscripciones" solo trae las propias
            cuerpo(hacer(getCon("/api/v1/usuarios/me/inscripciones", s.vol2()))).path("data")
                    .forEach(i -> assertThat(i.path("id").asText()).isNotEqualTo(ins));

            // el titular sí puede cancelar, se libera el cupo y se puede volver a inscribir
            assertThat(status(con(delete("/api/v1/inscripciones/" + ins), s.vol1()))).isEqualTo(204);
            assertThat(inscribir(s.vol1(), act).getResponse().getStatus()).isEqualTo(201);
        }
    }

    @Nested
    @DisplayName("Gestión de participantes (R4) + RS3")
    class Participantes {

        @Test
        @DisplayName("aceptar, transiciones, contacto visible y certificado")
        void flujo() throws Exception {
            String act = crearActividad(s.coordA()).path("id").asText();
            String ins = cuerpo(inscribir(s.vol1(), act)).path("id").asText();

            JsonNode lista = cuerpo(hacer(getCon("/api/v1/actividades/" + act + "/inscripciones", s.coordA())));
            assertThat(lista.get(0).path("voluntario").path("telefono").asText()).isNotEmpty();

            assertThat(status(con(json(patch("/api/v1/inscripciones/" + ins), Map.of("estado", "aceptada")), s.coordB()))).isEqualTo(403);
            assertThat(status(con(json(patch("/api/v1/inscripciones/" + ins), Map.of("estado", "aceptada")), s.vol1()))).isEqualTo(403);
            assertThat(status(con(json(patch("/api/v1/inscripciones/" + ins), Map.of("estado", "cualquiera")), s.coordA()))).isEqualTo(400);
            assertThat(status(con(json(patch("/api/v1/inscripciones/" + ins), Map.of("estado", "aceptada")), s.coordA()))).isEqualTo(200);
            assertThat(status(con(json(patch("/api/v1/inscripciones/" + ins), Map.of("estado", "aceptada")), s.coordA()))).isEqualTo(409);

            assertThat(status(getCon("/api/v1/inscripciones/" + ins + "/certificado", s.vol1()))).isEqualTo(409);
            assertThat(status(con(json(post("/api/v1/inscripciones/" + ins + "/asistencia-manual"), Map.of("horas", 2)), s.coordA()))).isEqualTo(201);

            MvcResult pdf = hacer(getCon("/api/v1/inscripciones/" + ins + "/certificado", s.vol1()));
            assertThat(pdf.getResponse().getStatus()).isEqualTo(200);
            assertThat(pdf.getResponse().getContentType()).startsWith("application/pdf");
            assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 4)).isEqualTo("%PDF");
            assertThat(status(getCon("/api/v1/inscripciones/" + ins + "/certificado", s.coordB()))).isEqualTo(403);
        }
    }

    @Test
    @DisplayName("un coordinador no puede inscribirse (rol)")
    void coordinadorNoSeInscribe() throws Exception {
        String id = crearActividad(s.coordA()).path("id").asText();
        assertThat(status(con(post("/api/v1/actividades/" + id + "/inscripciones"), s.coordB()))).isEqualTo(403);
        assertThat(status(get("/api/v1/actividades/" + id))).isEqualTo(401);
    }
}
