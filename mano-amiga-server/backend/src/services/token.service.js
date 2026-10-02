const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const config = require('../config/env');

// Access token: vence a los 15 minutos y SOLO lleva el id del usuario (sin rol) -> RS5.
function firmarAccessToken(usuarioId) {
  return jwt.sign({ typ: 'access' }, config.jwtSecret, {
    algorithm: 'HS256',
    subject: String(usuarioId),
    expiresIn: config.accessTokenTtl,
    issuer: config.jwtIssuer,
    audience: config.jwtAudience,
  });
}

// Refresh token: 64 bytes aleatorios (opaco, no es un JWT). En la base guardamos solo su HMAC.
function generarRefreshToken() {
  return crypto.randomBytes(64).toString('hex');
}

function hashRefreshToken(token) {
  return crypto.createHmac('sha256', config.jwtRefreshSecret).update(token).digest('hex');
}

module.exports = { firmarAccessToken, generarRefreshToken, hashRefreshToken };
