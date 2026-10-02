import { api } from './client';

export const login = (email, password) => api.post('/auth/login', { email, password }).then((r) => r.data);

export const registrar = (datos) => api.post('/auth/register', datos).then((r) => r.data);

export const logout = () => api.post('/auth/logout', {});
