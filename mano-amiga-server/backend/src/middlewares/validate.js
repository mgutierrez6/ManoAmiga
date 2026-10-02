const ApiError = require('../utils/ApiError');

/**
 * Valida params, query y body con esquemas Joi (5.4).
 * - stripUnknown: los campos que no están en el esquema se DESCARTAN (lista blanca contra mass assignment).
 * - Lo validado queda en req.valid.{params,query,body}; los controllers usan SOLO eso.
 */
function validate(esquemas) {
  return function validateMiddleware(req, res, next) {
    req.valid = req.valid || {};
    const detalles = [];
    for (const parte of ['params', 'query', 'body']) {
      if (!esquemas[parte]) continue;
      const { value, error } = esquemas[parte].validate(req[parte] || {}, {
        abortEarly: false,
        stripUnknown: true,
        convert: true,
      });
      if (error) {
        for (const d of error.details) {
          detalles.push({ campo: d.path.join('.'), motivo: d.message.replace(/"/g, '') });
        }
      } else {
        req.valid[parte] = value;
      }
    }
    if (detalles.length) throw ApiError.badRequest('VALIDACION', 'Datos inválidos', detalles);
    next();
  };
}

module.exports = validate;
