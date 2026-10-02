/**
 * Protección contra inyección NoSQL (5.4): elimina toda clave que empiece con "$" o contenga "."
 * del body y de los params, para que nadie pueda mandar { "password": { "$ne": null } }.
 *
 * Hace lo mismo que express-mongo-sanitize, que no es compatible con Express 5.
 * El query string no hace falta: Express 5 usa el parser "simple", que no arma objetos anidados.
 */
function limpiar(valor) {
  if (Array.isArray(valor)) return valor.map(limpiar);
  if (valor && typeof valor === 'object') {
    const limpio = {};
    for (const [clave, v] of Object.entries(valor)) {
      if (clave.startsWith('$') || clave.includes('.')) continue;
      limpio[clave] = limpiar(v);
    }
    return limpio;
  }
  return valor;
}

function sanitize(req, res, next) {
  if (req.body && typeof req.body === 'object') req.body = limpiar(req.body);
  if (req.params) req.params = limpiar(req.params);
  next();
}

module.exports = sanitize;
module.exports.limpiar = limpiar;
