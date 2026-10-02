const { Schema, model } = require('mongoose');

// Registro verificable de horas (5.8). Las horas cumplidas NO se guardan en el usuario:
// se calculan sumando las asistencias validadas.
const asistenciaSchema = new Schema(
  {
    inscripcionId: { type: Schema.Types.ObjectId, ref: 'Inscripcion', required: true },
    actividadId: { type: Schema.Types.ObjectId, ref: 'Actividad', required: true },
    voluntarioId: { type: Schema.Types.ObjectId, ref: 'Usuario', required: true },
    organizacionId: { type: Schema.Types.ObjectId, ref: 'Organizacion', required: true },
    checkIn: { type: Date, required: true },
    checkOut: { type: Date },
    horas: { type: Number, default: 0, min: 0 },
    metodo: { type: String, enum: ['qr', 'manual'], required: true },
    estado: { type: String, enum: ['abierta', 'validada'], default: 'abierta' },
    validadaPor: { type: Schema.Types.ObjectId, ref: 'Usuario' },
    validadaEn: { type: Date },
  },
  { timestamps: true, collection: 'asistencias' }
);

// Una asistencia por inscripción: el mismo QR no sirve dos veces para la misma persona.
asistenciaSchema.index({ inscripcionId: 1 }, { unique: true });
asistenciaSchema.index({ voluntarioId: 1 });
asistenciaSchema.index({ actividadId: 1 });

module.exports = model('Asistencia', asistenciaSchema);
