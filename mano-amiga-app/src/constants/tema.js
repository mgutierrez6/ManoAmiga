// Identidad visual de Mano Amiga: alegre, cálida y colorida.
// Paleta inspirada en combinaciones tipo colorhunt (coral · sol · menta · océano) + un lila de acento.

export const colores = {
  // Marca
  coral: '#EF476F',
  coralOscuro: '#C9304F',
  sol: '#FFD166',
  solOscuro: '#E0A930',
  menta: '#06D6A0',
  mentaOscuro: '#05A87E',
  oceano: '#118AB2',
  oceanoOscuro: '#0B6A8A',
  lila: '#9B5DE5',
  lilaOscuro: '#7A3FC4',
  durazno: '#FFB4A2',

  // Tintes suaves (fondos de chips, tarjetas y estados)
  coralSuave: '#FDE3EA',
  solSuave: '#FFF3D1',
  mentaSuave: '#D5F8EE',
  oceanoSuave: '#D8EEF6',
  lilaSuave: '#EEE3FB',

  // Neutros
  tinta: '#073B4C', // texto principal
  tintaSuave: '#4A6572', // texto secundario
  crema: '#FFF8EE', // fondo general
  blanco: '#FFFFFF',
  borde: '#EADFCF',
  gris: '#8A9BA3',

  // Alias usados por pantallas existentes
  primario: '#EF476F',
  primarioClaro: '#FDE3EA',
  secundario: '#118AB2',
  peligro: '#D62246',
  advertencia: '#E0A930',
  texto: '#073B4C',
  textoSuave: '#4A6572',
  fondo: '#FFF8EE',
  tarjeta: '#FFFFFF',
};

// Familias cargadas en App.js con expo-font (Fredoka para títulos, Nunito para texto).
export const fuentes = {
  titulo: 'Fredoka_700Bold',
  tituloMedio: 'Fredoka_600SemiBold',
  texto: 'Nunito_400Regular',
  textoMedio: 'Nunito_600SemiBold',
  negrita: 'Nunito_800ExtraBold',
};

export const espacio = { xs: 4, s: 8, m: 12, l: 16, xl: 24, xxl: 32 };

export const radio = 20;

// Cada "familia" de color: fondo sólido, versión oscura (sombra 3D del botón) y tinte suave.
export const familias = [
  { base: colores.coral, oscuro: colores.coralOscuro, suave: colores.coralSuave, texto: colores.blanco },
  { base: colores.sol, oscuro: colores.solOscuro, suave: colores.solSuave, texto: colores.tinta },
  { base: colores.menta, oscuro: colores.mentaOscuro, suave: colores.mentaSuave, texto: colores.tinta },
  { base: colores.oceano, oscuro: colores.oceanoOscuro, suave: colores.oceanoSuave, texto: colores.blanco },
  { base: colores.lila, oscuro: colores.lilaOscuro, suave: colores.lilaSuave, texto: colores.blanco },
];

/** Devuelve una familia de color estable a partir de un texto o número (misma etiqueta = mismo color). */
export function familiaPara(clave) {
  const s = String(clave ?? '');
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
  return familias[h % familias.length];
}

// Estados: color + emoji para que se entiendan de un vistazo (no solo por color).
export const estilosEstado = {
  pendiente: { fondo: colores.solSuave, texto: '#8A5A00', emoji: '⏳' },
  aceptada: { fondo: colores.mentaSuave, texto: '#03694F', emoji: '✅' },
  rechazada: { fondo: colores.coralSuave, texto: '#A3173A', emoji: '✖️' },
  cancelada: { fondo: '#ECEFF1', texto: '#4A6572', emoji: '🚫' },
  abierta: { fondo: colores.oceanoSuave, texto: '#0B5D78', emoji: '🕒' },
  validada: { fondo: colores.mentaSuave, texto: '#03694F', emoji: '🏅' },
  publicada: { fondo: colores.mentaSuave, texto: '#03694F', emoji: '🟢' },
  finalizada: { fondo: '#ECEFF1', texto: '#4A6572', emoji: '🏁' },
  activa: { fondo: colores.mentaSuave, texto: '#03694F', emoji: '🟢' },
};

// Compatibilidad con código anterior
export const coloresEstado = Object.fromEntries(Object.entries(estilosEstado).map(([k, v]) => [k, v.texto]));

// Emoji por etiqueta (si no está, se usa 🤝).
const EMOJI_ETIQUETA = {
  'ollas populares': '🍲',
  medioambiente: '🌱',
  urgente: '⚡',
  donaciones: '🎁',
  'adultos mayores': '👵',
  'apto programa judicial': '⚖️',
  'apoyo escolar': '📚',
  'contacto con menores': '🧸',
  'hogares de niños': '🏠',
  oratorios: '⛪',
};

export function emojiEtiqueta(nombre) {
  return EMOJI_ETIQUETA[String(nombre || '').toLowerCase()] || '🤝';
}

// Sombra suave "de papel" para tarjetas.
export const sombra = {
  shadowColor: '#073B4C',
  shadowOpacity: 0.08,
  shadowRadius: 12,
  shadowOffset: { width: 0, height: 6 },
  elevation: 3,
};
