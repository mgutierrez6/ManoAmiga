import { useEffect, useRef } from 'react';
import { Animated, Easing, StyleSheet, View } from 'react-native';
import Svg, { Circle, Path } from 'react-native-svg';
import { colores } from '../constants/tema';

/*
 * Figuras decorativas propias (SVG): blobs orgánicos, anillos, puntos, garabatos y destellos.
 * Se usan de fondo en los encabezados y pantallas vacías. No son interactivas (pointerEvents="none").
 */

const BLOBS = [
  'M44.7,-58.6C57.4,-48.6,66.8,-34.2,71.1,-18.1C75.4,-2,74.6,15.8,67,30.1C59.4,44.4,45,55.1,29.2,62.3C13.4,69.5,-3.8,73.2,-19.9,69.3C-36,65.4,-51,53.9,-61.3,38.9C-71.6,23.9,-77.2,5.4,-73.9,-11.3C-70.6,-28,-58.4,-42.9,-44,-52.7C-29.6,-62.5,-14.8,-67.2,0.9,-68.3C16.6,-69.4,32,-68.6,44.7,-58.6Z',
  'M39.5,-49.1C51.9,-40.9,63.2,-29.3,67.5,-15.1C71.8,-0.9,69.1,15.9,61.2,29.3C53.3,42.7,40.2,52.7,25.7,59.1C11.2,65.5,-4.7,68.3,-20.5,65C-36.3,61.7,-52,52.3,-61.6,38.5C-71.2,24.7,-74.7,6.5,-70.9,-9.8C-67.1,-26.1,-56,-40.5,-42.4,-48.5C-28.8,-56.5,-14.4,-58.1,0.3,-58.5C15,-58.9,27.1,-57.3,39.5,-49.1Z',
  'M51.3,-62.1C64.5,-50.6,71.6,-32.5,73.6,-14.4C75.6,3.7,72.5,21.8,63.2,35.7C53.9,49.6,38.4,59.3,21.4,65.4C4.4,71.5,-14.1,74,-29.6,67.8C-45.1,61.6,-57.6,46.7,-65.4,29.8C-73.2,12.9,-76.3,-6,-70.6,-21.6C-64.9,-37.2,-50.4,-49.5,-35.4,-60.4C-20.4,-71.3,-4.9,-80.8,10.8,-79.6C26.5,-78.4,38.1,-73.6,51.3,-62.1Z',
];

/** Hace que un hijo "flote" suavemente para dar movimiento a la pantalla. */
export function Flotante({ children, distancia = 8, duracion = 3200, retraso = 0, style }) {
  const v = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const anim = Animated.loop(
      Animated.sequence([
        Animated.timing(v, { toValue: 1, duration: duracion, delay: retraso, easing: Easing.inOut(Easing.sin), useNativeDriver: true }),
        Animated.timing(v, { toValue: 0, duration: duracion, easing: Easing.inOut(Easing.sin), useNativeDriver: true }),
      ])
    );
    anim.start();
    return () => anim.stop();
  }, [v, distancia, duracion, retraso]);
  const translateY = v.interpolate({ inputRange: [0, 1], outputRange: [0, -distancia] });
  const rotate = v.interpolate({ inputRange: [0, 1], outputRange: ['-3deg', '3deg'] });
  return <Animated.View style={[style, { transform: [{ translateY }, { rotate }] }]}>{children}</Animated.View>;
}

/** Hace girar lentamente a un hijo (para anillos punteados y destellos). */
export function Girando({ children, duracion = 18000, style }) {
  const v = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const anim = Animated.loop(Animated.timing(v, { toValue: 1, duration: duracion, easing: Easing.linear, useNativeDriver: true }));
    anim.start();
    return () => anim.stop();
  }, [v, duracion]);
  const rotate = v.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] });
  return <Animated.View style={[style, { transform: [{ rotate }] }]}>{children}</Animated.View>;
}

export function Blob({ tamano = 160, color = colores.sol, variante = 0, opacidad = 1 }) {
  return (
    <Svg width={tamano} height={tamano} viewBox="-80 -80 160 160">
      <Path d={BLOBS[variante % BLOBS.length]} fill={color} opacity={opacidad} />
    </Svg>
  );
}

export function Anillo({ tamano = 70, color = colores.blanco, grosor = 6, punteado = false, opacidad = 1 }) {
  const r = tamano / 2 - grosor;
  return (
    <Svg width={tamano} height={tamano}>
      <Circle
        cx={tamano / 2}
        cy={tamano / 2}
        r={r}
        stroke={color}
        strokeWidth={grosor}
        fill="none"
        opacity={opacidad}
        strokeDasharray={punteado ? `${grosor * 1.2} ${grosor * 1.6}` : undefined}
        strokeLinecap="round"
      />
    </Svg>
  );
}

