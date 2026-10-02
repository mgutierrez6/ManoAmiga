const usuarioService = require('../services/usuario.service');
const inscripcionService = require('../services/inscripcion.service');
const asistenciaService = require('../services/asistencia.service');

async function me(req, res) {
  res.json(await usuarioService.obtenerPerfil(req.user));
}

async function pushToken(req, res) {
  await usuarioService.registrarPushToken(req.user, req.valid.body.token);
  res.status(204).end();
}

async function misInscripciones(req, res) {
  res.json(await inscripcionService.misInscripciones(req.user, req.valid.query));
}

async function misHoras(req, res) {
  res.json(await asistenciaService.misHoras(req.user));
}

module.exports = { me, pushToken, misInscripciones, misHoras };
