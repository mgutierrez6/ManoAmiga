# Mano Amiga · API en Java (Spring Boot)

Es **la misma API** que `../backend` (Node), pero en Java, para la materia de Seguridad:

- mismas rutas (`/api/v1/...`), mismo JSON, mismos códigos de error (`{ "error": { "code", "message" } }`);
- mismos tokens (JWT HS256 con los mismos secretos) y misma base de datos (mismas colecciones e índices).

Por eso **la app no cambia nada**: el front-service reenvía a `127.0.0.1:3000` y no le importa si ahí hay Node o Java.
Se levanta **una o la otra** (las dos usan el puerto 3000).

| Para… | Backend |
|---|---|
| Apps Móviles (demo, informe) | `backend/` (Node) |
| Seguridad (código y tests que se evalúan) | `backend-java/` (este) |

---

## 1. Instalar (una vez)

1. **IntelliJ IDEA Community** (gratis): https://www.jetbrains.com/idea/download → la versión *Community*.
   Trae Maven incluido, no hace falta instalarlo aparte.
2. **JDK 21**: lo descarga IntelliJ en el paso 2.3 (no hace falta instalarlo a mano).
3. **Docker Desktop** (solo para correr los tests): https://www.docker.com/products/docker-desktop
   Los tests levantan un MongoDB descartable dentro de Docker. Si no pueden instalarlo, ver la sección 4.2.

## 2. Abrir y levantar la API

1. En IntelliJ: **File → Open…** → elegí la carpeta `mano-amiga-server/backend-java` → **Trust Project**.
2. Abajo a la derecha aparece "Maven build scripts found" → **Load**. La primera vez baja las dependencias (unos minutos).
3. **File → Project Structure → Project → SDK**: si no hay ninguno, **Download JDK… → versión 21 → Eclipse Temurin** → OK.
4. El `.env` es **el mismo** que usa la versión Node (`mano-amiga-server/.env`): no hay que crear otro.
   Tiene que tener `MONGO_URI`, `JWT_SECRET`, `JWT_REFRESH_SECRET` y `QR_SECRET` (cada secreto de 32 caracteres o más).
5. **Apagá la API de Node** si está corriendo (usa el mismo puerto).
6. Abrí `src/main/java/uy/edu/um/manoamiga/ManoAmigaApplication.java` y tocá el ▶ verde al lado de `main` → **Run**.
7. En la consola tiene que aparecer `Tomcat started on port 3000`. Probá en el navegador:
   http://localhost:3000/api/v1/health → `{"ok":true}`.

El front-service y la app se levantan igual que siempre (ver el README general). La app no se entera del cambio.

> Si falta una variable, la API **no arranca** y dice cuál falta (por ejemplo `Falta la variable de entorno JWT_SECRET`).
> Es a propósito (5.6): mejor fallar al inicio que correr con un secreto vacío.

### 2.1 Datos iniciales (seed)

Igual que `npm run seed`: etiquetas, organización demo, admin, coordinador y (opcional) voluntario judicial, con los `SEED_*` del `.env`.

En IntelliJ: **Run → Edit Configurations… → ManoAmigaApplication → Active profiles: `seed`** → Run. Termina solo.
(Después borrá `seed` del campo para volver a levantar la API normal.)

Con Maven en la terminal: `mvn spring-boot:run -Dspring-boot.run.profiles=seed`

Si ya corrieron el seed con la versión Node sobre la misma base, no hace falta: los datos son los mismos.

## 3. Cómo está organizado

Un pedido recorre siempre:

```
JwtFiltro ──► SecurityConfig (matriz) ──► ReauthInterceptor ──► Controller (@Valid) ──► Service ──► MongoDB
autenticación   rol por ruta (RS1/RS4/RS8)   admin (RS9)          datos de entrada      RS2, RS3, ABAC
```

