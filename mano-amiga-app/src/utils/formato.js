const MESES = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN', 'JUL', 'AGO', 'SET', 'OCT', 'NOV', 'DIC'];
const dos = (n) => String(n).padStart(2, '0');

export function fechaHora(valor) {
  if (!valor) return '';
  const d = new Date(valor);
  return `${d.getDate()} ${MESES[d.getMonth()].toLowerCase()} · ${dos(d.getHours())}:${dos(d.getMinutes())}`;
}

export function hora(valor) {
  if (!valor) return '';
  const d = new Date(valor);
  return `${dos(d.getHours())}:${dos(d.getMinutes())}`;
}

/** Para el "calendario" de las tarjetas: { dia: '24', mes: 'SET' } */
export function diaMes(valor) {
  const d = new Date(valor);
  return { dia: String(d.getDate()), mes: MESES[d.getMonth()] };
}

export function rangoFechas(inicio, fin) {
  if (!inicio) return '';
  const a = new Date(inicio);
  const b = new Date(fin);
  const mismoDia = a.toDateString() === b.toDateString();
  return mismoDia ? `${fechaHora(inicio)} a ${hora(fin)}` : `${fechaHora(inicio)} → ${fechaHora(fin)}`;
}

export const estadoTexto = {
  pendiente: 'Pendiente',
  aceptada: 'Aceptada',
  rechazada: 'Rechazada',
  cancelada: 'Cancelada',
  abierta: 'Sin validar',
  validada: 'Validada',
  publicada: 'Publicada',
  finalizada: 'Finalizada',
  activa: 'Activa',
};

/** Saludo según la hora del día. */
export function saludo() {
  const h = new Date().getHours();
  if (h < 12) return '¡Buen día';
  if (h < 20) return '¡Buenas tardes';
  return '¡Buenas noches';
}
