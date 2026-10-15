# Mano Amiga · Despliegue en AWS EC2 (sección 7 del informe)

Objetivo: dejar el servidor en Internet con la arquitectura del informe:

```
Celular ──HTTPS 443──▶ [ front-service ]  ← DMZ: único contenedor con puertos publicados (80 y 443)
                              │ red interna de Docker (secure_net, sin salida a Internet)
                              ▼
                          [ API ]  ── egress_net ──▶ MongoDB Atlas  /  Expo Push
```

> Hacerlo **una sola persona**, con la cuenta de AWS del equipo. Tiempo estimado: 1 h la primera vez.

---

## 0. Antes de empezar: qué opción de base de datos usar

La instancia gratuita (`t3.micro` o `t2.micro`) tiene **1 GB de RAM**. MongoDB 7 solo ya pide ~1 GB, así que:

| Opción | Cómo | Cuándo |
|---|---|---|
| **A · Base en Atlas (recomendada)** | En EC2 corren solo front-service + API. La base sigue siendo el cluster M0 gratis de Atlas que ya usan. | Plan gratuito. Es lo que describe esta guía. |
| B · Mongo en la misma instancia | `docker compose --profile mongo-local up -d --build` (el `docker-compose.yml` ya lo trae, con el usuario de mínimos privilegios de `mongo-init/`). | Solo con una instancia de 2 GB o más (`t3.small`, consume créditos). |

Con la opción A la red `secure_net` sigue aislando a la API (no tiene puertos publicados); la salida a Atlas va por `egress_net` y Atlas solo acepta la IP fija del servidor.

---

## 1. Cuenta de AWS y alarma de gastos (5 min)

1. Entrar a https://console.aws.amazon.com con la cuenta del equipo (plan gratuito: 6 meses y hasta USD 200 de créditos).
2. Arriba a la derecha, elegir la región **us-east-1 (N. Virginia)** y usar siempre la misma.
3. Buscar **Billing and Cost Management → Budgets → Create budget → Zero spend budget** y poner los emails de las tres. Así avisa si algo empieza a cobrar.

## 2. Crear la instancia EC2 (10 min)

**EC2 → Instances → Launch instances**:

| Campo | Valor |
|---|---|
| Name | `mano-amiga` |
| AMI | **Ubuntu Server 24.04 LTS** (dice *Free tier eligible*) |
| Instance type | `t3.micro` (o `t2.micro`, la que diga *Free tier eligible*) |
| Key pair | **Create new key pair** → nombre `mano-amiga`, tipo ED25519, formato `.pem`. Se descarga UNA vez: guardarlo bien y **no subirlo a GitHub**. |
| Network settings → Edit | Crear security group `mano-amiga-sg` con estas reglas de entrada: |
| | `SSH` · TCP 22 · **My IP** (solo tu IP; nunca 0.0.0.0/0 para SSH) |
| | `HTTP` · TCP 80 · Anywhere (0.0.0.0/0) → solo para redirigir a HTTPS y renovar el certificado |
| | `HTTPS` · TCP 443 · Anywhere (0.0.0.0/0) |
| Storage | 20 GiB gp3 (el plan gratuito cubre hasta 30) |

**Launch instance**.

Este security group es el **FW 1** del informe (DMZ ↔ Internet). El **FW 2** (DMZ ↔ red segura) lo hace Docker: la API y Mongo no publican puertos.

### IP fija (Elastic IP)
**EC2 → Elastic IPs → Allocate Elastic IP address → Allocate**. Después **Actions → Associate** → elegir la instancia `mano-amiga`. Anotar esa IP (ej. `3.95.12.34`): es la que va en Atlas y en el dominio.

> Si cambian de IP de casa, actualizar la regla SSH (*Security groups → mano-amiga-sg → Edit inbound rules → My IP*).

## 3. Dejar entrar al servidor en Atlas (2 min)

Atlas → **Network Access → Add IP Address** → la Elastic IP con `/32` (ej. `3.95.12.34/32`) → comentario "EC2 producción". **No** usar `0.0.0.0/0`.

