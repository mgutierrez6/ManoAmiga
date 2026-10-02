const { Joi, idParam } = require('./comunes');

const porId = { params: idParam };

const registrarConQR = {
  params: idParam,
  body: Joi.object({ qr: Joi.string().trim().max(1000).required() }),
};

const registrarManual = {
  params: idParam,
  body: Joi.object({ horas: Joi.number().min(0.25).max(24) }),
};

module.exports = { porId, registrarConQR, registrarManual };
