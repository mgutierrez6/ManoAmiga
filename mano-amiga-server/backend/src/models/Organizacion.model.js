const { Schema, model } = require('mongoose');

const organizacionSchema = new Schema(
  {
    nombre: { type: String, required: true, unique: true, trim: true, maxlength: 100 },
    contacto: { type: String, trim: true, maxlength: 120 },
    convenioOSLA: { type: Boolean, default: false },
    // Si la organización se da de baja, sus coordinadores pierden acceso en el siguiente pedido (RS5).
    activa: { type: Boolean, default: true },
  },
  { timestamps: true, collection: 'organizaciones' }
);

module.exports = model('Organizacion', organizacionSchema);
