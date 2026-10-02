const { Schema, model } = require('mongoose');

// Refresh tokens activos. Se guarda SOLO el hash (HMAC-SHA256), nunca el token (5.2).
const sesionSchema = new Schema(
  {
    userId: { type: Schema.Types.ObjectId, ref: 'Usuario', required: true },
    tokenHash: { type: String, required: true, unique: true },
    familia: { type: String, required: true }, // cadena de rotación de un mismo inicio de sesión
    reemplazada: { type: Boolean, default: false }, // true = ya se usó; si vuelve a llegar, es un robo
    dispositivo: { type: String, maxlength: 200 },
    creadaEn: { type: Date, default: Date.now },
    expiraEn: { type: Date, required: true },
  },
  { collection: 'sesiones' }
);

// TTL: MongoDB borra solo las sesiones vencidas.
sesionSchema.index({ expiraEn: 1 }, { expireAfterSeconds: 0 });
sesionSchema.index({ userId: 1 });

module.exports = model('Sesion', sesionSchema);
