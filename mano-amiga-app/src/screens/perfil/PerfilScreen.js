import { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Aparecer, Avatar, BarraProgreso, Boton, Cargando, MensajeError, Subtitulo, Tarjeta, TextoSuave } from '../../components/ui';
import { FigurasEncabezado, Flotante } from '../../components/Decoraciones';
import { IlustracionMedalla } from '../../components/Ilustraciones';
import { useCarga } from '../../hooks/useCarga';
import { useAuth } from '../../hooks/useAuth';
import { useTema } from '../../hooks/useTema';
import { actualizarPerfil, misHoras, obtenerPerfil } from '../../api/usuarios';
import { mensajeDeError } from '../../api/client';
import { colores, espacio, estilosDinamicos, fuentes } from '../../constants/tema';

const ROLES = {
  voluntario: { texto: 'Voluntario/a', emoji: '🙋' },
  coordinador: { texto: 'Coordinador/a', emoji: '🧭' },
  admin: { texto: 'Administrador/a', emoji: '🛠' },
};

const TEMAS = [
  { valor: 'sistema', texto: 'Automático', emoji: '📱' },
  { valor: 'claro', texto: 'Claro', emoji: '☀️' },
  { valor: 'oscuro', texto: 'Oscuro', emoji: '🌙' },
];

/**
 * Perfil (R8 / R16): datos de contacto, tema, contraseña, historial de accesos y horas judiciales.
 * Todo lo que se muestra es del propio usuario: la API usa el id del token, nunca uno de la URL (RS2).
 */
export default function PerfilScreen({ navigation }) {
  const { logout, actualizarUsuario } = useAuth();
  const { preferencia, setPreferenciaLocal, limpiarPreferenciaLocal } = useTema();
  const insets = useSafeAreaInsets();
  const [errorTema, setErrorTema] = useState(null);
  const { datos, cargando, error, recargar } = useCarga(async () => {
    const perfil = await obtenerPerfil();
    const horas = perfil.esJudicial ? await misHoras() : null;
    return { perfil, horas };
  }, []);

  // El tema se aplica al instante y después se guarda en el servidor (queda en el perfil).
  const elegirTema = async (tema) => {
    if (tema === preferencia) return;
    setErrorTema(null);
    setPreferenciaLocal(tema);
    try {
      const p = await actualizarPerfil({ preferencias: { tema } });
      actualizarUsuario({ preferencias: p.preferencias });
      limpiarPreferenciaLocal();
    } catch (e) {
      limpiarPreferenciaLocal(); // vuelve al tema que estaba guardado
      setErrorTema(mensajeDeError(e));
    }
  };

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
                <Boton titulo="Editar mis datos" icono="✏️" variante="borde" chico onPress={() => navigation.navigate('EditarPerfil', { perfil: p })} estilo={{ marginTop: espacio.s }} />
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
                      <TextoSuave>
                        {datos.horas.horasRestantes > 0 ? `¡Te faltan ${datos.horas.horasRestantes} horas!` : '¡Completaste tus horas! 🎉'}
                      </TextoSuave>
                    </View>
                  </View>
                  <BarraProgreso valor={datos.horas.horasCumplidas} total={datos.horas.horasAsignadas || 1} color={colores.lila} />
                  <TextoSuave estilo={{ marginTop: 10 }}>Solo cuentan las asistencias validadas por un coordinador.</TextoSuave>
                </Tarjeta>
              </Aparecer>
            ) : null}

            <Aparecer indice={2}>
              <Subtitulo>Apariencia</Subtitulo>
              <View style={styles.temas}>
                {TEMAS.map((t) => {
                  const activo = preferencia === t.valor;
                  return (
                    <Pressable
                      key={t.valor}
                      accessibilityRole="radio"
                      accessibilityState={{ selected: activo }}
                      onPress={() => elegirTema(t.valor)}
                      style={({ pressed }) => [styles.tema, activo && styles.temaActivo, pressed && { transform: [{ scale: 0.96 }] }]}
                    >
                      <Text style={styles.temaEmoji}>{t.emoji}</Text>
                      <Text style={[styles.temaTexto, activo && styles.temaTextoActivo]}>{t.texto}</Text>
                    </Pressable>
                  );
                })}
              </View>
              <MensajeError texto={errorTema} />
            </Aparecer>

            <Aparecer indice={3}>
              <Subtitulo>Seguridad</Subtitulo>
              <Tarjeta onPress={() => navigation.navigate('CambiarPassword')} acento={colores.coral}>
                <Opcion emoji="🔑" titulo="Cambiar contraseña" texto="Se cierran las sesiones en tus otros dispositivos." />
              </Tarjeta>
              <Tarjeta onPress={() => navigation.navigate('Accesos')} acento={colores.oceano}>
                <Opcion emoji="🕵️" titulo="Historial de accesos" texto="Revisá cuándo y desde dónde se usó tu cuenta." />
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

function Opcion({ emoji, titulo, texto }) {
  return (
    <View style={styles.fila}>
      <Text style={styles.filaEmoji}>{emoji}</Text>
      <View style={{ flex: 1 }}>
        <Text style={styles.opcionTitulo}>{titulo}</Text>
        <TextoSuave>{texto}</TextoSuave>
      </View>
      <Text style={styles.flecha}>›</Text>
    </View>
  );
}

const styles = estilosDinamicos(() => ({
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
  opcionTitulo: { fontFamily: fuentes.tituloMedio, fontSize: 16, color: colores.tinta },
  flecha: { fontFamily: fuentes.titulo, fontSize: 26, color: colores.gris },
  horasCabecera: { flexDirection: 'row', alignItems: 'center', gap: espacio.m, marginBottom: espacio.m },
  horasGrande: { fontFamily: fuentes.titulo, fontSize: 30, color: colores.lila },
  horasDe: { fontFamily: fuentes.textoMedio, fontSize: 16, color: colores.tintaSuave },
  temas: { flexDirection: 'row', gap: espacio.s, marginBottom: espacio.s },
  tema: { flex: 1, alignItems: 'center', paddingVertical: espacio.m, borderRadius: 20, backgroundColor: colores.tarjeta, borderWidth: 2, borderColor: colores.borde },
  temaActivo: { backgroundColor: colores.lilaSuave, borderColor: colores.lila },
  temaEmoji: { fontSize: 26, marginBottom: 4 },
  temaTexto: { fontFamily: fuentes.textoMedio, color: colores.tintaSuave },
  temaTextoActivo: { fontFamily: fuentes.negrita, color: colores.tinta },
}));
