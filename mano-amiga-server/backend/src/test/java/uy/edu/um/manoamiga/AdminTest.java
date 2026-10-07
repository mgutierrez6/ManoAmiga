package uy.edu.um.manoamiga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import uy.edu.um.manoamiga.modelo.Actividad;
import uy.edu.um.manoamiga.modelo.Auditoria;
import uy.edu.um.manoamiga.modelo.Organizacion;
import uy.edu.um.manoamiga.modelo.Sesion;
import uy.edu.um.manoamiga.modelo.Usuario;

/** Panel de administración (R7) + RS9 (reautenticación) + RS5 (revocación inmediata). */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminTest extends BaseTest {

    Escenario s;

    @BeforeAll
    void preparar() {
        s = Escenario.armar(this);
    }

    private MvcResult reauth(Prueba u, String password) throws Exception {
        return hacer(con(json(post("/api/v1/auth/reauth"), Map.of("password", password)), u));
    }

    /** Pedido del admin con un token de reauth recién obtenido. */
    private MockHttpServletRequestBuilder conReauth(MockHttpServletRequestBuilder b, Prueba admin) throws Exception {
        String token = cuerpo(reauth(admin, PASSWORD)).path("reauthToken").asText();
        return con(b, admin).header("X-Reauth-Token", token);
    }

    @Test
    @DisplayName("RS9 · sin X-Reauth-Token → 403 REAUTH_REQUERIDA, queda auditado y no se crea nada")
    void sinReauth() throws Exception {
        MvcResult r = hacer(con(json(post("/api/v1/admin/organizaciones"), Map.of("nombre", "Org sin reauth")), s.admin()));
        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(codigoError(r)).isEqualTo("REAUTH_REQUERIDA");
        assertThat(mongo.exists(query(where("usuarioId").is(s.admin().usuario().getId()).and("accion").is("reauth_requerida")), Auditoria.class)).isTrue();
        assertThat(mongo.exists(query(where("nombre").is("Org sin reauth")), Organizacion.class)).isFalse();
    }

    @Test
    @DisplayName("RS9 · contraseña incorrecta → 401 auditado; un voluntario no puede pedir reauth")
    void reauthFallida() throws Exception {
        assertThat(reauth(s.admin(), "otraCosa123").getResponse().getStatus()).isEqualTo(401);
        assertThat(mongo.exists(query(where("usuarioId").is(s.admin().usuario().getId()).and("accion").is("reauth").and("resultado").is("fallo")), Auditoria.class)).isTrue();
        assertThat(reauth(s.vol1(), PASSWORD).getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("RS9 · el token de reauth de OTRO admin no sirve, ni un access token")
    void tokenAjeno() throws Exception {
        Prueba otro = admin();
        String ajeno = cuerpo(reauth(otro, PASSWORD)).path("reauthToken").asText();
        assertThat(status(con(json(post("/api/v1/admin/organizaciones"), Map.of("nombre", "Org ajena")), s.admin()).header("X-Reauth-Token", ajeno))).isEqualTo(403);
        assertThat(status(con(json(post("/api/v1/admin/organizaciones"), Map.of("nombre", "Org access")), s.admin()).header("X-Reauth-Token", s.admin().token()))).isEqualTo(403);
    }

    @Test
    @DisplayName("con reauth válida crea la organización (201), queda auditado y no se duplica")
    void altaOrganizacion() throws Exception {
        String nombre = "Fundación " + unico();
        MvcResult r = hacer(conReauth(json(post("/api/v1/admin/organizaciones"), Map.of("nombre", nombre, "convenioOSLA", true)), s.admin()));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        JsonNode o = cuerpo(r);
        assertThat(o.path("convenioOSLA").asBoolean()).isTrue();
        assertThat(o.path("activa").asBoolean()).isTrue();
        assertThat(mongo.exists(query(where("accion").is("admin_org_alta").and("recursoId").is(o.path("id").asText())), Auditoria.class)).isTrue();
        assertThat(status(conReauth(json(post("/api/v1/admin/organizaciones"), Map.of("nombre", nombre)), s.admin()))).isEqualTo(409);
    }

    @Test
    @DisplayName("las lecturas del panel no piden reauth")
    void lecturas() throws Exception {
        for (String ruta : List.of("/api/v1/admin/organizaciones", "/api/v1/admin/usuarios", "/api/v1/admin/judiciales/horas", "/api/v1/admin/auditoria")) {
            assertThat(status(getCon(ruta, s.admin()))).as(ruta).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("asignar coordinador; no a un judicial ni a una organización inexistente")
    void asignar() throws Exception {
        Prueba v = voluntario();
        MvcResult r = hacer(conReauth(json(post("/api/v1/admin/coordinadores"),
                Map.of("usuarioId", v.id(), "organizacionId", s.orgA().getId().toHexString())), s.admin()));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        assertThat(cuerpo(r).path("rol").asText()).isEqualTo("coordinador");
        assertThat(cuerpo(r).path("organizacion").path("id").asText()).isEqualTo(s.orgA().getId().toHexString());

        assertThat(status(conReauth(json(post("/api/v1/admin/coordinadores"),
                Map.of("usuarioId", s.judicial().id(), "organizacionId", s.orgA().getId().toHexString())), s.admin()))).isEqualTo(409);
        assertThat(status(conReauth(json(post("/api/v1/admin/coordinadores"),
                Map.of("usuarioId", s.vol2().id(), "organizacionId", ID_FALSO)), s.admin()))).isEqualTo(404);
    }

    @Test
    @DisplayName("RS5 · al quitar el rol, el token VIEJO del coordinador deja de servir al instante (401)")
    void quitarCoordinador() throws Exception {
        Prueba c = coordinador(s.orgA());
        assertThat(status(getCon("/api/v1/actividades?mias=true", c))).isEqualTo(200);
        assertThat(status(conReauth(delete("/api/v1/admin/coordinadores/" + c.id()), s.admin()))).isEqualTo(204);

        MvcResult despues = hacer(getCon("/api/v1/actividades?mias=true", c));
        assertThat(despues.getResponse().getStatus()).isEqualTo(401);
        assertThat(codigoError(despues)).isEqualTo("SESION_REVOCADA");
        Usuario u = mongo.findById(c.usuario().getId(), Usuario.class);
        assertThat(u.getRol()).isEqualTo("voluntario");
        assertThat(u.getOrganizacionId()).isNull();
        assertThat(mongo.count(query(where("userId").is(c.usuario().getId())), Sesion.class)).isZero();
    }

    @Test
    @DisplayName("RS5 · baja de organización: sus coordinadores pierden acceso y sus actividades se cancelan")
    void bajaOrganizacion() throws Exception {
        Organizacion org = crearOrganizacion();
        Prueba c = coordinador(org);
        String act = crearActividad(c).path("id").asText();
        assertThat(status(conReauth(delete("/api/v1/admin/organizaciones/" + org.getId().toHexString()), s.admin()))).isEqualTo(204);
        assertThat(status(getCon("/api/v1/usuarios/me", c))).isEqualTo(401);
        assertThat(mongo.findById(new ObjectId(act), Actividad.class).getEstado()).isEqualTo("cancelada");
        assertThat(mongo.findById(org.getId(), Organizacion.class).isActiva()).isFalse();
    }

    @Test
    @DisplayName("condición judicial: exige horas; aplica en el siguiente pedido; aparece en el reporte")
    void condicionJudicial() throws Exception {
        Prueba v = voluntario();
        assertThat(status(conReauth(json(patch("/api/v1/admin/voluntarios/" + v.id() + "/judicial"), Map.of("esJudicial", true)), s.admin()))).isEqualTo(400);
        MvcResult r = hacer(conReauth(json(patch("/api/v1/admin/voluntarios/" + v.id() + "/judicial"), Map.of("esJudicial", true, "horasAsignadas", 40)), s.admin()));
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpo(r).path("horasAsignadas").asInt()).isEqualTo(40);
        assertThat(status(getCon("/api/v1/usuarios/me/horas", v))).isEqualTo(200);

        // Se recorren todas las páginas del reporte (la base de tests puede tener muchos judiciales).
        boolean aparece = false;
        for (int pagina = 1; !aparece; pagina++) {
            JsonNode datos = cuerpo(hacer(getCon("/api/v1/admin/judiciales/horas?limit=50&page=" + pagina, s.admin()))).path("data");
            if (datos.size() == 0) break;
            for (JsonNode fila : datos) {
                if (fila.path("id").asText().equals(v.id())) {
                    aparece = true;
                    assertThat(fila.path("horasRestantes").asDouble()).isEqualTo(40.0);
                }
            }
        }
        assertThat(aparece).isTrue();
        assertThat(status(getCon("/api/v1/admin/judiciales/horas", s.coordA()))).isEqualTo(403);
    }

    @Test
    @DisplayName("búsqueda con caracteres de regex no rompe; el listado no expone hashes ni tokens")
    void listado() throws Exception {
        MvcResult r = hacer(con(get("/api/v1/admin/usuarios").param("buscar", ".*("), s.admin()));
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpo(r).path("data").size()).isZero();
        String todo = hacer(getCon("/api/v1/admin/usuarios", s.admin())).getResponse().getContentAsString();
        assertThat(todo).doesNotContain("passwordHash").doesNotContain("pushTokens").doesNotContainPattern("\\$2[aby]\\$");
    }

    @Test
    @DisplayName("la auditoría se puede filtrar por resultado")
    void auditoria() throws Exception {
        for (JsonNode e : cuerpo(hacer(getCon("/api/v1/admin/auditoria?resultado=denegado", s.admin()))).path("data")) {
            assertThat(e.path("resultado").asText()).isEqualTo("denegado");
        }
    }
}
