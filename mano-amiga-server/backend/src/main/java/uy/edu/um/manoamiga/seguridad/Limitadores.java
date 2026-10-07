package uy.edu.um.manoamiga.seguridad;

import java.time.Duration;

import org.springframework.stereotype.Component;

/**
 * Los límites de intentos de la API (el límite general de 100 pedidos por minuto está en el front-service).
 * - login: 5 intentos fallidos cada 15 minutos por IP + email (5.1).
 * - reauth: 5 intentos fallidos cada 15 minutos por administrador (RS9).
 */
@Component
public class Limitadores {

    private final LimitadorIntentos login = new LimitadorIntentos(5, Duration.ofMinutes(15));
    private final LimitadorIntentos reauth = new LimitadorIntentos(5, Duration.ofMinutes(15));

    public LimitadorIntentos login() { return login; }
    public LimitadorIntentos reauth() { return reauth; }
}
