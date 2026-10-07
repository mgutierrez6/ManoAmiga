package uy.edu.um.manoamiga.servicio;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import uy.edu.um.manoamiga.modelo.Actividad;
import uy.edu.um.manoamiga.modelo.Etiqueta;
import uy.edu.um.manoamiga.modelo.Organizacion;
import uy.edu.um.manoamiga.modelo.Usuario;

/**
 * Lo que en la versión Node hacía populate(): trae en UNA consulta los documentos relacionados
 * (organizaciones, etiquetas, usuarios...) de una lista, para armar los DTOs.
 */
@Service
public class PoblarService {

    private final MongoTemplate mongo;

    public PoblarService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public <T> Map<ObjectId, T> porId(Class<T> clase, Collection<ObjectId> ids, Function<T, ObjectId> getId) {
        var unicos = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (unicos.isEmpty()) return new HashMap<>();
        return mongo.find(new Query(Criteria.where("_id").in(unicos)), clase).stream()
                .collect(Collectors.toMap(getId, Function.identity(), (a, b) -> a));
    }

    public Map<ObjectId, String> nombresOrganizaciones(Collection<ObjectId> ids) {
        return porId(Organizacion.class, ids, Organizacion::getId).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getNombre()));
    }

    public Map<ObjectId, String> nombresEtiquetas(Collection<ObjectId> ids) {
        return porId(Etiqueta.class, ids, Etiqueta::getId).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getNombre()));
    }

    public Map<ObjectId, Usuario> usuarios(Collection<ObjectId> ids) {
        return porId(Usuario.class, ids, Usuario::getId);
    }

    public Map<ObjectId, Actividad> actividades(Collection<ObjectId> ids) {
        return porId(Actividad.class, ids, Actividad::getId);
    }

    public Map<ObjectId, Organizacion> organizaciones(Collection<ObjectId> ids) {
        return porId(Organizacion.class, ids, Organizacion::getId);
    }
}
