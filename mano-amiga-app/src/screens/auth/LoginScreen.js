import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Pantalla from '../../components/Pantalla';
import { Boton, Campo, MensajeError } from '../../components/ui';
import { Anillo, Blob, Destello, Flotante, Girando, Puntos } from '../../components/Decoraciones';
import { IlustracionBienvenida } from '../../components/Ilustraciones';
import { useAuth } from '../../hooks/useAuth';
import { mensajeDeError } from '../../api/client';
import { colores, espacio, fuentes, sombra } from '../../constants/tema';

export default function LoginScreen({ navigation }) {
  const { login } = useAuth();
  const insets = useSafeAreaInsets();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);

  const ingresar = async () => {
    if (!email || !password) return setError('Completá email y contraseña');
    setEnviando(true);
    setError(null);
    try {
      await login(email, password);
    } catch (e) {
      setError(mensajeDeError(e));
      setEnviando(false);
    }
  };

  return (
    <Pantalla decorada={false} estilo={{ paddingTop: insets.top }}>
      {/* Figuras de fondo */}
      <View style={StyleSheet.absoluteFill} pointerEvents="none">
        <Flotante style={styles.blob1} distancia={12} duracion={4000}>
          <Blob tamano={260} color={colores.coralSuave} variante={0} />
        </Flotante>
        <Flotante style={styles.blob2} distancia={10} duracion={5200} retraso={500}>
          <Blob tamano={220} color={colores.oceanoSuave} variante={2} />
        </Flotante>
        <Girando style={styles.anillo}>
          <Anillo tamano={80} color={colores.lila} grosor={5} punteado opacidad={0.6} />
        </Girando>
        <View style={styles.puntos}>
          <Puntos color={colores.menta} filas={4} columnas={4} opacidad={0.8} />
        </View>
      </View>

      <View style={styles.marca}>
        <Flotante distancia={8}>
          <IlustracionBienvenida tamano={200} />
        </Flotante>
        <View style={styles.tituloFila}>
          <Text style={styles.nombre}>Mano Amiga</Text>
          <Destello tamano={22} color={colores.sol} />
        </View>
        <Text style={styles.lema}>Sumate, ayudá y hacé que cada hora cuente 💛</Text>
      </View>

      <View style={styles.formulario}>
        <MensajeError texto={error} />
        <Campo etiqueta="Email" icono="✉️" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" autoComplete="email" placeholder="tu@email.com" />
        <Campo etiqueta="Contraseña" icono="🔒" value={password} onChangeText={setPassword} secureTextEntry autoComplete="password" placeholder="••••••••" />
        <Boton titulo="¡Entrar!" onPress={ingresar} cargando={enviando} />
        <Boton titulo="Quiero sumarme" icono="✨" variante="sol" onPress={() => navigation.navigate('Registro')} />
      </View>
    </Pantalla>
  );
}

const styles = StyleSheet.create({
  blob1: { position: 'absolute', top: -80, left: -90 },
  blob2: { position: 'absolute', bottom: -60, right: -80 },
  anillo: { position: 'absolute', top: 70, right: 24 },
  puntos: { position: 'absolute', top: 150, left: 22 },
  marca: { alignItems: 'center', marginTop: espacio.l, marginBottom: espacio.l },
  tituloFila: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: espacio.s },
  nombre: { fontFamily: fuentes.titulo, fontSize: 40, color: colores.tinta },
  lema: { fontFamily: fuentes.textoMedio, color: colores.tintaSuave, marginTop: espacio.xs, textAlign: 'center', fontSize: 15 },
  formulario: { backgroundColor: colores.blanco, borderRadius: 28, padding: espacio.l, ...sombra },
});
