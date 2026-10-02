const jwt = require('jsonwebtoken');
const config = require('../config/env');
const ApiError = require('../utils/ApiError');

const AUDIENCIA_QR = 'mano-amiga-qr';

/** Código firmado con QR_SECRET que incluye la actividad y vence a los 60 segundos (2.3, 5.8). */
function generar(actividadId) {
  const token = jwt.sign({ act: String(actividadId), typ: 'qr' }, config.qrSecret, {
    algorithm: 'HS256',
    expiresIn: config.qrTtlSegundos,
    audience: AUDIENCIA_QR,
    issuer: config.jwtIssuer,
  });
  return { qr: token, expiraEn: new Date(Date.now() + config.qrTtlSegundos * 1000) };
}

function verificar(token, actividadId) {
  let payload;
  try {
    payload = jwt.verify(token, config.qrSecret, { algorithms: ['HS256'], audience: AUDIENCIA_QR, issuer: config.jwtIssuer });
  } catch (err) {
    if (err.name === 'TokenExpiredError') throw ApiError.badRequest('QR_VENCIDO', 'El código QR venció, escaneá el nuevo');
    throw ApiError.badRequest('QR_INVALIDO', 'El código QR no es válido');
  }
  if (payload.typ !== 'qr' || payload.act !== String(actividadId)) {
    throw ApiError.badRequest('QR_INVALIDO', 'El código QR no corresponde a esta actividad');
  }
}

module.exports = { generar, verificar };
