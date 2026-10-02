const actividadService = require('../services/actividad.service');
const ctx = require('./ctx');

async function etiquetas(req, res) {
  res.json(await actividadService.listarEtiquetas());
}

async function listar(req, res) {
  res.json(await actividadService.listar(req.user, req.valid.query));
}

async function obtener(req, res) {
  res.json(await actividadService.obtener(req.user, req.valid.params.id, ctx(req)));
}

async function crear(req, res) {
  res.status(201).json(await actividadService.crear(req.user, req.valid.body, ctx(req)));
}

async function actualizar(req, res) {
  res.json(await actividadService.actualizar(req.user, req.valid.params.id, req.valid.body, ctx(req)));
}

async function eliminar(req, res) {
  await actividadService.eliminar(req.user, req.valid.params.id, ctx(req));
  res.status(204).end();
}

module.exports = { etiquetas, listar, obtener, crear, actualizar, eliminar };
