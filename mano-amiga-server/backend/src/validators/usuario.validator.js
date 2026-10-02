const { Joi, paginacion } = require('./comunes');

const pushToken = {
  body: Joi.object({ token: Joi.string().trim().max(200).required() }),
};

const misInscripciones = {
  query: Joi.object({
    estado: Joi.string().valid('pendiente', 'aceptada', 'rechazada', 'cancelada'),
    ...paginacion,
  }),
};

module.exports = { pushToken, misInscripciones };
