const config = require('../config/env');
const logger = require('../utils/logger');
const { Usuario } = require('../models');

const EXPO_PUSH_URL = 'https://exp.host/--/api/v2/push/send';
const LOTE = 100; // Expo acepta hasta 100 mensajes por pedido

function esPushTokenValido(token) {
  return typeof token === 'string' && /^(ExponentPushToken|ExpoPushToken)\[[A-Za-z0-9_-]+\]$/.test(token);
}

/**
 * Envía una notificación push a uno o varios usuarios vía el servicio de Expo (-> FCM / APNs).
 * Se usa la API HTTP de Expo directamente (una dependencia menos).
 * Límite de confianza 3 (5.11): el texto NUNCA incluye datos personales (ni teléfono, ni condición judicial).
 * Es "best effort": si falla, se loguea y la operación principal sigue.
 */
async function notificar(usuarioIds, titulo, cuerpo, data = {}) {
  if (config.esTest) return;
  try {
    const usuarios = await Usuario.find({ _id: { $in: [].concat(usuarioIds) } }).select('+pushTokens').lean();
    const mensajes = [];
    for (const u of usuarios) {
      for (const token of u.pushTokens || []) {
        if (esPushTokenValido(token)) mensajes.push({ to: token, sound: 'default', title: titulo, body: cuerpo, data });
      }
    }
    for (let i = 0; i < mensajes.length; i += LOTE) {
      const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
      if (config.expoAccessToken) headers.Authorization = `Bearer ${config.expoAccessToken}`;
      const res = await fetch(EXPO_PUSH_URL, { method: 'POST', headers, body: JSON.stringify(mensajes.slice(i, i + LOTE)) });
      if (!res.ok) logger.warn(`Expo Push respondió ${res.status}`);
    }
  } catch (err) {
    logger.warn('No se pudo enviar una notificación push', err.message);
  }
}

module.exports = { notificar, esPushTokenValido };
