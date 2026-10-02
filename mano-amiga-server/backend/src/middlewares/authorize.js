const ApiError = require('../utils/ApiError');
const auditoria = require('../utils/auditoria');

/**
 * RS1 / RS4 · Control por rol en el servidor, en el 100 % de las rutas protegidas.
 * Uso: router.post('/', authorize('coordinador'), ...)
 * Los intentos denegados quedan en la auditoría (sirven para detectar abuso).
 */
function authorize(...rolesPermitidos) {
  if (rolesPermitidos.length === 0) throw new Error('authorize() necesita al menos un rol');
  return function authorizeMiddleware(req, res, next) {
    if (!req.user) throw ApiError.unauthorized();
    if (!rolesPermitidos.includes(req.user.rol)) {
      auditoria.registrar({
        usuarioId: req.user.id,
        accion: 'acceso_denegado_rol',
        recurso: `${req.method} ${req.baseUrl}${req.route ? req.route.path : ''}`,
        resultado: 'denegado',
        ip: req.ip,
      });
      throw ApiError.forbidden('ROL_NO_PERMITIDO', 'Tu rol no tiene permiso para esta acción');
    }
    next();
  };
}

module.exports = authorize;
