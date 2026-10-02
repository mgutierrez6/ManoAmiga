const { Router } = require('express');
const authorize = require('../middlewares/authorize');
const validate = require('../middlewares/validate');
const vi = require('../validators/inscripcion.validator');
const vs = require('../validators/asistencia.validator');
const inscripcion = require('../controllers/inscripcion.controller');
const asistencia = require('../controllers/asistencia.controller');

const router = Router();

router.delete('/:id', authorize('voluntario'), validate(vi.porId), inscripcion.cancelar); // titular (RS2)
router.patch('/:id', authorize('coordinador'), validate(vi.cambiarEstado), inscripcion.cambiarEstado); // org (RS3)
router.get('/:id/certificado', authorize('voluntario', 'coordinador'), validate(vi.porId), inscripcion.certificado);
router.post('/:id/asistencia-manual', authorize('coordinador'), validate(vs.registrarManual), asistencia.registrarManual);

module.exports = router;
