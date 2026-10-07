package uy.edu.um.manoamiga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import uy.edu.um.manoamiga.seguridad.Matriz;

/** RS1, RS4, RS8, RS10 · La matriz endpoint × rol se verifica automáticamente. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatrizTest extends BaseTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mapeos;

    Map<String, Prueba> porRol;

    @BeforeAll
    void preparar() {
        porRol = Map.of("voluntario", voluntario(), "coordinador", coordinador(crearOrganizacion()), "admin", admin());
    }

    private static String url(Matriz.Entrada e) {
        return e.ruta().replaceAll(":[a-zA-Z]+", ID_FALSO);
    }

    static Stream<Arguments> protegidas() {
        return Matriz.ENTRADAS.stream().filter(e -> !e.publico()).map(e -> Arguments.of(e.metodo() + " " + e.ruta(), e));
    }

    static Stream<Arguments> rolesNoPermitidos() {
        return Matriz.ENTRADAS.stream()
                .filter(e -> !e.publico())
                .flatMap(e -> Matriz.TODOS.stream().filter(r -> !e.roles().contains(r))
                        .map(r -> Arguments.of(r + " → " + e.metodo() + " " + e.ruta(), r, e)));
    }

    /** Todas las rutas que Spring registró, en formato "GET /api/v1/actividades/{id}". */
    private List<String> registradas() {
        List<String> rutas = new ArrayList<>();
        for (RequestMappingInfo info : mapeos.getHandlerMethods().keySet()) {
            if (info.getPathPatternsCondition() == null) {
                continue;
            }
            for (String patron : info.getPathPatternsCondition().getPatternValues()) {
                if (!patron.startsWith("/api/v1")) {
                    continue; // /error de Spring Boot
                }
                for (RequestMethod m : info.getMethodsCondition().getMethods()) {
                    rutas.add(m.name() + " " + patron);
                }
            }
        }
        return rutas;
    }

    private List<String> documentadas() {
        return Matriz.ENTRADAS.stream().map(e -> e.metodo() + " " + e.patronSpring()).toList();
    }

    @Test
    @DisplayName("RS10 · toda ruta registrada figura en la matriz (nadie agrega un endpoint \"a escondidas\")")
    void todaRutaRegistradaEstaEnLaMatriz() {
        List<String> sinDocumentar = registradas().stream().filter(r -> !documentadas().contains(r)).toList();
        assertThat(sinDocumentar).isEmpty();
    }

    @Test
    @DisplayName("RS10 · todo endpoint de la matriz está implementado")
    void todoEndpointDeLaMatrizExiste() {
        List<String> faltan = documentadas().stream().filter(d -> !registradas().contains(d)).toList();
        assertThat(faltan).isEmpty();
    }

    @ParameterizedTest(name = "{0} sin token → 401")
    @MethodSource("protegidas")
    @DisplayName("RS8 · denegar por defecto")
    void sinToken401(String nombre, Matriz.Entrada e) throws Exception {
        assertThat(status(json(request(HttpMethod.valueOf(e.metodo()), url(e)), Map.of()))).isEqualTo(401);
    }

    @ParameterizedTest(name = "{0} = 403")
    @MethodSource("rolesNoPermitidos")
    @DisplayName("RS1 / RS4 · cada rol que no figura en la matriz recibe 403")
    void rolNoPermitido403(String nombre, String rol, Matriz.Entrada e) throws Exception {
        MvcResult r = hacer(con(json(request(HttpMethod.valueOf(e.metodo()), url(e)), Map.of()), porRol.get(rol)));
        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(codigoError(r)).isEqualTo("ROL_NO_PERMITIDO");
    }

    @Test
    @DisplayName("RS8 · los endpoints de la lista blanca responden sin token")
    void listaBlanca() throws Exception {
        for (String ruta : List.of("/api/v1/health", "/api/v1/etiquetas", "/api/v1/campanas", "/api/v1/mapa/puntos")) {
            assertThat(status(get(ruta))).as(ruta).isEqualTo(200);
        }
        // login sin datos: 400 (validación), no 401 → llegó al endpoint sin pedir token
        assertThat(status(json(post("/api/v1/auth/login"), Map.of()))).isEqualTo(400);
    }

    @Test
    @DisplayName("token con firma inválida → 401")
    void firmaInvalida() throws Exception {
        Prueba v = porRol.get("voluntario");
        assertThat(status(get("/api/v1/usuarios/me").header("Authorization", v.bearer() + "x"))).isEqualTo(401);
    }

    @Test
    @DisplayName("token con \"alg: none\" → 401")
    void algNone() throws Exception {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String header = b64.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = b64.encodeToString(("{\"sub\":\"" + porRol.get("voluntario").id() + "\",\"typ\":\"access\"}").getBytes(StandardCharsets.UTF_8));
        assertThat(status(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + header + "." + payload + "."))).isEqualTo(401);
    }

    @Test
    @DisplayName("una ruta inexistente también exige token (no revela qué existe); con token da 404")
    void rutaInexistente() throws Exception {
        assertThat(status(get("/api/v1/no-existe"))).isEqualTo(401);
        MvcResult r = hacer(con(get("/api/v1/no-existe"), porRol.get("voluntario")));
        assertThat(r.getResponse().getStatus()).isEqualTo(404);
        assertThat(codigoError(r)).isEqualTo("RUTA_INEXISTENTE");
    }

    @Test
    @DisplayName("cabeceras de seguridad (5.7)")
    void cabeceras() throws Exception {
        MvcResult r = hacer(get("/api/v1/health"));
        assertThat(r.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(r.getResponse().getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(r.getResponse().getHeader("Content-Security-Policy")).contains("default-src 'none'");
    }
}
