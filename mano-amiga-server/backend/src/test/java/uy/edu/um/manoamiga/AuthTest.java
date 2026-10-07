package uy.edu.um.manoamiga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;

import uy.edu.um.manoamiga.modelo.Auditoria;
import uy.edu.um.manoamiga.modelo.Organizacion;
import uy.edu.um.manoamiga.modelo.Usuario;

/** Registro, login, refresh con rotación, logout y relectura de permisos (5.1, 5.2, RS5). */
class AuthTest extends BaseTest {

    private Map<String, Object> registro(Map<String, Object> extra) {
        Map<String, Object> d = new HashMap<>(Map.of(
                "nombre", "Ana", "apellido", "Pérez", "email", "ana-" + unico() + "@mail.com",
                "password", PASSWORD, "telefono", "099 123 456"));
        d.putAll(extra);
        return d;
    }

    @Nested
    @DisplayName("Registro (5.1 · mass assignment)")
    class Registro {

        @Test
        @DisplayName("crea la cuenta siempre como voluntario aunque mande rol/admin/judicial")
        void siempreVoluntario() throws Exception {
            MvcResult r = hacer(json(post("/api/v1/auth/register"),
                    registro(Map.of("rol", "admin", "esJudicial", true, "organizacionId", ID_FALSO, "horasAsignadas", 999))));
            assertThat(r.getResponse().getStatus()).isEqualTo(201);
            JsonNode u = cuerpo(r).path("usuario");
            assertThat(u.path("rol").asText()).isEqualTo("voluntario");
            Usuario db = mongo.findById(oid(u), Usuario.class);
            assertThat(db.getRol()).isEqualTo("voluntario");
            assertThat(db.isEsJudicial()).isFalse();
            assertThat(db.getOrganizacionId()).isNull();
            assertThat(db.getHorasAsignadas()).isZero();
            assertThat(db.getPasswordHash()).matches("^\\$2[aby]\\$.*"); // hash bcrypt, nunca la contraseña
        }

        @Test
        @DisplayName("email repetido → 409")
        void emailRepetido() throws Exception {
            Map<String, Object> datos = registro(Map.of());
            hacer(json(post("/api/v1/auth/register"), datos));
            assertThat(status(json(post("/api/v1/auth/register"), datos))).isEqualTo(409);
        }

        @Test
        @DisplayName("contraseña corta → 400 con detalle del campo")
        void passwordCorta() throws Exception {
            MvcResult r = hacer(json(post("/api/v1/auth/register"), registro(Map.of("password", "123"))));
            assertThat(r.getResponse().getStatus()).isEqualTo(400);
            assertThat(codigoError(r)).isEqualTo("VALIDACION");
            assertThat(cuerpo(r).path("error").path("details").get(0).path("campo").asText()).isEqualTo("password");
        }
    }

    @Nested
    @DisplayName("Login")
    class Login {

        @Test
        @DisplayName("ok devuelve access + refresh token y no devuelve el hash")
        void ok() throws Exception {
            Prueba u = voluntario();
            MvcResult r = hacer(json(post("/api/v1/auth/login"), Map.of("email", u.usuario().getEmail(), "password", PASSWORD)));
            assertThat(r.getResponse().getStatus()).isEqualTo(200);
            JsonNode b = cuerpo(r);
            assertThat(b.path("accessToken").asText()).isNotEmpty();
            assertThat(b.path("refreshToken").asText()).hasSize(128);
            assertThat(r.getResponse().getContentAsString()).doesNotContain("passwordHash");
        }

        @Test
        @DisplayName("mensaje genérico: mismo código si el email no existe o la contraseña es incorrecta")
        void mensajeGenerico() throws Exception {
            Prueba u = voluntario();
            MvcResult a = hacer(json(post("/api/v1/auth/login"), Map.of("email", u.usuario().getEmail(), "password", "incorrecta1")));
            MvcResult b = hacer(json(post("/api/v1/auth/login"), Map.of("email", "noexiste-" + unico() + "@mail.com", "password", "incorrecta1")));
            assertThat(a.getResponse().getStatus()).isEqualTo(401);
            assertThat(b.getResponse().getStatus()).isEqualTo(401);
            assertThat(codigoError(a)).isEqualTo(codigoError(b));
        }

        @Test
        @DisplayName("inyección NoSQL en el login no funciona")
        void inyeccion() throws Exception {
            Prueba u = voluntario();
            MvcResult r = hacer(json(post("/api/v1/auth/login"), Map.of("email", u.usuario().getEmail(), "password", Map.of("$ne", ""))));
            assertThat(r.getResponse().getStatus()).isIn(400, 401);
            assertThat(cuerpo(r).has("accessToken")).isFalse();
        }

        @Test
        @DisplayName("se registra en la auditoría (éxito y fallo)")
        void auditoria() throws Exception {
            Prueba u = voluntario();
            hacer(json(post("/api/v1/auth/login"), Map.of("email", u.usuario().getEmail(), "password", "mal-password")));
            hacer(json(post("/api/v1/auth/login"), Map.of("email", u.usuario().getEmail(), "password", PASSWORD)));
            List<String> resultados = mongo.find(query(where("usuarioId").is(u.usuario().getId()).and("accion").is("login")), Auditoria.class)
                    .stream().map(Auditoria::getResultado).sorted().toList();
            assertThat(resultados).containsExactly("fallo", "ok");
        }

