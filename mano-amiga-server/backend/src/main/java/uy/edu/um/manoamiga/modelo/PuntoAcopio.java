package uy.edu.um.manoamiga.modelo;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Punto de acopio dentro de una campaña (subdocumento con su propio _id, como en la versión Node).
 */
public class PuntoAcopio {

    @Field("_id")
    private ObjectId id;
    private String nombre;
    private String direccion;
    private GeoJsonPoint ubicacion;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public GeoJsonPoint getUbicacion() { return ubicacion; }
    public void setUbicacion(GeoJsonPoint ubicacion) { this.ubicacion = ubicacion; }
}
