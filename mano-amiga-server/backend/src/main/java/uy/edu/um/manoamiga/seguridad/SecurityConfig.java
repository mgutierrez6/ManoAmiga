package uy.edu.um.manoamiga.seguridad;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import uy.edu.um.manoamiga.config.Configuracion;

/**
 * Control de acceso de TODA la API, armado a partir de la matriz (Matriz.java):
 *
 *  - RS8 · Denegar por defecto: cualquier ruta que no figure en la matriz como pública exige autenticación.
 *    Si alguien agrega un endpoint y se olvida de configurarlo, igual queda protegido (y el test falla).
 *  - RS1 / RS4 · Permiso explícito por rol, validado en el servidor, antes de llegar al controller.
 *  - Sin sesiones en el servidor (STATELESS) ni cookies: el token viaja en el header Authorization,
 *    por eso CSRF no aplica y se desactiva.
 *  - Cabeceras de seguridad (equivalente a helmet): nosniff, X-Frame-Options DENY, CSP restrictiva,
 *    Cache-Control no-store, HSTS si hay HTTPS.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain cadena(HttpSecurity http, AutenticacionService autenticacion, ManejadoresSeguridad manejadores) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .requestCache(cache -> cache.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                .frameOptions(f -> f.deny()))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(manejadores)
                .accessDeniedHandler(manejadores))
            .addFilterBefore(new JwtFiltro(autenticacion), AnonymousAuthenticationFilter.class)
            .authorizeHttpRequests(reglas -> {
                for (Matriz.Entrada e : Matriz.ENTRADAS) {
                    HttpMethod metodo = HttpMethod.valueOf(e.metodo());
                    if (e.acceso() == Matriz.Acceso.PUBLICO) {
                        reglas.requestMatchers(metodo, e.patron()).permitAll();
                    } else {
                        String[] roles = e.roles().stream().map(r -> r.toUpperCase(Locale.ROOT)).toArray(String[]::new);
                        reglas.requestMatchers(metodo, e.patron()).hasAnyRole(roles);
                    }
                }
                reglas.anyRequest().authenticated(); // RS8: todo lo demás, con sesión (después da 404)
            });
        return http.build();
    }

    /** bcrypt con costo 12 (5.1). Verifica también los hashes que generó la versión Node ($2a$ / $2b$). */
    @Bean
    public PasswordEncoder passwordEncoder(Configuracion config) {
        return new BCryptPasswordEncoder(config.bcryptCosto());
    }
}
