const { Router } = require('express');
const authorize = require('../middlewares/authorize');
const validate = require('../middlewares/validate');
const v = require('../validators/comunidad.validator');
const c = require('../controllers/comunidad.controller');

const campanas = Router();
campanas.get('/', validate(v.listarCampanas), c.listarCampanas); // pública (RS8)
campanas.post('/', authorize('coordinador'), validate(v.crearCampana), c.crearCampana);

const proyectos = Router();
proyectos.get('/:id/publicaciones', validate(v.listarPublicaciones), c.listarPublicaciones); // pública (RS8)
proyectos.post('/:id/publicaciones', authorize('coordinador'), validate(v.crearPublicacion), c.crearPublicacion);

module.exports = { campanas, proyectos };
