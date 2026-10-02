const inscripcionService = require('../services/inscripcion.service');
const certificadoService = require('../services/certificado.service');
const ctx = require('./ctx');

async function inscribir(req, res) {
  res.status(201).json(await inscripcionService.inscribir(req.user, req.valid.params.id, ctx(req)));
}

async function cancelar(req, res) {
  await inscripcionService.cancelar(req.user, req.valid.params.id, ctx(req));
  res.status(204).end();
}

async function listarPorActividad(req, res) {
  res.json(await inscripcionService.listarPorActividad(req.user, req.valid.params.id, req.valid.query, ctx(req)));
}

async function cambiarEstado(req, res) {
  res.json(await inscripcionService.cambiarEstado(req.user, req.valid.params.id, req.valid.body.estado, ctx(req)));
}

async function certificado(req, res) {
  const datos = await inscripcionService.datosCertificado(req.user, req.valid.params.id, ctx(req));
  const pdf = await certificadoService.generarPDF(datos);
  res.set({
    'Content-Type': 'application/pdf',
    'Content-Disposition': `attachment; filename="certificado-${datos.codigo}.pdf"`,
    'Cache-Control': 'no-store',
  });
  res.send(pdf);
}

module.exports = { inscribir, cancelar, listarPorActividad, cambiarEstado, certificado };
