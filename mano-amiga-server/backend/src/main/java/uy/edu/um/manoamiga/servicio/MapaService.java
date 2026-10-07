package uy.edu.um.manoamiga.servicio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import uy.edu.um.manoamiga.dto.Dto;
import uy.edu.um.manoamiga.modelo.Actividad;
import uy.edu.um.manoamiga.modelo.Campana;
import uy.edu.um.manoamiga.modelo.PuntoAcopio;
import uy.edu.um.manoamiga.seguridad.UsuarioActual;
import uy.edu.um.manoamiga.util.Ids;
import uy.edu.um.manoamiga.util.Textos;

/**
 * R5 · Mapa de actividades y puntos de acopio.
 *
 * RS7 (minimización de datos): el mapa solo expone ubicaciones INSTITUCIONALES (dónde se hace una actividad
 * y los puntos de acopio de las campañas). Nunca la ubicación de un voluntario, ni quién se inscribió.
 *
 * ABAC: el endpoint es público, pero si llega con sesión de un voluntario judicial se excluyen las
 * actividades con etiquetas sensibles (igual que en el listado de actividades).
 */
@Service
public class MapaService {

    private static final int MAX_PUNTOS = 300; // tope para no devolver la base entera en una sola consulta

    /** Rectángulo visible del mapa. */
    public record Caja(double minLng, double minLat, double maxLng, double maxLat) {
        boolean contiene(double lng, double lat) {
            return lng >= minLng && lng <= maxLng && lat >= minLat && lat <= maxLat;
        }
    }

    private final MongoTemplate mongo;
    private final PermisosService permisos;
    private final PoblarService poblar;

    public MapaService(MongoTemplate mongo, PermisosService permisos, PoblarService poblar) {
        this.mongo = mongo;
        this.permisos = permisos;
        this.poblar = poblar;
    }

    /** @param tipo todos | actividad | acopio */
    public Dto.Lista<Object> puntos(UsuarioActual usuario, String tipo, String etiqueta, Caja caja) {
        Instant ahora = Instant.now();
        List<Object> resultado = new ArrayList<>();

        if (!"acopio".equals(tipo)) {
            List<Criteria> condiciones = new ArrayList<>();
            condiciones.add(Criteria.where("estado").is("publicada").and("fechaFin").gte(ahora));
            if (caja != null) {
                // Filtro por rectángulo sobre [lng, lat] por rango numérico (zonas chicas, cualquier versión de Mongo).
                condiciones.add(Criteria.where("ubicacion.coordinates.0").gte(caja.minLng()).lte(caja.maxLng())
                        .and("ubicacion.coordinates.1").gte(caja.minLat()).lte(caja.maxLat()));
            }
            if (etiqueta != null) condiciones.add(Criteria.where("etiquetas").is(Ids.oid(etiqueta)));
            if (usuario != null && usuario.esJudicial()) {
                condiciones.add(Criteria.where("etiquetas").nin(permisos.idsEtiquetasSensibles()));
            }
            Query q = new Query(new Criteria().andOperator(condiciones)).with(Sort.by("fechaInicio")).limit(MAX_PUNTOS);
            List<Actividad> actividades = mongo.find(q, Actividad.class);
            Map<ObjectId, String> orgs = poblar.nombresOrganizaciones(actividades.stream().map(Actividad::getOrganizacionId).toList());
            Map<ObjectId, String> tags = poblar.nombresEtiquetas(actividades.stream().flatMap(a -> a.getEtiquetas().stream()).toList());
            for (Actividad a : actividades) {
                resultado.add(new Dto.PuntoActividad("actividad", Ids.str(a.getId()), a.getTitulo(), orgs.getOrDefault(a.getOrganizacionId(), ""),
                        a.getEtiquetas().stream().map(tags::get).filter(n -> n != null).toList(), Textos.vacioSiNull(a.getDireccion()),
                        Dto.LatLng.de(a.getUbicacion()), a.getFechaInicio(), a.getFechaFin(), Math.max(0, a.getCupo() - a.getInscriptos())));
            }
        }

        // Los puntos de acopio no tienen etiquetas: si se filtra por etiqueta, no aplican.
        if (!"actividad".equals(tipo) && etiqueta == null) {
            // Hay pocas campañas activas a la vez: el recorte por rectángulo de cada punto se hace abajo.
            Query q = new Query(Criteria.where("estado").is("activa").and("fechaFin").gte(ahora)).limit(MAX_PUNTOS);
            List<Campana> campanas = mongo.find(q, Campana.class);
            Map<ObjectId, String> orgs = poblar.nombresOrganizaciones(campanas.stream().map(Campana::getOrganizacionId).toList());
            for (Campana c : campanas) {
                for (PuntoAcopio p : c.getPuntos()) {
                    if (caja != null && !caja.contiene(p.getUbicacion().getX(), p.getUbicacion().getY())) continue;
                    resultado.add(new Dto.PuntoAcopio("acopio", Ids.str(p.getId()), Ids.str(c.getId()), p.getNombre(), c.getTitulo(),
                            orgs.getOrDefault(c.getOrganizacionId(), ""), Textos.vacioSiNull(p.getDireccion()), Dto.LatLng.de(p.getUbicacion()), c.getFechaFin()));
                }
            }
        }

        return new Dto.Lista<>(resultado.size() > MAX_PUNTOS ? resultado.subList(0, MAX_PUNTOS) : resultado);
    }
}
