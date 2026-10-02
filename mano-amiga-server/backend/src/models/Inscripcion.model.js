const { Schema, model } = require('mongoose');

const inscripcionSchema = new Schema(
  {
    actividadId: { type: Schema.Types.ObjectId, ref: 'Actividad', required: true },
    voluntarioId: { type: Schema.Types.ObjectId, ref: 'Usuario', required: true },
    // Copia de la organización de la actividad: permite chequear RS3 sin otra consulta.
    organizacionId: { type: Schema.Types.ObjectId, ref: 'Organizacion', required: true },
    estado: { type: String, enum: ['pendiente', 'aceptada', 'rechazada', 'cancelada'], default: 'pendiente' },
    fecha: { type: Date, default: Date.now },
  },
  { timestamps: true, collection: 'inscripciones' }
);

// Impide inscripciones duplicadas incluso si llegan dos pedidos a la vez.
inscripcionSchema.index({ actividadId: 1, voluntarioId: 1 }, { unique: true });
inscripcionSchema.index({ voluntarioId: 1, fecha: -1 });

module.exports = model('Inscripcion', inscripcionSchema);
