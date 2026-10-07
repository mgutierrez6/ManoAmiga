# Imagen de la API Java. Dos etapas: compilar con Maven y correr solo con el JRE (imagen chica, sin herramientas de build).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/mano-amiga-api.jar app.jar
# No corremos como root dentro del contenedor.
RUN addgroup -S app && adduser -S app -G app
USER app
EXPOSE 3000
# -XX:MaxRAMPercentage: en la instancia gratuita de AWS (1 GB) la JVM no se come toda la memoria.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-jar", "app.jar"]