| Archivo | Qué hace | Requisito |
|---|---|---|
| `seguridad/Matriz.java` | La matriz endpoint × rol (43 endpoints). **Es la única fuente** del control por rol | RS10 |
| `seguridad/SecurityConfig.java` | Arma Spring Security **a partir de la matriz**: públicos con `permitAll()`, el resto con `hasAnyRole(...)`, y **cualquier otra ruta exige sesión** (denegar por defecto). Sin sesiones ni cookies (STATELESS), CSRF no aplica. Cabeceras de seguridad | RS1, RS4, RS8 |
| `seguridad/JwtFiltro.java` + `AutenticacionService.java` | Verifica el JWT (algoritmo fijo HS256, emisor, audiencia, vencimiento) y **relee al usuario en la base en cada pedido**: rol, organización activa y `versionSesion` | RS5 |
| `seguridad/ManejadoresSeguridad.java` | Respuestas 401 / 403 con el formato estándar; los 403 por rol quedan en la auditoría | RS1, RS9 |
| `seguridad/ReauthInterceptor.java` + `@RequiereReauth` | Las escrituras del panel de admin exigen `X-Reauth-Token` (5 min, de **ese** admin) | RS9 |
| `seguridad/TokenService.java` | Access token (15 min, solo `sub` + `ver`), refresh token opaco guardado como HMAC, token de reauth con otra audiencia | 5.2, RS5, RS9 |
| `seguridad/Limitadores.java` | 5 intentos fallidos / 15 min en login (IP + email) y en reauth (por admin) | 5.1 |
| `seguridad/LimiteTamanoFiltro.java` | Body de 100 KB como máximo | 5.4 |
| `web/entrada/*.java` | Los cuerpos de los pedidos: **lista blanca** de campos con validaciones (`@NotBlank`, `@Size`, `@Pattern`…). Lo que no está (rol, esJudicial, organizacionId…) se descarta | mass assignment |
| `servicio/PermisosService.java` | `assertMismaOrganizacion`, `assertTitular`, `assertABAC` | RS3, RS2, ABAC |
| `servicio/AuthService.java` | bcrypt (costo 12), login con mensaje genérico y tiempo constante, rotación de refresh tokens con detección de reuso, `revocarSesiones()` | 5.1, 5.2, RS5 |
| `servicio/InscripcionService.java` | Cupo **atómico** (`findAndModify` con `$expr`), IDOR, transiciones de estado | R3, R4, RS2 |
| `servicio/AdminService.java` | Organizaciones, coordinadores, condición judicial; al quitar permisos revoca al instante | R7, RS5 |
| `servicio/MapaService.java` | Solo ubicaciones institucionales; ABAC si hay sesión | R5, RS7 |
| `dto/Dto.java` | Cada respuesta con **solo** los campos que corresponden (nunca la entidad entera) | 5.5 |
| `error/ManejadorErrores.java` | Toda excepción → `{ error: { code, message } }`; los errores inesperados dan 500 sin detalles internos | 3.6 |
| `servicio/AuditoriaService.java` | Logins, denegaciones y acciones sensibles (nunca contraseñas ni tokens) | 5.9 |

**Inyección NoSQL:** en Java no hace falta el "sanitize" de la versión Node: cada campo del body se convierte a su
tipo (`String`, `Integer`…), así que un `{ "password": { "$ne": null } }` no se puede armar (da 400), y las consultas
se arman con `Criteria`, nunca concatenando texto.

## 4. Tests

Son tests de integración: levantan la API entera (con Spring Security) y le pegan por HTTP con MockMvc, como
`supertest` en la versión Node. Cubren la matriz completa (401 sin token y 403 para cada rol no permitido en
**cada** endpoint), IDOR, aislamiento entre organizaciones, ABAC, cupos concurrentes, QR firmado, revocación
inmediata, reautenticación del admin, mass assignment, el mapa y el perfil.

### 4.1 Con Docker (recomendado)

Con Docker Desktop **abierto**:
- IntelliJ: clic derecho en `src/test/java` → **Run 'All Tests'**.
- O en la terminal: `mvn test`.

La primera vez baja la imagen de MongoDB (unos minutos).

### 4.2 Sin Docker

Pueden usar una base de prueba propia con la variable `MONGO_TEST_URI` (en IntelliJ: *Run → Edit Configurations →
All Tests → Environment variables*). Los tests usan **siempre** la base `mano_amiga_test`, nunca la de desarrollo.
Si es en Atlas, al usuario hay que darle también `readWrite` sobre `mano_amiga_test` (Database Access → Edit).

## 5. Docker y AWS

Mismo esquema que la versión Node (ver `docs/DESPLIEGUE_AWS.md`), cambiando solo la API:

```
docker compose -f docker-compose.yml -f docker-compose.java.yml up -d --build
```

El contenedor corre sin root, con un tope de 600 MB de memoria (la instancia gratuita tiene 1 GB).
Seed dentro de Docker:

```
docker compose exec -e SEED_ADMIN_EMAIL=... -e SEED_ADMIN_PASSWORD=... -e SEED_COORD_EMAIL=... -e SEED_COORD_PASSWORD=... \
  api java -jar app.jar --spring.profiles.active=seed
```

## 6. Versiones

Java 21 (LTS), Spring Boot 3.5, Spring Security 6, jjwt 0.12, OpenPDF 2. Antes de la entrega conviene correr
`mvn versions:display-dependency-updates` y subir a la última versión de Spring Boot 3.5.x (parches de seguridad).
