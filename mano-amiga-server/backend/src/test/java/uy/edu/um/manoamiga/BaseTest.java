package uy.edu.um.manoamiga;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uy.edu.um.manoamiga.modelo.Etiqueta;
import uy.edu.um.manoamiga.modelo.Organizacion;
import uy.edu.um.manoamiga.modelo.Usuario;
import uy.edu.um.manoamiga.seguridad.TokenService;

/**
 * Base de los tests de integración: levantan la API entera (Spring Security incluido) y le pegan
 * con MockMvc, igual que supertest en la versión Node.
 *
 * Base de datos:
 *  - Si definen la variable MONGO_TEST_URI, usan esa (por ejemplo, un MongoDB local o una base
 *    "mano_amiga_test" en Atlas). NUNCA la base de producción.
 *  - Si no, levantan un MongoDB real dentro de Docker (Testcontainers). Necesita Docker Desktop prendido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseTest {

    private static MongoDBContainer contenedor;

    @DynamicPropertySource
    static void mongo(DynamicPropertyRegistry r) {
        String externa = System.getenv("MONGO_TEST_URI");
        if (externa != null && !externa.isBlank()) {
            r.add("spring.data.mongodb.uri", () -> externa);
        } else {
            synchronized (BaseTest.class) {
                if (contenedor == null) {
                    contenedor = new MongoDBContainer(DockerImageName.parse("mongo:7"));
                    contenedor.start();
                }
            }
            r.add("spring.data.mongodb.uri", contenedor::getReplicaSetUrl);
        }
        r.add("spring.data.mongodb.database", () -> "mano_amiga_test");
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected MongoTemplate mongo;
    @Autowired
    protected ObjectMapper om;
    @Autowired
    protected TokenService tokens;
    @Autowired
    protected PasswordEncoder encoder;

    public static final String PASSWORD = "Password123";
    public static final String ID_FALSO = "64b7f0c2a1b2c3d4e5f60718";

    /** Usuario de prueba creado directo en la base, con su access token. */
    public record Prueba(Usuario usuario, String token) {
        public String id() {
            return usuario.getId().toHexString();
        }

        public String bearer() {
            return "Bearer " + token;
        }
    }

    protected static String unico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected Organizacion crearOrganizacion() {
        Organizacion o = new Organizacion();
        o.setNombre("Org " + unico());
        o.setActiva(true);
        return mongo.insert(o);
    }

    protected Prueba crearUsuario(String rol, Organizacion org, boolean judicial) {
        Usuario u = new Usuario();
        u.setNombre("Nombre");
        u.setApellido("Apellido");
        u.setEmail(rol + "-" + unico() + "@test.com");
        u.setTelefono("099123456");
        u.setPasswordHash(encoder.encode(PASSWORD));
        u.setRol(rol);
        u.setOrganizacionId(org == null ? null : org.getId());
        u.setEsJudicial(judicial);
        u.setHorasAsignadas(judicial ? 30 : 0);
        u = mongo.insert(u);
        return new Prueba(u, tokens.firmarAccessToken(u.getId().toHexString(), 0));
    }

    protected Prueba voluntario() {
        return crearUsuario("voluntario", null, false);
    }

    protected Prueba judicial() {
        return crearUsuario("voluntario", null, true);
    }

    protected Prueba admin() {
        return crearUsuario("admin", null, false);
    }

    protected Prueba coordinador(Organizacion org) {
        return crearUsuario("coordinador", org, false);
    }

    protected Etiqueta crearEtiqueta(boolean sensible) {
        Etiqueta e = new Etiqueta();
        e.setNombre((sensible ? "Sensible " : "Normal ") + unico());
        e.setSensible(sensible);
        return mongo.insert(e);
    }

    /** Datos de una actividad que empezó hace 30 minutos (sirve para probar el QR). */
    protected Map<String, Object> datosActividad(Map<String, Object> extra) {
        Instant inicio = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant fin = Instant.now().plus(3, ChronoUnit.HOURS);
        Map<String, Object> d = new java.util.HashMap<>(Map.of(
                "titulo", "Limpieza de playa Ramírez",
                "descripcion", "Juntamos residuos en la playa, traer guantes.",
                "etiquetas", List.of(),
                "ubicacion", Map.of("lat", -34.91, "lng", -56.17),
                "direccion", "Playa Ramírez, Montevideo",
                "fechaInicio", inicio.toString(),
                "fechaFin", fin.toString(),
                "cupo", 20));
        d.putAll(extra);
        return d;
    }

    protected JsonNode crearActividad(Prueba coord, Map<String, Object> extra) throws Exception {
        MvcResult r = mvc.perform(json(post("/api/v1/actividades"), datosActividad(extra)).header("Authorization", coord.bearer())).andReturn();
        if (r.getResponse().getStatus() != 201) {
            throw new IllegalStateException("No se pudo crear la actividad: " + r.getResponse().getStatus() + " " + r.getResponse().getContentAsString());
        }
        return om.readTree(r.getResponse().getContentAsString());
    }

    protected JsonNode crearActividad(Prueba coord) throws Exception {
        return crearActividad(coord, Map.of());
    }

    // ----- Atajos para pedidos -----

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, Object cuerpo) throws Exception {
        return b.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(cuerpo));
    }

    protected MockHttpServletRequestBuilder con(MockHttpServletRequestBuilder b, Prueba u) {
        return b.header("Authorization", u.bearer());
    }

    protected JsonNode cuerpo(MvcResult r) throws Exception {
        String s = r.getResponse().getContentAsString();
        return s.isEmpty() ? om.createObjectNode() : om.readTree(s);
    }

    protected int status(MockHttpServletRequestBuilder b) throws Exception {
        return mvc.perform(b).andReturn().getResponse().getStatus();
    }

    protected MvcResult hacer(MockHttpServletRequestBuilder b) throws Exception {
        return mvc.perform(b).andReturn();
    }

    protected String codigoError(MvcResult r) throws Exception {
        return cuerpo(r).path("error").path("code").asText();
    }

    /** Login real por la API (devuelve accessToken y refreshToken). */
    protected JsonNode login(String email, String password) throws Exception {
        MvcResult r = hacer(json(post("/api/v1/auth/login"), Map.of("email", email, "password", password)));
        if (r.getResponse().getStatus() != 200) {
            throw new IllegalStateException("Login falló: " + r.getResponse().getContentAsString());
        }
        return cuerpo(r);
    }

    protected static String hexAleatorio(int bytes) {
        byte[] b = new byte[bytes];
        new java.util.Random().nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    protected MockHttpServletRequestBuilder getCon(String url, Prueba u) {
        return con(get(url), u);
    }

    protected ObjectId oid(JsonNode n) {
        return new ObjectId(n.path("id").asText());
    }
}
