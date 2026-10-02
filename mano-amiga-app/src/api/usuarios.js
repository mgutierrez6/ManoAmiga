import { api } from './client';

export const obtenerPerfil = () => api.get('/usuarios/me').then((r) => r.data);

export const misHoras = () => api.get('/usuarios/me/horas').then((r) => r.data);

export const registrarPushToken = (token) => api.post('/usuarios/me/push-token', { token });
