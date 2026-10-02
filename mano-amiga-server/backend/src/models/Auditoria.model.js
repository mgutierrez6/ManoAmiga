const { Schema, model } = require('mongoose');

// Quién hizo qué, cuándo y desde dónde (5.9). Nunca contraseñas, tokens ni datos personales completos.
const auditoriaSchema = new Schema(
  {
    usuarioId: { type: Schema.Types.ObjectId, ref: 'Usuario', default: null },
    accion: { type: String, required: true },
    recurso: { type: String },
    recursoId: { type: String },
    resultado: { type: String, enum: ['ok', 'fallo', 'denegado'], required: true },
    ip: { type: String },
    detalle: { type: String, maxlength: 300 },
    fecha: { type: Date, default: Date.now },
  },
  { collection: 'auditoria' }
);

auditoriaSchema.index({ usuarioId: 1, fecha: -1 });

module.exports = model('Auditoria', auditoriaSchema);
