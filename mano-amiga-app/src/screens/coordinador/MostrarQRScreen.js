import { useCallback, useEffect, useRef, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import QRCode from 'react-native-qrcode-svg';
import { BarraProgreso, Cargando, MensajeError, TextoSuave } from '../../components/ui';
import { Anillo, Blob, Destello, Flotante, Girando } from '../../components/Decoraciones';
import { obtenerQR } from '../../api/asistencias';
import { mensajeDeError } from '../../api/client';
import { colores, espacio, fuentes, sombra } from '../../constants/tema';

const RENOVAR_CADA_MS = 50 * 1000; // el QR vence a los 60 s: lo renovamos un poco antes

/** El coordinador muestra este QR y los voluntarios lo escanean para marcar la entrada (2.3). */
export default function MostrarQRScreen({ route }) {
  const { id, titulo } = route.params;
  const [qr, setQr] = useState(null);
  const [error, setError] = useState(null);
  const [segundos, setSegundos] = useState(0);
  const vence = useRef(0);

  const renovar = useCallback(async () => {
    try {
      const r = await obtenerQR(id);
      setQr(r.qr);
      vence.current = new Date(r.expiraEn).getTime();
      setError(null);
    } catch (e) {
      setError(mensajeDeError(e));
    }
  }, [id]);

  useEffect(() => {
    renovar();
    const renovacion = setInterval(renovar, RENOVAR_CADA_MS);
    const reloj = setInterval(() => setSegundos(Math.max(0, Math.round((vence.current - Date.now()) / 1000))), 1000);
    return () => {
      clearInterval(renovacion);
      clearInterval(reloj);
    };
  }, [renovar]);

  return (
    <View style={styles.contenedor}>
      <View style={StyleSheet.absoluteFill} pointerEvents="none">
        <Flotante style={{ position: 'absolute', top: -40, left: -60 }}>
          <Blob tamano={200} color={colores.lilaSuave} />
        </Flotante>
        <Flotante style={{ position: 'absolute', bottom: -50, right: -60 }} retraso={500}>
          <Blob tamano={220} color={colores.solSuave} variante={2} />
        </Flotante>
      </View>

      <Text style={styles.titulo}>{titulo}</Text>
      <TextoSuave estilo={styles.centrado}>¡Mostrá este código a los voluntarios! 🙌</TextoSuave>
      <MensajeError texto={error} onReintentar={renovar} />
      {!qr && !error ? <Cargando /> : null}

      {qr ? (
        <View style={styles.marcoExterior}>
          <Girando style={styles.anillo} duracion={24000}>
            <Anillo tamano={330} color={colores.coral} grosor={6} punteado />
          </Girando>
          <View style={[styles.destello, { top: 6, right: 20 }]}>
            <Destello color={colores.sol} tamano={30} />
          </View>
          <View style={[styles.destello, { bottom: 10, left: 16 }]}>
            <Destello color={colores.menta} tamano={24} />
          </View>
          <View style={styles.qr}>
            <QRCode value={qr} size={230} color={colores.tinta} />
          </View>
        </View>
      ) : null}

      {qr ? (
        <View style={styles.contador}>
          <Text style={styles.contadorTexto}>🔄 Se renueva solo · vence en {segundos} s</Text>
          <BarraProgreso valor={segundos} total={60} color={segundos > 15 ? colores.menta : colores.coral} />
        </View>
      ) : null}
      <TextoSuave estilo={styles.centrado}>
        Los voluntarios aceptados lo escanean desde "Inscripciones". Al terminar, validá cada asistencia en la pantalla anterior.
      </TextoSuave>
    </View>
  );
}

const styles = StyleSheet.create({
  contenedor: { flex: 1, backgroundColor: colores.crema, padding: espacio.l, alignItems: 'center', overflow: 'hidden' },
  titulo: { fontFamily: fuentes.titulo, fontSize: 24, textAlign: 'center', color: colores.tinta },
  marcoExterior: { width: 330, height: 330, alignItems: 'center', justifyContent: 'center', marginVertical: espacio.m },
  anillo: { position: 'absolute' },
  destello: { position: 'absolute' },
  qr: { padding: espacio.l, backgroundColor: colores.blanco, borderRadius: 32, ...sombra },
  contador: { alignSelf: 'stretch', marginBottom: espacio.m },
  contadorTexto: { fontFamily: fuentes.negrita, color: colores.tinta, textAlign: 'center', marginBottom: 8 },
  centrado: { textAlign: 'center', marginTop: espacio.s },
});
