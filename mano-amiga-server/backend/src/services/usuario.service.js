const ApiError = require('../utils/ApiError');
const { Usuario } = require('../models');
const dto = require('./dto');
const { esPushTokenValido } = require('./notificacion.service');

// /usuarios/me/... siempre toma el id del token, nunca de la URL (RS2).
async function obtenerPerfil(usuario) {
  const u = await Usuario.findById(usuario.id).lean();
  if (!u) throw ApiError.notFound();
  return dto.usuarioPropio(u);
}

async function registrarPushToken(usuario, token) {
  if (!esPushTokenValido(token)) throw ApiError.badRequest('PUSH_TOKEN_INVALIDO', 'Token de notificaciones inválido');
  // Guardamos como máximo 5 dispositivos por usuario.
  await Usuario.updateOne({ _id: usuario.id }, { $pull: { pushTokens: token } });
  await Usuario.updateOne({ _id: usuario.id }, { $push: { pushTokens: { $each: [token], $slice: -5 } } });
}

module.exports = { obtenerPerfil, registrarPushToken };
