package uy.edu.um.manoamiga.config;

import java.util.List;
import java.util.Locale;

import org.bson.types.ObjectId;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import uy.edu.um.manoamiga.modelo.Etiqueta;
import uy.edu.um.manoamiga.modelo.Organizacion;
import uy.edu.um.manoamiga.modelo.Usuario;

/**
 * Carga datos iniciales para desarrollo (lo mismo que "npm run seed" en la versión Node):
 *  - catálogo de etiquetas (con cuáles son sensibles para la regla ABAC),
 *  - un administrador, una organización de prueba y un coordinador,
 *  - (opcional) un voluntario judicial para probar ABAC.
 *
 * Se corre con el perfil "seed" (ver README). Las contraseñas NO están en el código: se leen del .env (SEED_*).
 * Es idempotente: se puede correr varias veces.
 */
@Component
@Profile("seed")
public class Seed implements CommandLineRunner {

    private record Tag(String nombre, boolean sensible) {}

    private static final List<Tag> ETIQUETAS = List.of(
            new Tag("Ollas populares", false), new Tag("Medioambiente", false), new Tag("Urgente", false),
            new Tag("Donaciones", false), new Tag("Adultos mayores", false), new Tag("Apto programa judicial", false),
            new Tag("Apoyo escolar", true), new Tag("Contacto con menores", true), new Tag("Hogares de niños", true),
            new Tag("Oratorios", true));

    private final MongoTemplate mongo;
    private final PasswordEncoder passwords;
    private final Environment env;
    private final ConfigurableApplicationContext contexto;

    public Seed(MongoTemplate mongo, PasswordEncoder passwords, Environment env, ConfigurableApplicationContext contexto) {
        this.mongo = mongo;
        this.passwords = passwords;
        this.env = env;
        this.contexto = contexto;
    }

    private String variable(String nombre, boolean obligatoria) {
        String v = env.getProperty(nombre, "");
        if (v.isBlank() && obligatoria) throw new IllegalStateException("Falta " + nombre + " en el .env para correr el seed");
        return v;
    }

    private void upsertUsuario(String email, String password, String nombre, String apellido, String rol, ObjectId org, boolean judicial, int horas) {
        if (password.length() < 8) throw new IllegalStateException("La contraseña de " + email + " debe tener al menos 8 caracteres");
        mongo.upsert(new Query(Criteria.where("email").is(email.toLowerCase(Locale.ROOT))),
                new Update().set("nombre", nombre).set("apellido", apellido).set("passwordHash", passwords.encode(password)).set("rol", rol)
                        .set("organizacionId", org).set("esJudicial", judicial).set("horasAsignadas", horas).set("activo", true)
                        .setOnInsert("versionSesion", 0),
                Usuario.class);
        System.out.println("  ✔ " + rol + (judicial ? " (judicial)" : "") + ": " + email);
    }

    @Override
    public void run(String... args) {
        System.out.println("Etiquetas:");
        for (Tag t : ETIQUETAS) {
            mongo.upsert(new Query(Criteria.where("nombre").is(t.nombre())), new Update().set("sensible", t.sensible()), Etiqueta.class);
            System.out.println("  ✔ " + t.nombre() + (t.sensible() ? "  (sensible)" : ""));
        }

        System.out.println("Organización:");
        Organizacion org = mongo.findAndModify(new Query(Criteria.where("nombre").is("Organización Demo")),
                new Update().set("contacto", "contacto@demo.org").set("convenioOSLA", true).set("activa", true),
                FindAndModifyOptions.options().upsert(true).returnNew(true), Organizacion.class);
        System.out.println("  ✔ " + org.getNombre());

        System.out.println("Usuarios:");
        upsertUsuario(variable("SEED_ADMIN_EMAIL", true), variable("SEED_ADMIN_PASSWORD", true), "Admin", "Mano Amiga", Usuario.ADMIN, null, false, 0);
        upsertUsuario(variable("SEED_COORD_EMAIL", true), variable("SEED_COORD_PASSWORD", true), "Coordinadora", "Demo", Usuario.COORDINADOR, org.getId(), false, 0);
        if (!variable("SEED_JUDICIAL_EMAIL", false).isBlank()) {
            upsertUsuario(variable("SEED_JUDICIAL_EMAIL", true), variable("SEED_JUDICIAL_PASSWORD", true), "Voluntario", "Judicial", Usuario.VOLUNTARIO, null, true, 60);
        }
        System.out.println("Listo.");
        contexto.close(); // el seed termina acá; no deja la API levantada
    }
}
