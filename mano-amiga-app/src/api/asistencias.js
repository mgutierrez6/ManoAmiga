import { api } from './client';

// Coordinador
export const obtenerQR = (actividadId) => api.get(`/actividades/${actividadId}/qr`).then((r) => r.data);

export const asistenciasDeActividad = (actividadId) => api.get(`/actividades/${actividadId}/asistencias`).then((r) => r.data);

export const validarAsistencia = (id) => api.patch(`/asistencias/${id}/validar`).then((r) => r.data);

export const asistenciaManual = (inscripcionId, horas) =>
  api.post(`/inscripciones/${inscripcionId}/asistencia-manual`, horas ? { horas } : {}).then((r) => r.data);

// Voluntario
export const registrarAsistenciaQR = (actividadId, qr) => api.post(`/actividades/${actividadId}/asistencias`, { qr }).then((r) => r.data);
