import { api } from './client';

export const listarCampanas = (params = {}) => api.get('/campanas', { params }).then((r) => r.data);

export const crearCampana = (datos) => api.post('/campanas', datos).then((r) => r.data);

export const listarPublicaciones = (proyectoId, params = {}) => api.get(`/proyectos/${proyectoId}/publicaciones`, { params }).then((r) => r.data);

export const publicar = (proyectoId, datos) => api.post(`/proyectos/${proyectoId}/publicaciones`, datos).then((r) => r.data);
