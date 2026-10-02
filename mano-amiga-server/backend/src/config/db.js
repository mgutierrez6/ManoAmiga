const mongoose = require('mongoose');
const logger = require('../utils/logger');

// strictQuery: los filtros con campos que no existen en el esquema se ignoran (defensa extra ante inyección).
mongoose.set('strictQuery', true);

async function conectar(uri) {
  await mongoose.connect(uri, {
    serverSelectionTimeoutMS: 10000,
  });
  logger.info('Conectado a MongoDB');
}

async function desconectar() {
  await mongoose.disconnect();
}

module.exports = { conectar, desconectar };
