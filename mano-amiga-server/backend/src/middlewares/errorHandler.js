const ApiError = require('../utils/ApiError');
const logger = require('../utils/logger');

// Ruta inexistente -> 404 con el formato estándar.
function notFound(req, res) {
  res.status(404).json({ error: { code: 'RUTA_INEXISTENTE', message: 'La ruta no existe' } });
}

/**
 * Único manejador de errores (3.6). Transforma cualquier error en { error: { code, message, details? } }.
 * Los errores inesperados se loguean en el servidor y al cliente le llega un 500 genérico,
 * sin stack trace ni detalles internos.
 */
// eslint-disable-next-line no-unused-vars
function errorHandler(err, req, res, next) {
  if (err instanceof ApiError) {
    const body = { error: { code: err.code, message: err.message } };
    if (err.details) body.error.details = err.details;
    return res.status(err.status).json(body);
  }

  // JSON mal formado en el body
  if (err.type === 'entity.parse.failed') {
    return res.status(400).json({ error: { code: 'JSON_INVALIDO', message: 'El cuerpo del pedido no es JSON válido' } });
  }
  // Body demasiado grande (límite de 100 KB)
  if (err.type === 'entity.too.large') {
    return res.status(413).json({ error: { code: 'DEMASIADO_GRANDE', message: 'El pedido es demasiado grande' } });
  }
  // Id con formato inválido que llegó a Mongoose
  if (err.name === 'CastError') {
    return res.status(400).json({ error: { code: 'ID_INVALIDO', message: 'Identificador inválido' } });
  }
  // Índice único violado (email repetido, inscripción duplicada...)
  if (err.code === 11000) {
    return res.status(409).json({ error: { code: 'DUPLICADO', message: 'El recurso ya existe' } });
  }
  if (err.name === 'ValidationError') {
    return res.status(400).json({ error: { code: 'VALIDACION', message: 'Datos inválidos' } });
  }

  logger.error(`Error inesperado en ${req.method} ${req.originalUrl}`, err.stack || err);
  return res.status(500).json({ error: { code: 'ERROR_INTERNO', message: 'Ocurrió un error inesperado' } });
}

module.exports = { notFound, errorHandler };
