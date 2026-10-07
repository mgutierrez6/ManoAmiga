package uy.edu.um.manoamiga.seguridad;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RS10 · Matriz endpoint × rol.
 *
 * Es la MISMA tabla de la sección 3.3 del informe, pero en código, y es la ÚNICA fuente de verdad del
 * control de acceso por rol:
 *  - SecurityConfig arma la cadena de Spring Security a partir de esta lista (RS1, RS4):
 *    los "PUBLICO" quedan con permitAll(), los "JWT" con hasAnyRole(roles), y CUALQUIER otra ruta
 *    exige autenticación (RS8: denegar por defecto).
 *  - Los tests (MatrizTest) verifican que toda ruta de los controllers figura acá, que todo endpoint no
 *    público responde 401 sin token y que todo rol que no figura recibe 403.
 *
 * control: control adicional que hace el service (titularidad RS2, organización RS3, ABAC, reauth RS9).
 */
public final class Matriz {

    public enum Acceso { PUBLICO, JWT }

    public static final List<String> TODOS = List.of("voluntario", "coordinador", "admin");
    private static final List<String> NINGUNO = List.of();

    public record Entrada(String metodo, String ruta, Acceso acceso, List<String> roles, String control, int sprint) {

        public boolean publico() {
            return acceso == Acceso.PUBLICO;
        }

        /** "/api/v1/actividades/:id" -> como lo registra Spring MVC: "/api/v1/actividades/{id}". */
        public String patronSpring() {
            return ruta.replaceAll(":([a-zA-Z]+)", "{$1}");
        }

        /** "/api/v1/actividades/:id" -> patrón de Spring Security "/api/v1/actividades/*". */
        public String patron() {
            return ruta.replaceAll(":[a-zA-Z]+", "*");
        }

        /** ¿Este pedido corresponde a esta entrada? Cada ":param" de la ruta es un segmento cualquiera. */
        public boolean coincide(String metodoPedido, String path) {
            if (!metodo.equals(metodoPedido)) return false;
            String regex = Arrays.stream(ruta.split("/"))
                    .map(seg -> seg.startsWith(":") ? "[^/]+" : Pattern.quote(seg))
                    .collect(Collectors.joining("/"));
            return path.matches(regex + "/?");
        }
    }

