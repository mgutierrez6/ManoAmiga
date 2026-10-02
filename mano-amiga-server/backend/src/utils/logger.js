// Logger mínimo. Regla (5.9): nunca loguear contraseñas, tokens ni datos personales completos.
const config = require('../config/env');

function linea(nivel, mensaje, extra) {
  if (config.esTest && nivel !== 'error') return;
  const base = `[${new Date().toISOString()}] ${nivel.toUpperCase()} ${mensaje}`;
  if (extra) console[nivel === 'error' ? 'error' : 'log'](base, extra);
  else console[nivel === 'error' ? 'error' : 'log'](base);
}

module.exports = {
  info: (m, e) => linea('info', m, e),
  warn: (m, e) => linea('warn', m, e),
  error: (m, e) => linea('error', m, e),
};
