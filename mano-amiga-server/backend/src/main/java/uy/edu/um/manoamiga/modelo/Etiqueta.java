package uy.edu.um.manoamiga.modelo;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Etiqueta de actividad. sensible = true -> un voluntario judicial no la ve ni se inscribe (ABAC).
 */
@Document("etiquetas")
public class Etiqueta {

    @Id
    private ObjectId id;
    @Indexed(name = "nombre_1", unique = true)
    private String nombre;
    private boolean sensible = false;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public boolean isSensible() { return sensible; }
    public void setSensible(boolean sensible) { this.sensible = sensible; }
}
