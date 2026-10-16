// Identidad visual de Mano Amiga: alegre, cálida y colorida.
// Paleta inspirada en combinaciones tipo colorhunt (coral · sol · menta · océano) + un lila de acento.

// ---------------------------------------------------------------------------------------------
// Tema claro / oscuro (R16 · preferencias del perfil)
//
// "colores" es UN objeto compartido: al cambiar de tema se le copian los valores de la otra paleta
// (aplicarPaleta) y la app se vuelve a dibujar (ver context/TemaContext.js). Los estilos se arman con
// estilosDinamicos(() => ({ ... })), que los recalcula solo cuando cambió el tema.
//
// Convenciones:
//  - blanco / tintaFija: NO cambian. Son para texto e íconos SOBRE colores de marca (botones, héroes, QR).
//  - tarjeta / crema / tinta / tintaSuave / borde / *Suave: SÍ cambian (superficies y texto normal).
// ---------------------------------------------------------------------------------------------

const marca = {
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
  blanco: '#FFFFFF',
  tintaFija: '#073B4C',
  peligro: '#D62246',
  primario: '#EF476F',
  secundario: '#118AB2',
  advertencia: '#E0A930',
};

export const paletas = {
  claro: {
    ...marca,
    coralSuave: '#FDE3EA',
    solSuave: '#FFF3D1',
    mentaSuave: '#D5F8EE',
    oceanoSuave: '#D8EEF6',
    lilaSuave: '#EEE3FB',
    neutroSuave: '#ECEFF1',
    coralTexto: '#A3173A',
    solTexto: '#8A5A00',
    mentaTexto: '#03694F',
    oceanoTexto: '#0B5D78',
    lilaTexto: '#5B2A99',
    tinta: '#073B4C', // texto principal
    tintaSuave: '#4A6572', // texto secundario
    crema: '#FFF8EE', // fondo general
    tarjeta: '#FFFFFF', // superficies (tarjetas, inputs, barra de pestañas)
    campoFoco: '#FFFDFB',
    pista: '#F1E8DA', // fondo de barras de progreso
    borde: '#EADFCF',
    gris: '#8A9BA3',
    sombra: '#073B4C',
  },
  oscuro: {
    ...marca,
    coralSuave: '#4A1F2E',
    solSuave: '#463813',
    mentaSuave: '#0F3D33',
    oceanoSuave: '#11344A',
    lilaSuave: '#33224D',
    neutroSuave: '#24363F',
    coralTexto: '#FF9BB2',
    solTexto: '#FFD166',
    mentaTexto: '#5CE6BF',
    oceanoTexto: '#82D1EE',
    lilaTexto: '#CDB0F5',
    tinta: '#F4EDE2',
    tintaSuave: '#A9BCC4',
    crema: '#0E1C24',
    tarjeta: '#182B35',
    campoFoco: '#1E3440',
    pista: '#2A3F4A',
    borde: '#2C4552',
    gris: '#7E949D',
    sombra: '#000000',
  },
};

// Alias que usan las pantallas existentes.
function conAlias(p) {
  return { ...p, primarioClaro: p.coralSuave, texto: p.tinta, textoSuave: p.tintaSuave, fondo: p.crema };
}

export const colores = conAlias(paletas.claro);

let versionTema = 0;
export let modoActual = 'claro';

/** Copia la paleta pedida en "colores". La llama TemaContext antes de redibujar. */
export function aplicarPaleta(modo) {
  const m = modo === 'oscuro' ? 'oscuro' : 'claro';
  if (m === modoActual) return;
  Object.assign(colores, conAlias(paletas[m]));
  modoActual = m;
  versionTema += 1;
}

/**
 * Reemplazo de StyleSheet.create para estilos que dependen de los colores.
 * Uso: const styles = estilosDinamicos(() => ({ caja: { backgroundColor: colores.tarjeta } }));
 * Devuelve un objeto que se recalcula (una sola vez) cada vez que cambia el tema.
 */
export function estilosDinamicos(fabrica) {
  let cache = null;
  let version = -1;
  const actual = () => {
    if (version !== versionTema) {
      cache = fabrica();
      version = versionTema;
    }
    return cache;
  };
  return new Proxy(
    {},
    {
      get: (_, k) => actual()[k],
      has: (_, k) => k in actual(),
      ownKeys: () => Reflect.ownKeys(actual()),
      getOwnPropertyDescriptor: (_, k) => ({ value: actual()[k], enumerable: true, configurable: true, writable: false }),
    }
  );
}

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
const NOMBRES_FAMILIA = ['coral', 'sol', 'menta', 'oceano', 'lila'];
const TEXTO_FAMILIA = { coral: 'blanco', sol: 'tintaFija', menta: 'tintaFija', oceano: 'blanco', lila: 'blanco' };
export const familias = estilosDinamicos(() =>
  NOMBRES_FAMILIA.map((n) => ({ base: colores[n], oscuro: colores[`${n}Oscuro`], suave: colores[`${n}Suave`], texto: colores[TEXTO_FAMILIA[n]], textoSuave: colores[`${n}Texto`] }))
);

/** Devuelve una familia de color estable a partir de un texto o número (misma etiqueta = mismo color). */
export function familiaPara(clave) {
  const s = String(clave ?? '');
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
  return familias[h % familias.length];
}

// Estados: color + emoji para que se entiendan de un vistazo (no solo por color).
export const estilosEstado = estilosDinamicos(() => ({
  pendiente: { fondo: colores.solSuave, texto: colores.solTexto, emoji: '⏳' },
  aceptada: { fondo: colores.mentaSuave, texto: colores.mentaTexto, emoji: '✅' },
  rechazada: { fondo: colores.coralSuave, texto: colores.coralTexto, emoji: '✖️' },
  cancelada: { fondo: colores.neutroSuave, texto: colores.tintaSuave, emoji: '🚫' },
  abierta: { fondo: colores.oceanoSuave, texto: colores.oceanoTexto, emoji: '🕒' },
  validada: { fondo: colores.mentaSuave, texto: colores.mentaTexto, emoji: '🏅' },
  publicada: { fondo: colores.mentaSuave, texto: colores.mentaTexto, emoji: '🟢' },
  finalizada: { fondo: colores.neutroSuave, texto: colores.tintaSuave, emoji: '🏁' },
  activa: { fondo: colores.mentaSuave, texto: colores.mentaTexto, emoji: '🟢' },
  // Sprint 2
  admin: { fondo: colores.lilaSuave, texto: colores.lilaTexto, emoji: '🛠' },
  inactiva: { fondo: colores.neutroSuave, texto: colores.tintaSuave, emoji: '⏸' },
}));

// Compatibilidad con código anterior
export const coloresEstado = estilosDinamicos(() => Object.fromEntries(Object.entries(estilosEstado).map(([k, v]) => [k, v.texto])));

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
  shadowColor: '#000000',
  shadowOpacity: 0.08,
  shadowRadius: 12,
  shadowOffset: { width: 0, height: 6 },
  elevation: 3,
};
