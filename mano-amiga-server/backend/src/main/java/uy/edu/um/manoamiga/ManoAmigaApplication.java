package uy.edu.um.manoamiga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ManoAmigaApplication {
    public static void main(String[] args) {
        SpringApplication.run(ManoAmigaApplication.class, args);
    }
}
