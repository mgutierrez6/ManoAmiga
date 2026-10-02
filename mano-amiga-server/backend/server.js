const config = require('./src/config/env');
const db = require('./src/config/db');
const logger = require('./src/utils/logger');
const crearApp = require('./app');

async function iniciar() {
  await db.conectar(config.mongoUri);
  const app = crearApp();
  const server = app.listen(config.port, config.host, () => {
    logger.info(`API de Mano Amiga escuchando en ${config.host}:${config.port} (${config.nodeEnv})`);
  });

  const cerrar = async () => {
    server.close();
    await db.desconectar();
    process.exit(0);
  };
  process.on('SIGINT', cerrar);
  process.on('SIGTERM', cerrar);
}

iniciar().catch((err) => {
  logger.error('No se pudo iniciar la API', err.message);
  process.exit(1);
});