    public static final List<Entrada> ENTRADAS = List.of(
        new Entrada("POST", "/api/v1/auth/register", Acceso.PUBLICO, NINGUNO, "Rol siempre voluntario", 1),
        new Entrada("POST", "/api/v1/auth/login", Acceso.PUBLICO, NINGUNO, "Rate limit 5/15 min", 1),
        new Entrada("POST", "/api/v1/auth/refresh", Acceso.PUBLICO, NINGUNO, "Refresh token válido y no reusado", 1),
        new Entrada("POST", "/api/v1/auth/logout", Acceso.JWT, TODOS, "Borra sus sesiones", 1),
        new Entrada("POST", "/api/v1/auth/reauth", Acceso.JWT, List.of("admin"), "RS9", 2),
        new Entrada("GET", "/api/v1/usuarios/me", Acceso.JWT, TODOS, "id del token", 1),
        new Entrada("PATCH", "/api/v1/usuarios/me", Acceso.JWT, TODOS, "id del token; solo nombre, apellido, teléfono y tema", 2),
        new Entrada("PATCH", "/api/v1/usuarios/me/password", Acceso.JWT, TODOS, "id del token + contraseña actual; revoca sesiones (RS5) y devuelve sesión nueva (200)", 2),
        new Entrada("GET", "/api/v1/usuarios/me/accesos", Acceso.JWT, TODOS, "id del token (solo sus accesos)", 2),
        new Entrada("POST", "/api/v1/usuarios/me/push-token", Acceso.JWT, TODOS, "id del token", 1),
        new Entrada("GET", "/api/v1/admin/organizaciones", Acceso.JWT, List.of("admin"), "", 2),
        new Entrada("POST", "/api/v1/admin/organizaciones", Acceso.JWT, List.of("admin"), "reauth (RS9)", 2),
        new Entrada("DELETE", "/api/v1/admin/organizaciones/:id", Acceso.JWT, List.of("admin"), "reauth (RS9) + RS5", 2),
        new Entrada("GET", "/api/v1/admin/usuarios", Acceso.JWT, List.of("admin"), "", 2),
        new Entrada("POST", "/api/v1/admin/coordinadores", Acceso.JWT, List.of("admin"), "reauth (RS9)", 2),
        new Entrada("DELETE", "/api/v1/admin/coordinadores/:id", Acceso.JWT, List.of("admin"), "reauth (RS9) + RS5", 2),
        new Entrada("PATCH", "/api/v1/admin/voluntarios/:id/judicial", Acceso.JWT, List.of("admin"), "reauth (RS9)", 2),
        new Entrada("GET", "/api/v1/admin/judiciales/horas", Acceso.JWT, List.of("admin"), "", 2),
        new Entrada("GET", "/api/v1/admin/auditoria", Acceso.JWT, List.of("admin"), "RS9: detectar uso indebido", 2),
        new Entrada("GET", "/api/v1/etiquetas", Acceso.PUBLICO, NINGUNO, "", 1),
        new Entrada("GET", "/api/v1/actividades", Acceso.JWT, TODOS, "ABAC en la consulta", 1),
        new Entrada("GET", "/api/v1/actividades/:id", Acceso.JWT, TODOS, "ABAC (403)", 1),
        new Entrada("POST", "/api/v1/actividades", Acceso.JWT, List.of("coordinador"), "org del token", 1),
        new Entrada("PATCH", "/api/v1/actividades/:id", Acceso.JWT, List.of("coordinador"), "Coordinador de la org (RS3)", 1),
        new Entrada("DELETE", "/api/v1/actividades/:id", Acceso.JWT, List.of("coordinador"), "Coordinador de la org (RS3)", 1),
        new Entrada("POST", "/api/v1/actividades/:id/inscripciones", Acceso.JWT, List.of("voluntario"), "ABAC + cupo", 1),
        new Entrada("GET", "/api/v1/usuarios/me/inscripciones", Acceso.JWT, List.of("voluntario"), "Titular (id del token)", 1),
        new Entrada("DELETE", "/api/v1/inscripciones/:id", Acceso.JWT, List.of("voluntario"), "Titular (RS2)", 1),
        new Entrada("GET", "/api/v1/actividades/:id/inscripciones", Acceso.JWT, List.of("coordinador"), "Coordinador de la org (RS3)", 1),
        new Entrada("PATCH", "/api/v1/inscripciones/:id", Acceso.JWT, List.of("coordinador"), "Coordinador de la org (RS3)", 1),
        new Entrada("GET", "/api/v1/inscripciones/:id/certificado", Acceso.JWT, List.of("voluntario", "coordinador"), "Titular (RS2) o coordinador de la org (RS3)", 1),
        new Entrada("GET", "/api/v1/actividades/:id/qr", Acceso.JWT, List.of("coordinador"), "Coordinador de la org (RS3)", 1),
        new Entrada("POST", "/api/v1/actividades/:id/asistencias", Acceso.JWT, List.of("voluntario"), "Titular con inscripción aceptada", 1),
        new Entrada("GET", "/api/v1/actividades/:id/asistencias", Acceso.JWT, List.of("coordinador"), "Coordinador de la org (RS3)", 1),
        new Entrada("POST", "/api/v1/inscripciones/:id/asistencia-manual", Acceso.JWT, List.of("coordinador"), "Coordinador de la org (RS3)", 1),
        new Entrada("PATCH", "/api/v1/asistencias/:id/validar", Acceso.JWT, List.of("coordinador"), "Coordinador de la org (RS3)", 1),
        new Entrada("GET", "/api/v1/usuarios/me/horas", Acceso.JWT, List.of("voluntario"), "Solo si esJudicial", 1),
        new Entrada("GET", "/api/v1/mapa/puntos", Acceso.PUBLICO, NINGUNO, "Solo ubicaciones institucionales (RS7); ABAC si hay sesión", 2),
        new Entrada("GET", "/api/v1/campanas", Acceso.PUBLICO, NINGUNO, "Solo datos institucionales", 1),
        new Entrada("POST", "/api/v1/campanas", Acceso.JWT, List.of("coordinador"), "org del token", 1),
        new Entrada("GET", "/api/v1/proyectos/:id/publicaciones", Acceso.PUBLICO, NINGUNO, "", 1),
        new Entrada("POST", "/api/v1/proyectos/:id/publicaciones", Acceso.JWT, List.of("coordinador"), "Coordinador de la org (RS3)", 1),
        new Entrada("GET", "/api/v1/health", Acceso.PUBLICO, NINGUNO, "Sin datos", 1)
    );

    private Matriz() {}

    public static List<Entrada> publicas() {
        return ENTRADAS.stream().filter(e -> e.acceso() == Acceso.PUBLICO).toList();
    }

    /** La entrada de la matriz que corresponde a un pedido (o null si la ruta no existe). */
    public static Entrada buscar(String metodo, String path) {
        return ENTRADAS.stream().filter(e -> e.coincide(metodo, path)).findFirst().orElse(null);
    }
}
