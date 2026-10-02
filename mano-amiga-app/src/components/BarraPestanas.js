import { useEffect, useRef } from 'react';
import { Animated, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colores, espacio, fuentes, sombra } from '../constants/tema';

// Color de la "burbuja" de cada pestaña activa (en orden).
const TONOS = [colores.coral, colores.sol, colores.menta, colores.lila, colores.oceano];

/** Barra de pestañas flotante: la pestaña activa se agranda en una burbuja de color con su nombre. */
export default function BarraPestanas({ state, descriptors, navigation }) {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.contenedor, { paddingBottom: Math.max(insets.bottom, espacio.s) }]}>
      <View style={styles.barra}>
        {state.routes.map((route, i) => {
          const { options } = descriptors[route.key];
          const activa = state.index === i;
          const etiqueta = options.title ?? route.name;
          const alTocar = () => {
            const evento = navigation.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
            if (!activa && !evento.defaultPrevented) navigation.navigate(route.name);
          };
          return (
            <Pestana
              key={route.key}
              activa={activa}
              emoji={options.tabBarEmoji || '•'}
              etiqueta={etiqueta}
              tono={TONOS[i % TONOS.length]}
              onPress={alTocar}
            />
          );
        })}
      </View>
    </View>
  );
}

function Pestana({ activa, emoji, etiqueta, tono, onPress }) {
  const v = useRef(new Animated.Value(activa ? 1 : 0)).current;
  useEffect(() => {
    Animated.spring(v, { toValue: activa ? 1 : 0, useNativeDriver: false, speed: 18, bounciness: 9 }).start();
  }, [activa, v]);

  const fondo = v.interpolate({ inputRange: [0, 1], outputRange: ['rgba(0,0,0,0)', tono] });
  const escala = v.interpolate({ inputRange: [0, 1], outputRange: [1, 1.12] });
  const textoOscuro = tono === colores.sol || tono === colores.menta;

  return (
    <Pressable accessibilityRole="tab" accessibilityState={{ selected: activa }} accessibilityLabel={etiqueta} onPress={onPress} style={[styles.pestana, activa && styles.pestanaActiva]}>
      <Animated.View style={[styles.burbuja, { backgroundColor: fondo }]}>
        <Animated.Text style={[styles.emoji, { transform: [{ scale: escala }] }]}>{emoji}</Animated.Text>
        {activa ? (
          <Text numberOfLines={1} style={[styles.etiqueta, { color: textoOscuro ? colores.tinta : colores.blanco }]}>
            {etiqueta}
          </Text>
        ) : null}
      </Animated.View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  contenedor: { backgroundColor: colores.crema, paddingHorizontal: espacio.m, paddingTop: espacio.s },
  barra: {
    flexDirection: 'row',
    backgroundColor: colores.blanco,
    borderRadius: 999,
    padding: 6,
    ...sombra,
    shadowOpacity: 0.12,
  },
  pestana: { flex: 1, alignItems: 'center' },
  pestanaActiva: { flex: 2.2 },
  burbuja: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', borderRadius: 999, paddingVertical: 10, paddingHorizontal: 12, alignSelf: 'stretch' },
  emoji: { fontSize: 20 },
  etiqueta: { fontFamily: fuentes.negrita, fontSize: 13, marginLeft: 6, flexShrink: 1 },
});
