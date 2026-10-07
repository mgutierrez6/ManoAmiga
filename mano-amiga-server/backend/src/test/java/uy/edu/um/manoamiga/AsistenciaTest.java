package uy.edu.um.manoamiga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.web.servlet.MvcResult;

import io.jsonwebtoken.Jwts;

/** Registro de horas con QR (2.3 · 5.8) y horas del voluntario judicial. Los tests van en orden. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AsistenciaTest extends BaseTest {

    static final String QR_SECRET = "test-qr-secret-solo-para-tests-00000000000";

    Escenario s;
    String act;
    String asistencia;

    @BeforeAll
    void preparar() throws Exception {
        s = Escenario.armar(this);
        act = crearActividad(s.coordA()).path("id").asText();
        String ins = cuerpo(hacer(con(post("/api/v1/actividades/" + act + "/inscripciones"), s.vol1()))).path("id").asText();
        hacer(con(post("/api/v1/actividades/" + act + "/inscripciones"), s.vol2())); // queda pendiente
        hacer(con(json(patch("/api/v1/inscripciones/" + ins), Map.of("estado", "aceptada")), s.coordA()));
    }

    private String pedirQR(String actividad) throws Exception {
        return cuerpo(hacer(getCon("/api/v1/actividades/" + actividad + "/qr", s.coordA()))).path("qr").asText();
    }

    private MvcResult escanear(Prueba vol, String qr) throws Exception {
        return hacer(con(json(post("/api/v1/actividades/" + act + "/asistencias"), Map.of("qr", qr)), vol));
    }

    private String qrFirmado(String secreto, Instant emitido) {
        return Jwts.builder().issuer("mano-amiga-api").audience().add("mano-amiga-qr").and()
                .claim("act", act).claim("typ", "qr")
                .issuedAt(Date.from(emitido)).expiration(Date.from(emitido.plusSeconds(60)))
                .signWith(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), Jwts.SIG.HS256)
                .compact();
    }

    @Test
    @Order(1)
    @DisplayName("el coordinador obtiene un QR que no se cachea")
    void qr() throws Exception {
        MvcResult r = hacer(getCon("/api/v1/actividades/" + act + "/qr", s.coordA()));
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(r.getResponse().getHeader("Cache-Control")).contains("no-store");
        assertThat(cuerpo(r).path("qr").asText()).isNotEmpty();
    }

    @Test
    @Order(2)
    @DisplayName("un inscripto NO aceptado no puede marcar asistencia")
    void noAceptado() throws Exception {
        assertThat(escanear(s.vol2(), pedirQR(act)).getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @Order(3)
    @DisplayName("QR falsificado (otro secreto) → 400 QR_INVALIDO; vencido → 400 QR_VENCIDO; de otra actividad → 400")
    void qrInvalidos() throws Exception {
        MvcResult falso = escanear(s.vol1(), qrFirmado("otro-secreto-de-al-menos-32-caracteres-xx", Instant.now()));
        assertThat(falso.getResponse().getStatus()).isEqualTo(400);
        assertThat(codigoError(falso)).isEqualTo("QR_INVALIDO");

        MvcResult vencido = escanear(s.vol1(), qrFirmado(QR_SECRET, Instant.now().minus(120, ChronoUnit.SECONDS)));
        assertThat(vencido.getResponse().getStatus()).isEqualTo(400);
        assertThat(codigoError(vencido)).isEqualTo("QR_VENCIDO");

        String otra = crearActividad(s.coordA()).path("id").asText();
        assertThat(escanear(s.vol1(), pedirQR(otra)).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @Order(4)
    @DisplayName("aceptado + QR válido → 201 check-in; otra vez → 409")
    void checkIn() throws Exception {
        MvcResult r = escanear(s.vol1(), pedirQR(act));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        asistencia = cuerpo(r).path("id").asText();
        assertThat(escanear(s.vol1(), pedirQR(act)).getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    @Order(5)
    @DisplayName("otra org NO valida; la org sí (se calculan las horas); dos veces → 409")
    void validar() throws Exception {
        assertThat(status(con(patch("/api/v1/asistencias/" + asistencia + "/validar"), s.coordB()))).isEqualTo(403);
        MvcResult r = hacer(con(patch("/api/v1/asistencias/" + asistencia + "/validar"), s.coordA()));
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpo(r).path("estado").asText()).isEqualTo("validada");
        assertThat(cuerpo(r).path("horas").asDouble()).isGreaterThanOrEqualTo(0);
        assertThat(status(con(patch("/api/v1/asistencias/" + asistencia + "/validar"), s.coordA()))).isEqualTo(409);
    }

    @Test
    @Order(6)
    @DisplayName("QR fuera del horario de la actividad → 409")
    void fueraDeHorario() throws Exception {
        String lejana = crearActividad(s.coordA(), Map.of(
                "fechaInicio", Instant.now().plus(3, ChronoUnit.DAYS).toString(),
                "fechaFin", Instant.now().plus(3, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS).toString())).path("id").asText();
        assertThat(status(getCon("/api/v1/actividades/" + lejana + "/qr", s.coordA()))).isEqualTo(409);
    }

    @Test
    @Order(7)
    @DisplayName("horas: un voluntario común → 403; el judicial ve asignadas, cumplidas y restantes")
    void horasJudicial() throws Exception {
        assertThat(status(getCon("/api/v1/usuarios/me/horas", s.vol1()))).isEqualTo(403);

        String a = crearActividad(s.coordA()).path("id").asText();
        String ins = cuerpo(hacer(con(post("/api/v1/actividades/" + a + "/inscripciones"), s.judicial()))).path("id").asText();
        hacer(con(json(patch("/api/v1/inscripciones/" + ins), Map.of("estado", "aceptada")), s.coordA()));
        assertThat(status(con(json(post("/api/v1/inscripciones/" + ins + "/asistencia-manual"), Map.of("horas", 20)), s.coordA()))).isEqualTo(400);
        assertThat(status(con(json(post("/api/v1/inscripciones/" + ins + "/asistencia-manual"), Map.of("horas", 2)), s.coordA()))).isEqualTo(201);

        MvcResult r = hacer(getCon("/api/v1/usuarios/me/horas", s.judicial()));
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpo(r).path("horasAsignadas").asInt()).isEqualTo(30);
        assertThat(cuerpo(r).path("horasCumplidas").asDouble()).isEqualTo(2.0);
        assertThat(cuerpo(r).path("horasRestantes").asDouble()).isEqualTo(28.0);
    }
}