Recomendado: crear en **Database Access** un usuario aparte para producción (ej. `manoamiga_prod`, rol `readWrite` sobre `mano_amiga`) en lugar de reutilizar el de desarrollo. Y si todavía no lo hicieron, cambiar la contraseña de `manoamiga_app` (se compartió en una captura).

## 4. Dominio gratis (DuckDNS) (5 min)

El certificado HTTPS necesita un nombre, no una IP.

1. Entrar a https://www.duckdns.org con la cuenta de GitHub o Google.
2. Crear el subdominio, ej. `manoamiga-um` → queda `manoamiga-um.duckdns.org`.
3. En *current ip* poner la **Elastic IP** y tocar **update ip**.

Probar desde la compu: `ping manoamiga-um.duckdns.org` tiene que responder con la Elastic IP.

## 5. Conectarse al servidor (SSH)

Desde la carpeta donde está el `.pem`:

**Windows (PowerShell):**
```powershell
icacls .\mano-amiga.pem /inheritance:r /grant:r "$($env:USERNAME):(R)"
ssh -i .\mano-amiga.pem ubuntu@3.95.12.34
```
**Mac / Linux:**
```bash
chmod 400 mano-amiga.pem
ssh -i mano-amiga.pem ubuntu@3.95.12.34
```
Contestar `yes` la primera vez. Todo lo que sigue se escribe **dentro del servidor**.

## 6. Preparar el servidor (10 min)

```bash
# Actualizaciones de seguridad automáticas + todo al día
sudo apt update && sudo apt upgrade -y
sudo apt install -y unattended-upgrades git
sudo dpkg-reconfigure -plow unattended-upgrades     # elegir "Yes"

# 2 GB de swap: con 1 GB de RAM, el build de Docker se puede quedar sin memoria
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Docker (script oficial) y permiso para usarlo sin sudo
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
exit
```
Volver a entrar con el mismo `ssh ...` (para que tome el grupo `docker`) y comprobar: `docker compose version`.

## 7. Bajar el código y crear el `.env` de producción

```bash
git clone https://github.com/USUARIO/mano-amiga.git
cd mano-amiga/mano-amiga-server
cp .env.example .env
nano .env
```
Si el repositorio es privado, GitHub pide usuario y un **token** (GitHub → Settings → Developer settings → Personal access tokens → *Fine-grained*, solo lectura de ese repo).

Completar en el `.env` (en `nano`: se guarda con `Ctrl+O`, `Enter`, y se sale con `Ctrl+X`):

```ini
NODE_ENV=production
DOMINIO=manoamiga-um.duckdns.org
MONGO_URI_PRODUCCION=mongodb+srv://manoamiga_prod:CONTRASEÑA@cluster0.xxxxx.mongodb.net/mano_amiga?retryWrites=true&w=majority
JWT_SECRET=...
JWT_REFRESH_SECRET=...
QR_SECRET=...
```
Los tres secretos tienen que ser **nuevos y distintos a los de desarrollo**. Generarlos en el servidor:
```bash
for i in 1 2 3; do openssl rand -hex 48; done
```
Proteger el archivo: `chmod 600 .env`.

## 8. Certificado HTTPS (Let's Encrypt) (5 min)

Con el puerto 80 todavía libre:
```bash
sudo apt install -y certbot
sudo certbot certonly --standalone -d manoamiga-um.duckdns.org --agree-tos -m EMAIL_DEL_EQUIPO --no-eff-email
```
Queda en `/etc/letsencrypt/live/manoamiga-um.duckdns.org/`. El `docker-compose.yml` lo monta en el front-service como solo lectura.

