import Svg, { Circle, Ellipse, G, Path, Rect } from 'react-native-svg';
import { colores } from '../constants/tema';

/*
 * Ilustraciones propias hechas con formas simples (estilo "flat" tipo DrawKit).
 * Son SVG: pesan poco y se ven nítidas en cualquier pantalla.
 */

/** Login: un corazón del que brota una plantita, con sol, destellos y confeti. */
export function IlustracionBienvenida({ tamano = 220 }) {
  return (
    <Svg width={tamano} height={tamano} viewBox="0 0 200 200">
      {/* fondo */}
      <Path
        d="M100 12c38 0 78 22 84 62s-18 86-58 100-92 2-110-36S8 44 40 26 78 12 100 12z"
        fill={colores.sol}
      />
      <Circle cx="160" cy="44" r="16" fill={colores.blanco} opacity="0.9" />
      <Circle cx="160" cy="44" r="9" fill={colores.durazno} />
      {/* tallo y hojas */}
      <Path d="M100 92 C100 72 100 60 104 46" stroke={colores.mentaOscuro} strokeWidth="5" fill="none" strokeLinecap="round" />
      <Path d="M104 58 C118 44 134 46 138 52 C130 64 114 66 104 58z" fill={colores.menta} />
      <Path d="M101 70 C88 56 72 58 68 64 C76 76 92 78 101 70z" fill={colores.menta} />
      {/* corazón */}
      <Path
        d="M100 168s-46-28-58-56C33 91 44 70 66 70c14 0 24 8 34 21 10-13 20-21 34-21 22 0 33 21 24 42-12 28-58 56-58 56z"
        fill={colores.coral}
      />
      <Path d="M62 92c3-8 10-12 18-11" stroke={colores.blanco} strokeWidth="6" strokeLinecap="round" fill="none" opacity="0.6" />
      {/* cara del corazón */}
      <Circle cx="86" cy="116" r="4.5" fill={colores.tinta} />
      <Circle cx="114" cy="116" r="4.5" fill={colores.tinta} />
      <Path d="M90 128 q10 9 20 0" stroke={colores.tinta} strokeWidth="4" fill="none" strokeLinecap="round" />
      <Ellipse cx="76" cy="126" rx="6" ry="4" fill={colores.durazno} />
      <Ellipse cx="124" cy="126" rx="6" ry="4" fill={colores.durazno} />
      {/* destellos y confeti */}
      <Path d="M36 60 C37 66 39 68 45 69 C39 70 37 72 36 78 C35 72 33 70 27 69 C33 68 35 66 36 60Z" fill={colores.oceano} />
      <Path d="M170 120 C171 125 173 127 178 128 C173 129 171 131 170 136 C169 131 167 129 162 128 C167 127 169 125 170 120Z" fill={colores.lila} />
      <Rect x="24" y="126" width="10" height="10" rx="2" fill={colores.menta} transform="rotate(20 29 131)" />
      <Circle cx="150" cy="170" r="6" fill={colores.oceano} />
      <Circle cx="48" cy="170" r="4" fill={colores.lila} />
    </Svg>
  );
}

/** Estados vacíos: una caja de donaciones con corazón y confeti. */
export function IlustracionVacia({ tamano = 150 }) {
  return (
    <Svg width={tamano} height={tamano} viewBox="0 0 160 160">
      <Circle cx="80" cy="84" r="66" fill={colores.oceanoSuave} />
      <G>
        <Path d="M36 74 L80 58 L124 74 L124 122 L80 138 L36 122 Z" fill={colores.sol} />
        <Path d="M80 90 L124 74 L124 122 L80 138 Z" fill={colores.solOscuro} />
        <Path d="M36 74 L80 90 L80 138 L36 122 Z" fill={colores.sol} />
        <Path d="M36 74 L22 58 L66 42 L80 58 Z" fill={colores.durazno} />
        <Path d="M124 74 L138 58 L94 42 L80 58 Z" fill={colores.coralSuave} />
      </G>
      <Path
        d="M58 116s-12-7-15-14c-2-5 1-10 6-10 3 0 6 2 9 5 3-3 5-5 9-5 5 0 8 5 6 10-3 7-15 14-15 14z"
        fill={colores.coral}
      />
      <Path d="M80 28 C81 34 83 36 89 37 C83 38 81 40 80 46 C79 40 77 38 71 37 C77 36 79 34 80 28Z" fill={colores.lila} />
      <Circle cx="130" cy="36" r="5" fill={colores.menta} />
      <Circle cx="28" cy="44" r="4" fill={colores.coral} />
      <Rect x="132" y="98" width="9" height="9" rx="2" fill={colores.oceano} transform="rotate(25 136 102)" />
    </Svg>
  );
}

/** Perfil / éxito: una medalla con cinta. */
export function IlustracionMedalla({ tamano = 64 }) {
  return (
    <Svg width={tamano} height={tamano} viewBox="0 0 64 64">
      <Path d="M20 4 L30 26 L22 30 L12 8 Z" fill={colores.oceano} />
      <Path d="M44 4 L34 26 L42 30 L52 8 Z" fill={colores.coral} />
      <Circle cx="32" cy="40" r="18" fill={colores.sol} />
      <Circle cx="32" cy="40" r="12" fill={colores.solOscuro} />
      <Path d="M32 32 l2.6 5.3 5.8 0.8 -4.2 4.1 1 5.8 -5.2 -2.7 -5.2 2.7 1 -5.8 -4.2 -4.1 5.8 -0.8z" fill={colores.blanco} />
    </Svg>
  );
}
