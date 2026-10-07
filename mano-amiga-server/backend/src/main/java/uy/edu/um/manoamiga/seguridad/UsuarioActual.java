package uy.edu.um.manoamiga.seguridad;

/**
 * El usuario autenticado del pedido, armado con los datos LEÍDOS DE LA BASE en este pedido (no del token).
 * Es lo que reciben los controllers con @AuthenticationPrincipal y lo que usan los services.
 */
public record UsuarioActual(String id, String rol, String organizacionId, boolean esJudicial, String nombre) {

    public boolean es(String r) {
        return rol.equals(r);
    }
}
