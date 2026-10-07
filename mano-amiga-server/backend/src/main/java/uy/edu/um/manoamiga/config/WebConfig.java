package uy.edu.um.manoamiga.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import uy.edu.um.manoamiga.seguridad.ReauthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ReauthInterceptor reauth;

    public WebConfig(ReauthInterceptor reauth) {
        this.reauth = reauth;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(reauth); // RS9
    }
}