Renovación automática (el certificado dura 90 días). Como el front-service ocupa el puerto 80, certbot lo para un momento y lo vuelve a prender:
```bash
sudo tee /etc/letsencrypt/renewal-hooks/pre/parar-front.sh > /dev/null <<'EOF'
#!/bin/sh
cd /home/ubuntu/mano-amiga/mano-amiga-server && docker compose stop front
EOF
sudo tee /etc/letsencrypt/renewal-hooks/post/prender-front.sh > /dev/null <<'EOF'
#!/bin/sh
cd /home/ubuntu/mano-amiga/mano-amiga-server && docker compose start front
EOF
sudo chmod +x /etc/letsencrypt/renewal-hooks/pre/parar-front.sh /etc/letsencrypt/renewal-hooks/post/prender-front.sh
sudo certbot renew --dry-run      # tiene que decir "Congratulations, all simulated renewals succeeded"
```

## 9. Levantar todo

```bash
cd ~/mano-amiga/mano-amiga-server
docker compose up -d --build        # la primera vez tarda unos minutos
docker compose ps                   # front y api en "running"
docker compose logs -f api          # Ctrl+C para salir de los logs
```

Probar desde el navegador de la compu o del celular:
- https://manoamiga-um.duckdns.org/api/v1/health → `{"ok":true}` (con candado)
- http://manoamiga-um.duckdns.org/api/v1/health → redirige a HTTPS

Comprobar que la API **no** está expuesta: `curl -m 5 http://3.95.12.34:3000` desde tu compu tiene que fallar (timeout).

### Datos iniciales (solo si la base está vacía)
Si usan el mismo cluster de Atlas que en desarrollo, los usuarios ya existen y esto no hace falta. Si es una base nueva:
```bash
docker compose exec \
  -e SEED_ADMIN_EMAIL=admin@manoamiga.org -e SEED_ADMIN_PASSWORD='UNA-CONTRASEÑA-LARGA' \
  -e SEED_COORD_EMAIL=coord@manoamiga.org -e SEED_COORD_PASSWORD='OTRA-CONTRASEÑA-LARGA' \
  api node src/scripts/seed.js
```
(Las contraseñas no quedan en el `.env` ni en el historial de Docker; conviene borrar el comando del historial con `history -c`.)

## 10. Apuntar la app al servidor

En `mano-amiga-app/.env` de cada integrante (o solo para el build de entrega):
```ini
EXPO_PUBLIC_API_URL=https://manoamiga-um.duckdns.org/api/v1
```
`npx expo start -c` y la app ya habla con el servidor de AWS, desde cualquier red (ya no hace falta la misma WiFi ni el hotspot).

Para instalar la app sin Expo Go (APK de Android): `npm install -g eas-cli`, `eas login`, `eas build -p android --profile preview`. El mapa en un build propio de Android necesita una API key de Google Maps (`android.config.googleMaps.apiKey` en `app.config.js`); en Expo Go funciona sin key.

## 11. Actualizar después de cada sprint

```bash
cd ~/mano-amiga && git pull
cd mano-amiga-server && docker compose up -d --build
docker image prune -f           # libera espacio de imágenes viejas
```

## 12. Problemas comunes

| Síntoma | Causa / solución |
|---|---|
| `ssh: Connection timed out` | Cambió tu IP: actualizar la regla SSH a *My IP*. |
| `Falta MONGO_URI_PRODUCCION` al hacer `up` | Falta esa línea en `mano-amiga-server/.env`. |
| La API se reinicia y el log dice `querySrv ETIMEOUT` o `Server selection timed out` | La Elastic IP no está en *Network Access* de Atlas. |
| `certbot` falla con *Timeout during connect* | El puerto 80 no está abierto en el security group, o DuckDNS no apunta a la Elastic IP. |
| El build se corta con `Killed` | Falta el swap (paso 6). |
| La app dice "No pudimos conectarnos" | ¿`EXPO_PUBLIC_API_URL` con `https://` y `/api/v1`? ¿Reiniciaste con `npx expo start -c`? |

## 13. Qué cuesta

Con el plan gratuito: la instancia `t3.micro`, 20 GB de disco y el tráfico de pruebas entran en los créditos. La IPv4 pública (Elastic IP) se cobra unos USD 3,6 por mes y también sale de los créditos. Atlas M0 y DuckDNS son gratis. **Al terminar el curso:** *Terminate instance* y *Release Elastic IP*, si no la IP sigue cobrando.
