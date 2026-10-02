const express = require('express');
const helmet = require('helmet');
const config = require('./src/config/env');
const sanitize = require('./src/middlewares/sanitize');
const { requerirAuthSalvoListaBlanca } = require('./src/middlewares/auth');
const { notFound, errorHandler } = require('./src/middlewares/errorHandler');
const api = require('./src/routes');

function crearApp() {
  const app = express();

  // Solo el front-service le habla a la API: confiamos en 1 proxy para obtener la IP real del cliente.
  app.set('trust proxy', config.trustProxy);
  app.disable('x-powered-by');

  app.use(helmet()); // cabeceras de seguridad (5.7)
  app.use(express.json({ limit: '100kb' })); // límite de tamaño del body (5.4)
  app.use(sanitize); // contra inyección NoSQL (5.4)

  // RS8: todo /api/v1 exige JWT salvo la lista blanca de la matriz (RS10).
  app.use('/api/v1', requerirAuthSalvoListaBlanca, api);

  app.use(notFound);
  app.use(errorHandler);
  return app;
}

module.exports = crearApp;
