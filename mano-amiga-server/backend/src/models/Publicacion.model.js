const { Schema, model } = require('mongoose');

// Feed de cada proyecto. Un "proyecto" es una actividad o una campaña.
const publicacionSchema = new Schema(
  {
    proyectoId: { type: Schema.Types.ObjectId, required: true },
    proyectoTipo: { type: String, enum: ['actividad', 'campana'], required: true },
    organizacionId: { type: Schema.Types.ObjectId, ref: 'Organizacion', required: true },
    autorId: { type: Schema.Types.ObjectId, ref: 'Usuario', required: true },
    tipo: { type: String, enum: ['post', 'convocatoria'], default: 'post' },
    contenido: { type: String, required: true, trim: true, maxlength: 2000 },
    fecha: { type: Date, default: Date.now },
  },
  { collection: 'publicaciones' }
);

publicacionSchema.index({ proyectoId: 1, fecha: -1 });

module.exports = model('Publicacion', publicacionSchema);
