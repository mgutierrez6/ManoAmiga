package uy.edu.um.manoamiga.seguridad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RS9 · Marca un endpoint del panel de administración que, además del rol, exige una reautenticación
 * reciente: header X-Reauth-Token (5 minutos), obtenido con POST /auth/reauth. Lo controla ReauthInterceptor.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiereReauth {}
