package uy.edu.um.manoamiga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;

import uy.edu.um.manoamiga.modelo.Auditoria;
import uy.edu.um.manoamiga.modelo.Usuario;

/** Perfil (R8 / R16): edición sin mass assignment, contraseña con revocación (RS5), accesos propios. */
class PerfilTest extends BaseTest {

    @Test
    @DisplayName("edita nombre, teléfono y tema")
    void editar() throws Exception {
        Prueba u = voluntario();
        MvcResult r = hacer(con(json(patch("/api/v1/usuarios/me"),
                Map.of("nombre", "Lucía", "telefono", "+598 99 111 222", "preferencias", Map.of("tema", "oscuro"))), u));
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode b = cuerpo(r);
        assertThat(b.path("nombre").asText()).isEqualTo("Lucía");
        assertThat(b.path("telefono").asText()).isEqualTo("+598 99 111 222");
        assertThat(b.path("preferencias").path("tema").asText()).isEqualTo("oscuro");
    }

    @Test
    @DisplayName("mass assignment: rol, email, esJudicial, organizacionId y versionSesion se ignoran")
    void massAssignment() throws Exception {
        Prueba u = voluntario();
        Map<String, Object> body = new HashMap<>();
        body.put("nombre", "Pepe");
        body.put("rol", "admin");
        body.put("email", "otro@x.com");
        body.put("esJudicial", true);
        body.put("organizacionId", ID_FALSO);
        body.put("versionSesion", 99);
        assertThat(status(con(json(patch("/api/v1/usuarios/me"), body), u))).isEqualTo(200);
        Usuario db = mongo.findById(u.usuario().getId(), Usuario.class);
        assertThat(db.getNombre()).isEqualTo("Pepe");
        assertThat(db.getRol()).isEqualTo("voluntario");
        assertThat(db.getEmail()).isEqualTo(u.usuario().getEmail());
        assertThat(db.isEsJudicial()).isFalse();
        assertThat(db.getOrganizacionId()).isNull();
        assertThat(db.getVersionSesion()).isZero();
    }

    @Test
    @DisplayName("tema inválido → 400")
    void temaInvalido() throws Exception {
        assertThat(status(con(json(patch("/api/v1/usuarios/me"), Map.of("preferencias", Map.of("tema", "rosa"))), voluntario()))).isEqualTo(400);
    }

    @Test
    @DisplayName("contraseña actual incorrecta → 400 y no cambia nada")
    void passwordActualMal() throws Exception {
        Prueba u = voluntario();
        MvcResult r = hacer(con(json(patch("/api/v1/usuarios/me/password"), Map.of("actual", "NoEsEsta1", "nueva", "NuevaClave123")), u));
        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertThat(codigoError(r)).isEqualTo("PASSWORD_ACTUAL_INCORRECTA");
        assertThat(status(getCon("/api/v1/usuarios/me", u))).isEqualTo(200);
    }

    @Test
    @DisplayName("RS5 · al cambiarla, los tokens viejos (de todos los dispositivos) dejan de servir")
    void cambioPassword() throws Exception {
        Prueba u = voluntario();
        JsonNode otroDispositivo = login(u.usuario().getEmail(), PASSWORD);

        MvcResult r = hacer(con(json(patch("/api/v1/usuarios/me/password"), Map.of("actual", PASSWORD, "nueva", "NuevaClave123")), u));
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        String nuevoAccess = cuerpo(r).path("accessToken").asText();
        assertThat(nuevoAccess).isNotEmpty();
        assertThat(cuerpo(r).path("refreshToken").asText()).isNotEmpty();

        assertThat(status(getCon("/api/v1/usuarios/me", u))).isEqualTo(401);
        assertThat(status(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + otroDispositivo.path("accessToken").asText()))).isEqualTo(401);
        assertThat(status(json(post("/api/v1/auth/refresh"), Map.of("refreshToken", otroDispositivo.path("refreshToken").asText())))).isEqualTo(401);

        assertThat(status(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + nuevoAccess))).isEqualTo(200);
        assertThat(status(json(post("/api/v1/auth/login"), Map.of("email", u.usuario().getEmail(), "password", PASSWORD)))).isEqualTo(401);
        assertThat(status(json(post("/api/v1/auth/login"), Map.of("email", u.usuario().getEmail(), "password", "NuevaClave123")))).isEqualTo(200);
    }

    @Test
    @DisplayName("nueva igual a la actual → 400")
    void passwordRepetida() throws Exception {
        assertThat(status(con(json(patch("/api/v1/usuarios/me/password"), Map.of("actual", PASSWORD, "nueva", PASSWORD)), voluntario()))).isEqualTo(400);
    }

    @Test
    @DisplayName("RS5 · logout también invalida el access token al instante")
    void logout() throws Exception {
        Prueba u = voluntario();
        String access = login(u.usuario().getEmail(), PASSWORD).path("accessToken").asText();
        assertThat(status(json(post("/api/v1/auth/logout"), Map.of()).header("Authorization", "Bearer " + access))).isEqualTo(204);
        assertThat(status(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + access))).isEqualTo(401);
    }

    @Test
    @DisplayName("el historial de accesos muestra solo los propios")
    void accesos() throws Exception {
        Prueba a = voluntario();
        Prueba b = voluntario();
        login(a.usuario().getEmail(), PASSWORD);
        login(b.usuario().getEmail(), PASSWORD);
        JsonNode r = cuerpo(hacer(getCon("/api/v1/usuarios/me/accesos", a)));
        assertThat(r.path("data").size()).isGreaterThan(0);
        List<String> deB = mongo.find(query(where("usuarioId").is(b.usuario().getId())), Auditoria.class).stream()
                .map(Auditoria::getId).map(ObjectId::toHexString).toList();
        r.path("data").forEach(e -> assertThat(deB).doesNotContain(e.path("id").asText()));
        assertThat(r.path("data").get(0).path("accion").asText()).isEqualTo("login");
    }
}
