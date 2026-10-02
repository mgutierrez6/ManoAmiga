const { Joi, idParam } = require('./comunes');

const porId = { params: idParam };

const cambiarEstado = {
  params: idParam,
  body: Joi.object({ estado: Joi.string().valid('aceptada', 'rechazada').required() }),
};

const listarPorActividad = {
  params: idParam,
  query: Joi.object({ estado: Joi.string().valid('pendiente', 'aceptada', 'rechazada', 'cancelada') }),
};

module.exports = { porId, cambiarEstado, listarPorActividad };
