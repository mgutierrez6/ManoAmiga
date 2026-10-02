const { Schema, model } = require('mongoose');

// sensible = true -> un voluntario judicial no ve ni puede inscribirse a actividades con esta etiqueta (ABAC).
const etiquetaSchema = new Schema(
  {
    nombre: { type: String, required: true, unique: true, trim: true, maxlength: 40 },
    sensible: { type: Boolean, default: false },
  },
  { collection: 'etiquetas' }
);

module.exports = model('Etiqueta', etiquetaSchema);
