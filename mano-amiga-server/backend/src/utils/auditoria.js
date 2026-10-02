const Auditoria = require('../models/Auditoria.model');
const logger = require('./logger');

/**
 * Registra una acción en la collection auditoria (5.9).
 * Nunca lanza: si la auditoría falla no debe romper la operación del usuario, pero queda en el log.
 * @param {object} e { usuarioId, accion, recurso, recursoId, resultado: 'ok'|'fallo'|'denegado', ip, detalle }
 */
async function registrar(e) {
  try {
    await Auditoria.create({
      usuarioId: e.usuarioId || null,
      accion: e.accion,
      recurso: e.recurso,
      recursoId: e.recursoId ? String(e.recursoId) : undefined,
      resultado: e.resultado,
      ip: e.ip,
      detalle: e.detalle,
    });
  } catch (err) {
    logger.error(`No se pudo registrar auditoría (${e.accion})`, err.message);
  }
}

module.exports = { registrar };
