import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { colores, espacio } from '../constants/tema';
import { Blob, Flotante } from './Decoraciones';

/**
 * Contenedor estándar de pantallas con formulario o contenido que scrollea.
 * "decorada" agrega un par de figuras suaves en las esquinas del fondo.
 */
export default function Pantalla({ children, scroll = true, estilo, decorada = true, cabecera }) {
  const contenido = scroll ? (
    <ScrollView contentContainerStyle={[styles.contenido, estilo]} keyboardShouldPersistTaps="handled">
      {cabecera}
      <View style={styles.cuerpo}>{children}</View>
    </ScrollView>
  ) : (
    <View style={[styles.flex, estilo]}>
      {cabecera}
      <View style={[styles.cuerpo, styles.flex]}>{children}</View>
    </View>
  );
  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={[styles.flex, { backgroundColor: colores.crema }]}>
        {decorada ? (
          <View style={StyleSheet.absoluteFill} pointerEvents="none">
            <Flotante style={styles.blobArriba} distancia={10} duracion={4200}>
              <Blob tamano={180} color={colores.solSuave} variante={1} />
            </Flotante>
            <Flotante style={styles.blobAbajo} distancia={10} duracion={5000} retraso={600}>
              <Blob tamano={200} color={colores.mentaSuave} variante={2} />
            </Flotante>
          </View>
        ) : null}
        {contenido}
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  contenido: { flexGrow: 1, paddingBottom: espacio.xxl },
  cuerpo: { padding: espacio.l },
  blobArriba: { position: 'absolute', top: -60, right: -70 },
  blobAbajo: { position: 'absolute', bottom: -80, left: -80 },
});
