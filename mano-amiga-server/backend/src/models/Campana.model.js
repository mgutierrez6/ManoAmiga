const { Schema, model } = require('mongoose');
const { puntoSchema } = require('./Actividad.model');

const puntoAcopioSchema = new Schema(
  {
    nombre: { type: String, required: true, trim: true, maxlength: 100 },
    direccion: { type: String, trim: true, maxlength: 200 },
    ubicacion: { type: puntoSchema, required: true },
  },
  { _id: true }
);

const campanaSchema = new Schema(
  {
    organizacionId: { type: Schema.Types.ObjectId, ref: 'Organizacion', required: true },
    creadaPor: { type: Schema.Types.ObjectId, ref: 'Usuario', required: true },
    titulo: { type: String, required: true, trim: true, maxlength: 100 },
    descripcion: { type: String, required: true, trim: true, maxlength: 2000 },
    objetivo: { type: String, trim: true, maxlength: 300 },
    puntos: { type: [puntoAcopioSchema], default: [] },
    fechaInicio: { type: Date, required: true },
    fechaFin: { type: Date, required: true },
    estado: { type: String, enum: ['activa', 'finalizada'], default: 'activa' },
  },
  { timestamps: true, collection: 'campanas' }
);

campanaSchema.index({ 'puntos.ubicacion': '2dsphere' });
campanaSchema.index({ organizacionId: 1 });

module.exports = model('Campana', campanaSchema);
