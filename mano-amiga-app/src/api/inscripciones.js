import { api } from './client';

export const inscribirme = (actividadId) => api.post(`/actividades/${actividadId}/inscripciones`).then((r) => r.data);

export const misInscripciones = (params = {}) => api.get('/usuarios/me/inscripciones', { params }).then((r) => r.data);

export const cancelarInscripcion = (id) => api.delete(`/inscripciones/${id}`);

// Coordinador
export const inscriptosDeActividad = (actividadId) => api.get(`/actividades/${actividadId}/inscripciones`).then((r) => r.data);

export const cambiarEstadoInscripcion = (id, estado) => api.patch(`/inscripciones/${id}`, { estado }).then((r) => r.data);

/** Descarga el PDF como bytes (pasa por el interceptor, así se renueva la sesión si hace falta). */
export const descargarCertificado = (id) =>
  api.get(`/inscripciones/${id}/certificado`, { responseType: 'arraybuffer', headers: { Accept: 'application/pdf' } }).then((r) => r.data);
