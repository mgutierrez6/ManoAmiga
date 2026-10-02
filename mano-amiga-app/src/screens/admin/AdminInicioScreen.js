import { StyleSheet, Text, View } from 'react-native';
import Hero from '../../components/Hero';
import Pantalla from '../../components/Pantalla';
import { Aparecer, Tarjeta, TextoSuave } from '../../components/ui';
import { colores, fuentes } from '../../constants/tema';

const PROXIMAS = [
  { emoji: '🏢', titulo: 'Organizaciones', texto: 'Dar de alta y de baja organizaciones.', color: colores.coralSuave },
  { emoji: '🧭', titulo: 'Coordinadores', texto: 'Asignar y quitar coordinadores.', color: colores.solSuave },
  { emoji: '⚖️', titulo: 'Programa judicial', texto: 'Marcar voluntarios judiciales y sus horas.', color: colores.mentaSuave },
  { emoji: '⏱', titulo: 'Horas cumplidas', texto: 'Consultar el avance de cada persona.', color: colores.lilaSuave },
];

/** Panel de administración: se implementa en el Sprint 2 (R7 + RS5 + RS9). */
export default function AdminInicioScreen() {
  return (
    <Pantalla cabecera={<Hero titulo="Administración" subtitulo="Llega en el próximo sprint 🚧" emoji="🛠" tema="oceano" figuras={2} />}>
      {PROXIMAS.map((p, i) => (
        <Aparecer key={p.titulo} indice={i}>
          <Tarjeta estilo={{ backgroundColor: p.color }}>
            <View style={styles.fila}>
              <Text style={styles.emoji}>{p.emoji}</Text>
              <View style={{ flex: 1 }}>
                <Text style={styles.titulo}>{p.titulo}</Text>
                <TextoSuave>{p.texto}</TextoSuave>
              </View>
            </View>
          </Tarjeta>
        </Aparecer>
      ))}
      <TextoSuave>Cada acción va a pedir reautenticación y va a quedar registrada en la auditoría (RS9).</TextoSuave>
    </Pantalla>
  );
}

const styles = StyleSheet.create({
  fila: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  emoji: { fontSize: 30 },
  titulo: { fontFamily: fuentes.tituloMedio, fontSize: 18, color: colores.tinta },
});
