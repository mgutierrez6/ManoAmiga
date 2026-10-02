const { Router } = require('express');
const authorize = require('../middlewares/authorize');
const validate = require('../middlewares/validate');
const vs = require('../validators/asistencia.validator');
const asistencia = require('../controllers/asistencia.controller');

const router = Router();

router.patch('/:id/validar', authorize('coordinador'), validate(vs.porId), asistencia.validar);

module.exports = router;
