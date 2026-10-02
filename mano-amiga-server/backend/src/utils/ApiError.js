// Error "esperado" que lanzan los services cuando una regla no se cumple (3.6).
// El errorHandler lo transforma en { error: { code, message, details? } }.
class ApiError extends Error {
  constructor(status, code, message, details) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details;
  }

  static badRequest(code, message, details) { return new ApiError(400, code, message, details); }
  static unauthorized(code = 'NO_AUTENTICADO', message = 'Necesitás iniciar sesión') { return new ApiError(401, code, message); }
  static forbidden(code = 'SIN_PERMISO', message = 'No tenés permiso para esta acción') { return new ApiError(403, code, message); }
  static notFound(code = 'NO_ENCONTRADO', message = 'El recurso no existe') { return new ApiError(404, code, message); }
  static conflict(code, message) { return new ApiError(409, code, message); }
}

module.exports = ApiError;
