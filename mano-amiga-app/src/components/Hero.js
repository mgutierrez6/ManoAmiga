import { StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { FigurasEncabezado, Flotante } from './Decoraciones';
import { colores, espacio, fuentes } from '../constants/tema';

export const DEGRADES = {
  coral: { colores: ['#EF476F', '#FF8A5B'], texto: colores.blanco },
  oceano: { colores: ['#118AB2', '#06D6A0'], texto: colores.blanco },
  lila: { colores: ['#9B5DE5', '#EF476F'], texto: colores.blanco },
  sol: { colores: ['#FFB703', '#FFD166'], texto: colores.tinta },
  menta: { colores: ['#06D6A0', '#118AB2'], texto: colores.blanco },
};

/**
 * Encabezado grande de color con degradé, figuras animadas y un emoji "sticker".
 * Se usa arriba de las pantallas principales (las pestañas). Con "conSafeArea" deja lugar para la barra de estado.
 */
export default function Hero({ titulo, subtitulo, emoji, tema = 'coral', figuras = 0, conSafeArea = true, children, compacto = false }) {
  const insets = useSafeAreaInsets();
  const d = DEGRADES[tema] || DEGRADES.coral;
  return (
    <LinearGradient
      colors={d.colores}
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 1 }}
      style={[styles.hero, { paddingTop: (conSafeArea ? insets.top : 0) + (compacto ? espacio.m : espacio.xl) }]}
    >
      <FigurasEncabezado variante={figuras} />
      <View style={styles.fila}>
        <View style={styles.textos}>
          <Text style={[styles.titulo, { color: d.texto }, compacto && { fontSize: 24 }]}>{titulo}</Text>
          {subtitulo ? <Text style={[styles.subtitulo, { color: d.texto }]}>{subtitulo}</Text> : null}
        </View>
        {emoji ? (
          <Flotante distancia={6}>
            <View style={styles.sticker}>
              <Text style={styles.stickerEmoji}>{emoji}</Text>
            </View>
          </Flotante>
        ) : null}
      </View>
      {children ? <View style={styles.extra}>{children}</View> : null}
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  hero: {
    paddingHorizontal: espacio.l,
    paddingBottom: espacio.xl,
    borderBottomLeftRadius: 34,
    borderBottomRightRadius: 34,
    overflow: 'hidden',
  },
  fila: { flexDirection: 'row', alignItems: 'center' },
  textos: { flex: 1, paddingRight: espacio.m },
  titulo: { fontFamily: fuentes.titulo, fontSize: 30, lineHeight: 36 },
  subtitulo: { fontFamily: fuentes.textoMedio, fontSize: 15, opacity: 0.95, marginTop: 4 },
  sticker: {
    width: 64,
    height: 64,
    borderRadius: 22,
    backgroundColor: 'rgba(255,255,255,0.92)',
    alignItems: 'center',
    justifyContent: 'center',
    transform: [{ rotate: '8deg' }],
  },
  stickerEmoji: { fontSize: 34 },
  extra: { marginTop: espacio.l },
});
