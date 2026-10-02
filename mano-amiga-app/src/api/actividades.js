import { api } from './client';

export const listarEtiquetas = () => api.get('/etiquetas').then((r) => r.data);

/** params: { etiqueta, page, limit, mias } */
export const listarActividades = (params = {}) => api.get('/actividades', { params }).then((r) => r.data);

export const obtenerActividad = (id) => api.get(`/actividades/${id}`).then((r) => r.data);

export const crearActividad = (datos) => api.post('/actividades', datos).then((r) => r.data);

export const editarActividad = (id, datos) => api.patch(`/actividades/${id}`, datos).then((r) => r.data);

export const cancelarActividad = (id) => api.delete(`/actividades/${id}`);
