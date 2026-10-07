package uy.edu.um.manoamiga.servicio;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import uy.edu.um.manoamiga.config.Configuracion;
import uy.edu.um.manoamiga.modelo.Usuario;

/**
 * Notificaciones push vía el servicio de Expo (-> FCM / APNs), con su API HTTP.
 * Límite de confianza 3 (5.11): el texto NUNCA incluye datos personales (ni teléfono, ni condición judicial).
 * Es "best effort" y asíncrono: si falla, se loguea y la operación principal sigue.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final URI EXPO_PUSH = URI.create("https://exp.host/--/api/v2/push/send");
    private static final int LOTE = 100; // Expo acepta hasta 100 mensajes por pedido

    private final MongoTemplate mongo;
    private final Configuracion config;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public NotificacionService(MongoTemplate mongo, Configuracion config, ObjectMapper json) {
        this.mongo = mongo;
        this.config = config;
        this.json = json;
    }

    public void notificar(Collection<ObjectId> usuarioIds, String titulo, String cuerpo, Map<String, String> data) {
        if (!config.pushHabilitado() || usuarioIds.isEmpty()) return;
        Thread.startVirtualThread(() -> enviar(usuarioIds, titulo, cuerpo, data));
    }

    private void enviar(Collection<ObjectId> usuarioIds, String titulo, String cuerpo, Map<String, String> data) {
        try {
            List<Map<String, Object>> mensajes = new ArrayList<>();
            for (Usuario u : mongo.find(new Query(Criteria.where("_id").in(usuarioIds)), Usuario.class)) {
                for (String token : u.getPushTokens()) {
                    if (!UsuarioService.PUSH_TOKEN.matcher(token).matches()) continue;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("to", token);
                    m.put("sound", "default");
                    m.put("title", titulo);
                    m.put("body", cuerpo);
                    m.put("data", data);
                    mensajes.add(m);
                }
            }
            for (int i = 0; i < mensajes.size(); i += LOTE) {
                HttpRequest.Builder req = HttpRequest.newBuilder(EXPO_PUSH)
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(mensajes.subList(i, Math.min(i + LOTE, mensajes.size())))));
                if (!config.expoAccessToken().isBlank()) req.header("Authorization", "Bearer " + config.expoAccessToken());
                HttpResponse<Void> res = http.send(req.build(), HttpResponse.BodyHandlers.discarding());
                if (res.statusCode() >= 300) log.warn("Expo Push respondió {}", res.statusCode());
            }
        } catch (Exception e) {
            log.warn("No se pudo enviar una notificación push: {}", e.getMessage());
        }
    }
}