export function Puntos({ filas = 4, columnas = 5, color = colores.blanco, separacion = 12, radio = 2.5, opacidad = 0.7 }) {
  const puntos = [];
  for (let f = 0; f < filas; f++) for (let c = 0; c < columnas; c++) puntos.push([c * separacion + radio, f * separacion + radio]);
  return (
    <Svg width={(columnas - 1) * separacion + radio * 2} height={(filas - 1) * separacion + radio * 2}>
      {puntos.map(([x, y]) => (
        <Circle key={`${x}-${y}`} cx={x} cy={y} r={radio} fill={color} opacity={opacidad} />
      ))}
    </Svg>
  );
}

export function Garabato({ ancho = 90, color = colores.blanco, grosor = 5, opacidad = 1 }) {
  return (
    <Svg width={ancho} height={ancho * 0.3} viewBox="0 0 100 30">
      <Path
        d="M3 20 C 13 3, 23 3, 33 15 S 53 27, 63 15 S 83 3, 97 12"
        stroke={color}
        strokeWidth={grosor}
        fill="none"
        strokeLinecap="round"
        opacity={opacidad}
      />
    </Svg>
  );
}

export function Destello({ tamano = 26, color = colores.sol }) {
  return (
    <Svg width={tamano} height={tamano} viewBox="0 0 24 24">
      <Path d="M12 0 C13 8 16 11 24 12 C16 13 13 16 12 24 C11 16 8 13 0 12 C8 11 11 8 12 0 Z" fill={color} />
    </Svg>
  );
}

export function Corazon({ tamano = 30, color = colores.coral }) {
  return (
    <Svg width={tamano} height={tamano} viewBox="0 0 24 24">
      <Path d="M12 21s-7.5-4.6-10-9.3C0.3 8.4 2.2 4 6.3 4c2.3 0 3.9 1.3 5.7 3.4C13.8 5.3 15.4 4 17.7 4c4.1 0 6 4.4 4.3 7.7C19.5 16.4 12 21 12 21z" fill={color} />
    </Svg>
  );
}

export function MedioCirculo({ tamano = 60, color = colores.menta, opacidad = 1 }) {
  return (
    <Svg width={tamano} height={tamano / 2}>
      <Path d={`M0 ${tamano / 2} A ${tamano / 2} ${tamano / 2} 0 0 1 ${tamano} ${tamano / 2} Z`} fill={color} opacity={opacidad} />
    </Svg>
  );
}

export function Zigzag({ ancho = 80, color = colores.lila, grosor = 4 }) {
  return (
    <Svg width={ancho} height={16} viewBox="0 0 80 16">
      <Path d="M2 12 L12 4 L22 12 L32 4 L42 12 L52 4 L62 12 L72 4 L78 9" stroke={color} strokeWidth={grosor} fill="none" strokeLinecap="round" strokeLinejoin="round" />
    </Svg>
  );
}

/** Capa de figuras para poner detrás de un encabezado de color. */
export function FigurasEncabezado({ variante = 0 }) {
  const conjuntos = [
    [
      { el: <Blob tamano={170} color={colores.blanco} opacidad={0.14} variante={0} />, pos: { top: -60, right: -50 }, flota: true },
      { el: <Anillo tamano={64} punteado opacidad={0.55} />, pos: { bottom: -36, left: -32 }, gira: true },
      { el: <Puntos filas={3} columnas={4} opacidad={0.5} />, pos: { bottom: 10, right: 20 } },
      { el: <Destello tamano={22} color={colores.sol} />, pos: { top: 8, right: 96 }, flota: true },
      { el: <Garabato ancho={70} opacidad={0.5} />, pos: { bottom: 4, left: 140 } },
    ],
    [
      { el: <Blob tamano={150} color={colores.blanco} opacidad={0.15} variante={1} />, pos: { top: -50, left: -40 }, flota: true },
      { el: <MedioCirculo tamano={80} color={colores.blanco} opacidad={0.18} />, pos: { bottom: 0, right: 30 } },
      { el: <Anillo tamano={48} grosor={5} opacidad={0.5} />, pos: { top: 14, right: 22 }, flota: true },
      { el: <Destello tamano={18} color={colores.blanco} />, pos: { top: 6, right: 100 }, flota: true },
      { el: <Zigzag ancho={64} color={colores.blanco} />, pos: { bottom: 4, right: 110 } },
    ],
    [
      { el: <Blob tamano={180} color={colores.blanco} opacidad={0.13} variante={2} />, pos: { bottom: -90, right: -60 }, flota: true },
      { el: <Anillo tamano={70} punteado grosor={5} opacidad={0.5} />, pos: { top: -10, right: 60 }, gira: true },
      { el: <Puntos filas={3} columnas={4} opacidad={0.45} />, pos: { bottom: 8, right: 110 } },
      { el: <Corazon tamano={22} color={colores.blanco} />, pos: { top: 14, right: 20 }, flota: true },
    ],
  ];
  const figuras = conjuntos[variante % conjuntos.length];
  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="none">
      {figuras.map((f, i) => {
        const Envoltura = f.gira ? Girando : f.flota ? Flotante : View;
        return (
          <Envoltura key={i} style={[styles.abs, f.pos]} retraso={i * 400}>
            {f.el}
          </Envoltura>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({ abs: { position: 'absolute' } });

