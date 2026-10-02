// Datos de contexto que los services necesitan para la auditoría, sin pasarles "req" (6.3).
module.exports = function ctx(req) {
  return { ip: req.ip, dispositivo: req.get('user-agent') || '' };
};
