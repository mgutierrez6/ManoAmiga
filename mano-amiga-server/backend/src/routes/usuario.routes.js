const { Router } = require('express');
const authorize = require('../middlewares/authorize');
const validate = require('../middlewares/validate');
const v = require('../validators/usuario.validator');
const c = require('../controllers/usuario.controller');
const { TODOS } = require('../security/matriz');

// Todas las rutas /usuarios/me/... toman el id del token, nunca de la URL (RS2).
const router = Router();

router.get('/me', authorize(...TODOS), c.me);
router.post('/me/push-token', authorize(...TODOS), validate(v.pushToken), c.pushToken);
router.get('/me/inscripciones', authorize('voluntario'), validate(v.misInscripciones), c.misInscripciones);
router.get('/me/horas', authorize('voluntario'), c.misHoras);

module.exports = router;
