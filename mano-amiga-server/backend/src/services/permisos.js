const ApiError = require('../utils/ApiError');
const auditoria = require('../utils/auditoria');
const { Etiqueta } = require('../models');

/**
 * RS3 · Aislamiento entre organizaciones (A2, CWE-862).
 * El coordinador solo opera sobre recursos de SU organización. La organización del usuario
 * viene de la base (middleware auth), nunca del body ni de la URL.
 */
function assertMismaOrganizacion(usuario, recurso, contexto = {}) {
  const orgRecurso = recurso && recurso.organizacionId ? String(recurso.organizacionId._id || recurso.organizacionId) : null;
  if (!usuario.organizacionId || !orgRecurso || orgRecurso !== usuario.organizacionId) {
    auditoria.registrar({
      usuarioId: usuario.id,
      accion: 'acceso_denegado_organizacion',
      recurso: contexto.recurso,
      recursoId: recurso && recurso._id,
      resultado: 'denegado',
      ip: contexto.ip,
    });
    throw ApiError.forbidden('OTRA_ORGANIZACION', 'El recurso pertenece a otra organización');
  }
}

/**
 * RS2 · Titularidad (A1, IDOR / CWE-639): el voluntario solo accede a lo suyo.
 */
function assertTitular(usuario, idDueno, contexto = {}) {
  if (String(idDueno) !== usuario.id) {
    auditoria.registrar({
      usuarioId: usuario.id,
      accion: 'acceso_denegado_titularidad',
      recurso: contexto.recurso,
      recursoId: contexto.recursoId,
      resultado: 'denegado',
      ip: contexto.ip,
    });
    throw ApiError.forbidden('NO_ES_TITULAR', 'El recurso no te pertenece');
  }
}

/** Ids de las etiquetas marcadas como sensibles (base de la regla ABAC). */
async function idsEtiquetasSensibles() {
  return Etiqueta.find({ sensible: true }).distinct('_id');
}

/**
 * ABAC · ¿Este usuario puede ver esta actividad?
 * Un voluntario judicial no accede a actividades con alguna etiqueta sensible.
 */
async function puedeVerActividad(usuario, actividad) {
  if (!usuario || !usuario.esJudicial) return true;
  const sensibles = (await idsEtiquetasSensibles()).map(String);
  const etiquetas = (actividad.etiquetas || []).map((e) => String(e._id || e));
  return !etiquetas.some((e) => sensibles.includes(e));
}

async function assertABAC(usuario, actividad, contexto = {}) {
  if (!(await puedeVerActividad(usuario, actividad))) {
    auditoria.registrar({
      usuarioId: usuario.id,
      accion: 'acceso_denegado_abac',
      recurso: 'actividad',
      recursoId: actividad._id,
      resultado: 'denegado',
      ip: contexto.ip,
    });
    throw ApiError.forbidden('ACTIVIDAD_RESTRINGIDA', 'No podés acceder a esta actividad');
  }
}

module.exports = { assertMismaOrganizacion, assertTitular, idsEtiquetasSensibles, puedeVerActividad, assertABAC };
