/**
 * DTOs de respuesta (5.5): cada respuesta incluye SOLO los campos que quien consulta puede ver.
 * Nunca se devuelve un documento de Mongoose "tal cual".
 */
const id = (v) => (v ? String(v._id || v) : null);

function usuarioPropio(u) {
  return {
    id: id(u),
    nombre: u.nombre,
    apellido: u.apellido,
    email: u.email,
    telefono: u.telefono || '',
    rol: u.rol,
    organizacionId: id(u.organizacionId),
    esJudicial: Boolean(u.esJudicial),
    horasAsignadas: u.esJudicial ? u.horasAsignadas || 0 : undefined,
    preferencias: u.preferencias || { tema: 'sistema' },
  };
}

// Lo mínimo para la sesión en la app.
function usuarioSesion(u) {
  return { id: id(u), nombre: u.nombre, rol: u.rol, organizacionId: id(u.organizacionId), esJudicial: Boolean(u.esJudicial) };
}

// Voluntario visto por un coordinador DE SU organización (RS6 permite teléfono, apellido y condición judicial).
function voluntarioParaCoordinador(u) {
  if (!u) return null;
  return {
    id: id(u),
    nombre: u.nombre,
    apellido: u.apellido,
    telefono: u.telefono || '',
    esJudicial: Boolean(u.esJudicial),
  };
}

function etiqueta(e) {
  return { id: id(e), nombre: e.nombre, sensible: Boolean(e.sensible) };
}

function actividad(a) {
  const org = a.organizacionId && a.organizacionId.nombre ? { id: id(a.organizacionId), nombre: a.organizacionId.nombre } : { id: id(a.organizacionId) };
  return {
    id: id(a),
    titulo: a.titulo,
    descripcion: a.descripcion,
    organizacion: org,
    organizacionId: org.id,
    etiquetas: (a.etiquetas || []).map((e) => (e && e.nombre ? { id: id(e), nombre: e.nombre } : { id: id(e) })),
    ubicacion: a.ubicacion ? { lat: a.ubicacion.coordinates[1], lng: a.ubicacion.coordinates[0] } : null,
    direccion: a.direccion || '',
    fechaInicio: a.fechaInicio,
    fechaFin: a.fechaFin,
    cupo: a.cupo,
    inscriptos: a.inscriptos,
    cuposLibres: Math.max(0, a.cupo - a.inscriptos),
    estado: a.estado,
  };
}

function actividadResumen(a) {
  if (!a || !a.titulo) return { id: id(a) };
  return { id: id(a), titulo: a.titulo, fechaInicio: a.fechaInicio, fechaFin: a.fechaFin, direccion: a.direccion || '', estado: a.estado };
}

// Inscripción vista por el propio voluntario.
function inscripcionPropia(i) {
  return {
    id: id(i),
    actividadId: id(i.actividadId),
    actividad: actividadResumen(i.actividadId),
    estado: i.estado,
    fecha: i.fecha,
  };
}

// Inscripción vista por el coordinador de la organización.
function inscripcionParaCoordinador(i) {
  return {
    id: id(i),
    actividadId: id(i.actividadId),
    estado: i.estado,
    fecha: i.fecha,
    voluntario: voluntarioParaCoordinador(i.voluntarioId),
  };
}

function asistencia(a) {
  const v = a.voluntarioId && a.voluntarioId.nombre ? { id: id(a.voluntarioId), nombre: a.voluntarioId.nombre, apellido: a.voluntarioId.apellido } : { id: id(a.voluntarioId) };
  return {
    id: id(a),
    inscripcionId: id(a.inscripcionId),
    actividadId: id(a.actividadId),
    voluntario: v,
    checkIn: a.checkIn,
    checkOut: a.checkOut || null,
    horas: a.horas,
    metodo: a.metodo,
    estado: a.estado,
    validadaEn: a.validadaEn || null,
  };
}

function campana(c) {
  return {
    id: id(c),
    titulo: c.titulo,
    descripcion: c.descripcion,
    objetivo: c.objetivo || '',
    organizacion: c.organizacionId && c.organizacionId.nombre ? { id: id(c.organizacionId), nombre: c.organizacionId.nombre } : { id: id(c.organizacionId) },
    puntos: (c.puntos || []).map((p) => ({
      id: id(p),
      nombre: p.nombre,
      direccion: p.direccion || '',
      ubicacion: { lat: p.ubicacion.coordinates[1], lng: p.ubicacion.coordinates[0] },
    })),
    fechaInicio: c.fechaInicio,
    fechaFin: c.fechaFin,
    estado: c.estado,
  };
}

// Publicación pública: el autor se muestra solo con su nombre (sin apellido ni contacto).
function publicacion(p) {
  return {
    id: id(p),
    proyectoId: id(p.proyectoId),
    proyectoTipo: p.proyectoTipo,
    tipo: p.tipo,
    contenido: p.contenido,
    autor: p.autorId && p.autorId.nombre ? { nombre: p.autorId.nombre } : null,
    fecha: p.fecha,
  };
}

module.exports = {
  usuarioPropio,
  usuarioSesion,
  voluntarioParaCoordinador,
  etiqueta,
  actividad,
  inscripcionPropia,
  inscripcionParaCoordinador,
  asistencia,
  campana,
  publicacion,
};
