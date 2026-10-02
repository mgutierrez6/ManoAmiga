const Joi = require('joi');

// Todo :id debe ser un ObjectId válido (24 caracteres hexadecimales).
const objectId = Joi.string().hex().length(24);

const idParam = Joi.object({ id: objectId.required() });

const paginacion = {
  page: Joi.number().integer().min(1).max(10000).default(1),
  limit: Joi.number().integer().min(1).max(50).default(20),
};

const ubicacion = Joi.object({
  lat: Joi.number().min(-90).max(90).required(),
  lng: Joi.number().min(-180).max(180).required(),
});

module.exports = { Joi, objectId, idParam, paginacion, ubicacion };
