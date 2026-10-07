package uy.edu.um.manoamiga.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

/**
 * - @EnableMongoAuditing: completa createdAt / updatedAt (como "timestamps" de Mongoose).
 * - Sin el campo "_class" que Spring agrega por defecto: así los documentos quedan iguales a los que
 *   guarda la versión Node y las dos pueden usar la misma base.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig implements InitializingBean {

    private final MappingMongoConverter converter;

    public MongoConfig(MappingMongoConverter converter) {
        this.converter = converter;
    }

    @Override
    public void afterPropertiesSet() {
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
    }
}
