import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Aparecer, Avatar, BarraProgreso, Boton, Cargando, MensajeError, Subtitulo, Tarjeta, TextoSuave } from '../../components/ui';
import { FigurasEncabezado, Flotante } from '../../components/Decoraciones';
import { IlustracionMedalla } from '../../components/Ilustraciones';
import { useCarga } from '../../hooks/useCarga';
import { useAuth } from '../../hooks/useAuth';
import { misHoras, obtenerPerfil } from '../../api/usuarios';
import { colores, espacio, fuentes } from '../../constants/tema';

const ROLES = {
  voluntario: { texto: 'Voluntario/a', emoji: '🙋' },
  coordinador: { texto: 'Coordinador/a', emoji: '🧭' },
  admin: { texto: 'Administrador/a', emoji: '🛠' },
};

/**
 * Perfil básico (Sprint 1). En el Sprint 2 (R8) se agregan: editar datos de contacto,
 * cambiar contraseña, modo oscuro e historial de inicios de sesión.
 */
export default function PerfilScreen() {
  const { logout } = useAuth();
  const insets = useSafeAreaInsets();
  const { datos, cargando, error, recargar } = useCarga(async () => {
    const perfil = await obtenerPerfil();
    const horas = perfil.esJudicial ? await misHoras() : null;
    return { perfil, horas };
  }, []);

  if (cargando && !datos) return <Cargando />;
  const p = datos?.perfil;
  const rol = ROLES[p?.rol] || ROLES.voluntario;

  return (
    <ScrollView style={styles.contenedor} contentContainerStyle={styles.contenido}>
      <LinearGradient colors={['#9B5DE5', '#EF476F']} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={[styles.hero, { paddingTop: insets.top + espacio.xl }]}>
        <FigurasEncabezado variante={2} />
        {p ? (
          <View style={styles.heroCentro}>
            <Flotante distancia={6}>
              <View style={styles.avatarAro}>
                <Avatar nombre={p.nombre} apellido={p.apellido} tamano={92} />
              </View>
            </Flotante>
            <Text style={styles.nombre}>
              {p.nombre} {p.apellido}
            </Text>
            <View style={styles.rol}>
              <Text style={styles.rolTexto}>
                {rol.emoji} {rol.texto}
              </Text>
            </View>
          </View>
        ) : null}
      </LinearGradient>

      <View style={styles.cuerpo}>
        <MensajeError texto={error} onReintentar={recargar} />
        {p ? (
          <>
            <Aparecer>
              <Tarjeta>
                <Fila emoji="✉️" etiqueta="Email" valor={p.email} />
                <Fila emoji="📱" etiqueta="Teléfono" valor={p.telefono || 'Sin teléfono'} />
              </Tarjeta>
            </Aparecer>

            {datos.horas ? (
              <Aparecer indice={1}>
                <Subtitulo>Horas de trabajo comunitario</Subtitulo>
                <Tarjeta>
                  <View style={styles.horasCabecera}>
                    <IlustracionMedalla tamano={56} />
                    <View style={{ flex: 1 }}>
                      <Text style={styles.horasGrande}>
                        {datos.horas.horasCumplidas} <Text style={styles.horasDe}>de {datos.horas.horasAsignadas} h</Text>
                      </Text>
                      <TextoSuave>¡Te faltan {datos.horas.horasRestantes} horas!</TextoSuave>
                    </View>
                  </View>
                  <BarraProgreso valor={datos.horas.horasCumplidas} total={datos.horas.horasAsignadas || 1} color={colores.lila} />
                  <TextoSuave estilo={{ marginTop: 10 }}>Solo cuentan las asistencias validadas por un coordinador.</TextoSuave>
                </Tarjeta>
              </Aparecer>
            ) : null}

            <Aparecer indice={2}>
              <Tarjeta estilo={styles.proximamente}>
                <Text style={styles.proxTitulo}>✨ Muy pronto</Text>
                <TextoSuave>Editar tus datos, cambiar la contraseña, modo oscuro e historial de accesos.</TextoSuave>
              </Tarjeta>
            </Aparecer>
          </>
        ) : null}
        <Boton titulo="Cerrar sesión" icono="👋" variante="borde" onPress={logout} estilo={{ marginTop: espacio.m }} />
      </View>
    </ScrollView>
  );
}

function Fila({ emoji, etiqueta, valor }) {
  return (
    <View style={styles.fila}>
      <Text style={styles.filaEmoji}>{emoji}</Text>
      <View style={{ flex: 1 }}>
        <Text style={styles.filaEtiqueta}>{etiqueta}</Text>
        <Text style={styles.filaValor}>{valor}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  contenedor: { flex: 1, backgroundColor: colores.crema },
  contenido: { paddingBottom: espacio.xxl },
  hero: { paddingBottom: espacio.xl, borderBottomLeftRadius: 36, borderBottomRightRadius: 36, overflow: 'hidden' },
  heroCentro: { alignItems: 'center' },
  avatarAro: { padding: 5, borderRadius: 60, backgroundColor: 'rgba(255,255,255,0.9)' },
  nombre: { fontFamily: fuentes.titulo, fontSize: 26, color: colores.blanco, marginTop: espacio.m },
  rol: { backgroundColor: 'rgba(255,255,255,0.25)', paddingHorizontal: 14, paddingVertical: 5, borderRadius: 999, marginTop: 6 },
  rolTexto: { fontFamily: fuentes.negrita, color: colores.blanco },
  cuerpo: { padding: espacio.l },
  fila: { flexDirection: 'row', alignItems: 'center', gap: espacio.m, paddingVertical: 8 },
  filaEmoji: { fontSize: 22 },
  filaEtiqueta: { fontFamily: fuentes.negrita, fontSize: 12, color: colores.gris },
  filaValor: { fontFamily: fuentes.textoMedio, fontSize: 15, color: colores.tinta },
  horasCabecera: { flexDirection: 'row', alignItems: 'center', gap: espacio.m, marginBottom: espacio.m },
  horasGrande: { fontFamily: fuentes.titulo, fontSize: 30, color: colores.lila },
  horasDe: { fontFamily: fuentes.textoMedio, fontSize: 16, color: colores.tintaSuave },
  proximamente: { backgroundColor: colores.solSuave },
  proxTitulo: { fontFamily: fuentes.tituloMedio, fontSize: 17, color: colores.tinta, marginBottom: 4 },
});
