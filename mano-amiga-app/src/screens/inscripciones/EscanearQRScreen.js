import { useRef, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { Boton, Cargando, TextoSuave } from '../../components/ui';
import { Anillo, Destello, Flotante, Girando } from '../../components/Decoraciones';
import { IlustracionMedalla } from '../../components/Ilustraciones';
import { registrarAsistenciaQR } from '../../api/asistencias';
import { mensajeDeError } from '../../api/client';
import { colores, espacio, fuentes } from '../../constants/tema';

/**
 * El voluntario escanea el QR que muestra el coordinador (2.3).
 * El QR es un código firmado por el servidor que vence a los 60 s: la app solo lo reenvía,
 * la validación la hace la API.
 */
export default function EscanearQRScreen({ route, navigation }) {
  const { actividadId, titulo } = route.params;
  const [permiso, pedirPermiso] = useCameraPermissions();
  const [estado, setEstado] = useState('escaneando'); // escaneando | enviando | ok | error
  const [mensaje, setMensaje] = useState('');
  const bloqueado = useRef(false); // evita mandar el mismo QR varias veces

  const alEscanear = async ({ data }) => {
    if (bloqueado.current) return;
    bloqueado.current = true;
    setEstado('enviando');
    try {
      await registrarAsistenciaQR(actividadId, data);
      setEstado('ok');
      setMensaje('El coordinador la va a validar al terminar la actividad.');
    } catch (e) {
      setEstado('error');
      setMensaje(mensajeDeError(e));
    }
  };

  const reintentar = () => {
    bloqueado.current = false;
    setEstado('escaneando');
    setMensaje('');
  };

  if (!permiso) return <Cargando />;
  if (!permiso.granted) {
    return (
      <View style={styles.centro}>
        <Text style={styles.grande}>📷</Text>
        <Text style={styles.texto}>Necesitamos la cámara para escanear el código QR de asistencia.</Text>
        <Boton titulo="Permitir cámara" onPress={pedirPermiso} />
      </View>
    );
  }

  if (estado === 'ok') {
    return (
      <View style={styles.centro}>
        <Flotante distancia={10} style={{ alignSelf: 'center' }}>
          <IlustracionMedalla tamano={110} />
        </Flotante>
        <View style={styles.destellos}>
          <Destello color={colores.sol} />
          <Destello color={colores.coral} tamano={18} />
          <Destello color={colores.menta} />
        </View>
        <Text style={styles.exito}>¡Asistencia registrada!</Text>
        <Text style={styles.texto}>{mensaje}</Text>
        <Boton titulo="Volver" variante="secundario" onPress={() => navigation.goBack()} />
      </View>
    );
  }

  return (
    <View style={styles.contenedor}>
      <Text style={styles.titulo}>{titulo}</Text>
      <View style={styles.marcoExterior}>
        <Girando style={styles.anillo}>
          <Anillo tamano={320} color={colores.coral} grosor={6} punteado />
        </Girando>
        <View style={styles.camaraMarco}>
          {estado !== 'error' ? (
            <CameraView
              style={StyleSheet.absoluteFill}
              facing="back"
              barcodeScannerSettings={{ barcodeTypes: ['qr'] }}
              onBarcodeScanned={estado === 'escaneando' ? alEscanear : undefined}
            />
          ) : null}
        </View>
      </View>
      {estado === 'enviando' ? <TextoSuave estilo={styles.centrado}>Registrando… ✨</TextoSuave> : null}
      {estado === 'escaneando' ? <TextoSuave estilo={styles.centrado}>Apuntá al código QR que muestra el coordinador 🎯</TextoSuave> : null}
      {estado === 'error' ? (
        <View style={styles.resultado}>
          <Text style={[styles.texto, { color: colores.peligro }]}>😕 {mensaje}</Text>
          <Boton titulo="Escanear de nuevo" onPress={reintentar} />
          <Boton titulo="Volver" variante="borde" onPress={() => navigation.goBack()} />
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  contenedor: { flex: 1, backgroundColor: colores.crema, padding: espacio.l, alignItems: 'center' },
  titulo: { fontFamily: fuentes.titulo, fontSize: 22, marginBottom: espacio.l, color: colores.tinta, textAlign: 'center' },
  marcoExterior: { width: 320, height: 320, alignItems: 'center', justifyContent: 'center' },
  anillo: { position: 'absolute' },
  camaraMarco: { width: 272, height: 272, borderRadius: 40, overflow: 'hidden', backgroundColor: colores.tinta },
  centrado: { textAlign: 'center', marginTop: espacio.l },
  centro: { flex: 1, justifyContent: 'center', alignItems: 'stretch', padding: espacio.xl, backgroundColor: colores.crema },
  grande: { fontSize: 64, textAlign: 'center', marginBottom: espacio.m },
  destellos: { flexDirection: 'row', justifyContent: 'center', gap: 16, marginVertical: espacio.s },
  exito: { fontFamily: fuentes.titulo, fontSize: 28, color: colores.mentaOscuro, textAlign: 'center', marginBottom: espacio.s },
  texto: { fontFamily: fuentes.textoMedio, fontSize: 16, textAlign: 'center', marginBottom: espacio.m, color: colores.tinta },
  resultado: { marginTop: espacio.l, alignSelf: 'stretch' },
});
