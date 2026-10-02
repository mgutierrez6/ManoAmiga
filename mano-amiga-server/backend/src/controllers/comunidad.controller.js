const comunidadService = require('../services/comunidad.service');
const ctx = require('./ctx');

async function listarCampanas(req, res) {
  res.json(await comunidadService.listarCampanas(req.valid.query));
}

async function crearCampana(req, res) {
  res.status(201).json(await comunidadService.crearCampana(req.user, req.valid.body, ctx(req)));
}

async function listarPublicaciones(req, res) {
  res.json(await comunidadService.listarPublicaciones(req.valid.params.id, req.valid.query));
}

async function crearPublicacion(req, res) {
  res.status(201).json(await comunidadService.crearPublicacion(req.user, req.valid.params.id, req.valid.body, ctx(req)));
}

module.exports = { listarCampanas, crearCampana, listarPublicaciones, crearPublicacion };
