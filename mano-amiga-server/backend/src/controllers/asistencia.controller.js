const asistenciaService = require('../services/asistencia.service');
const ctx = require('./ctx');

async function generarQR(req, res) {
  res.set('Cache-Control', 'no-store');
  res.json(await asistenciaService.generarQR(req.user, req.valid.params.id, ctx(req)));
}

async function registrarConQR(req, res) {
  res.status(201).json(await asistenciaService.registrarConQR(req.user, req.valid.params.id, req.valid.body.qr, ctx(req)));
}

async function registrarManual(req, res) {
  res.status(201).json(await asistenciaService.registrarManual(req.user, req.valid.params.id, req.valid.body.horas, ctx(req)));
}

async function listarPorActividad(req, res) {
  res.json(await asistenciaService.listarPorActividad(req.user, req.valid.params.id, ctx(req)));
}

async function validar(req, res) {
  res.json(await asistenciaService.validar(req.user, req.valid.params.id, ctx(req)));
}

module.exports = { generarQR, registrarConQR, registrarManual, listarPorActividad, validar };