        @Test
        @DisplayName("más de 5 intentos fallidos → 429")
        void limite() throws Exception {
            Prueba u = voluntario();
            int ultimo = 0;
            for (int i = 0; i < 6; i++) {
                ultimo = status(json(post("/api/v1/auth/login"), Map.of("email", u.usuario().getEmail(), "password", "incorrecta1")));
            }
            assertThat(ultimo).isEqualTo(429);
        }
    }

    @Nested
    @DisplayName("Refresh token con rotación (5.2)")
    class Refresh {

        @Test
        @DisplayName("rota el token y detecta el reuso de uno viejo (cierra todas las sesiones)")
        void rotacion() throws Exception {
            Prueba u = voluntario();
            String viejo = login(u.usuario().getEmail(), PASSWORD).path("refreshToken").asText();

            MvcResult r1 = hacer(json(post("/api/v1/auth/refresh"), Map.of("refreshToken", viejo)));
            assertThat(r1.getResponse().getStatus()).isEqualTo(200);
            String nuevo = cuerpo(r1).path("refreshToken").asText();
            assertThat(nuevo).isNotEqualTo(viejo);

            // Un atacante reusa el token viejo
            assertThat(status(json(post("/api/v1/auth/refresh"), Map.of("refreshToken", viejo)))).isEqualTo(401);
            // Como hubo reuso, también se invalidó el token nuevo
            assertThat(status(json(post("/api/v1/auth/refresh"), Map.of("refreshToken", nuevo)))).isEqualTo(401);
        }

        @Test
        @DisplayName("logout borra las sesiones")
        void logout() throws Exception {
            Prueba u = voluntario();
            JsonNode s = login(u.usuario().getEmail(), PASSWORD);
            int out = status(json(post("/api/v1/auth/logout"), Map.of("refreshToken", s.path("refreshToken").asText()))
                    .header("Authorization", "Bearer " + s.path("accessToken").asText()));
            assertThat(out).isEqualTo(204);
            assertThat(status(json(post("/api/v1/auth/refresh"), Map.of("refreshToken", s.path("refreshToken").asText())))).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("RS5 · los permisos se releen de la base en cada pedido")
    class Relectura {

        @Test
        @DisplayName("un usuario desactivado pierde el acceso en el siguiente pedido, aunque su JWT no venció")
        void desactivado() throws Exception {
            Prueba u = voluntario();
            assertThat(status(getCon("/api/v1/usuarios/me", u))).isEqualTo(200);
            mongo.updateFirst(query(where("_id").is(u.usuario().getId())), new Update().set("activo", false), Usuario.class);
            assertThat(status(getCon("/api/v1/usuarios/me", u))).isEqualTo(401);
        }

        @Test
        @DisplayName("si se da de baja la organización, su coordinador pierde el acceso inmediatamente")
        void bajaOrganizacion() throws Exception {
            Organizacion org = crearOrganizacion();
            Prueba c = coordinador(org);
            assertThat(status(getCon("/api/v1/actividades?mias=true", c))).isEqualTo(200);
            mongo.updateFirst(query(where("_id").is(org.getId())), new Update().set("activa", false), Organizacion.class);
            assertThat(status(getCon("/api/v1/actividades?mias=true", c))).isEqualTo(401);
        }

        @Test
        @DisplayName("si le cambian el rol, el cambio aplica sin esperar a que venza el token")
        void cambioDeRol() throws Exception {
            Prueba c = coordinador(crearOrganizacion());
            mongo.updateFirst(query(where("_id").is(c.usuario().getId())), new Update().set("rol", "voluntario").set("organizacionId", null), Usuario.class);
            assertThat(status(con(json(post("/api/v1/actividades"), Map.of()), c))).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("Perfil propio")
    class PerfilPropio {

        @Test
        @DisplayName("GET /usuarios/me devuelve solo los datos del token, sin hash ni push tokens")
        void me() throws Exception {
            Prueba u = voluntario();
            MvcResult r = hacer(getCon("/api/v1/usuarios/me", u));
            assertThat(r.getResponse().getStatus()).isEqualTo(200);
            JsonNode b = cuerpo(r);
            assertThat(b.path("id").asText()).isEqualTo(u.id());
            assertThat(b.has("passwordHash")).isFalse();
            assertThat(b.has("pushTokens")).isFalse();
            assertThat(b.has("versionSesion")).isFalse();
        }

        @Test
        @DisplayName("push token inválido → 400; válido → 204")
        void pushToken() throws Exception {
            Prueba u = voluntario();
            assertThat(status(con(json(post("/api/v1/usuarios/me/push-token"), Map.of("token", "cualquier-cosa")), u))).isEqualTo(400);
            assertThat(status(con(json(post("/api/v1/usuarios/me/push-token"), Map.of("token", "ExponentPushToken[abcdefghijklmnop]")), u))).isEqualTo(204);
        }

        @Test
        @DisplayName("body de más de 100 KB → 413")
        void cuerpoGrande() throws Exception {
            Prueba u = voluntario();
            String enorme = "x".repeat(110 * 1024);
            assertThat(status(con(json(post("/api/v1/usuarios/me/push-token"), Map.of("token", enorme)), u))).isEqualTo(413);
        }
    }

    @Test
    @DisplayName("GET de una ruta pública con un token roto no da 401 (sigue como anónimo)")
    void publicoConTokenRoto() throws Exception {
        assertThat(status(get("/api/v1/health").header("Authorization", "Bearer basura"))).isEqualTo(200);
    }
}
