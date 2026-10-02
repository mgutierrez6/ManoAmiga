/**
 * Carga datos iniciales para desarrollo:  npm run seed
 *  - Catálogo de etiquetas (con cuáles son sensibles para la regla ABAC)
 *  - Un administrador, una organización de prueba y un coordinador
 *  - (Opcional) un voluntario judicial para probar ABAC
 *
 * Las contraseñas NO están en el código: se leen del .env (SEED_*). Es idempotente: se puede correr varias veces.
 * En el Sprint 2 los coordinadores y la condición judicial se van a gestionar desde el panel de admin (R7).
 */
const bcrypt = require('bcryptjs');
const config = require('../config/env');
const db = require('../config/db');
const { Etiqueta, Organizacion, Usuario } = require('../models');

const ETIQUETAS = [
  { nombre: 'Ollas populares', sensible: false },
  { nombre: 'Medioambiente', sensible: false },
  { nombre: 'Urgente', sensible: false },
  { nombre: 'Donaciones', sensible: false },
  { nombre: 'Adultos mayores', sensible: false },
  { nombre: 'Apto programa judicial', sensible: false },
  { nombre: 'Apoyo escolar', sensible: true },
  { nombre: 'Contacto con menores', sensible: true },
  { nombre: 'Hogares de niños', sensible: true },
  { nombre: 'Oratorios', sensible: true },
];

function env(nombre, obligatoria = true) {
  const v = process.env[nombre];
  if (!v && obligatoria) throw new Error(`Falta ${nombre} en el .env para correr el seed`);
  return v;
}

async function upsertUsuario({ email, password, nombre, apellido, rol, organizacionId = null, esJudicial = false, horasAsignadas = 0 }) {
  if (password.length < 8) throw new Error(`La contraseña de ${email} debe tener al menos 8 caracteres`);
  const passwordHash = await bcrypt.hash(password, config.bcryptCosto);
  await Usuario.updateOne(
    { email: email.toLowerCase() },
    { $set: { nombre, apellido, passwordHash, rol, organizacionId, esJudicial, horasAsignadas, activo: true } },
    { upsert: true }
  );
  console.log(`  ✔ ${rol}${esJudicial ? ' (judicial)' : ''}: ${email}`);
}

async function main() {
  await db.conectar(config.mongoUri);
  console.log('Creando índices...');
  for (const modelo of Object.values(require('../models'))) await modelo.syncIndexes();

  console.log('Etiquetas:');
  for (const e of ETIQUETAS) {
    await Etiqueta.updateOne({ nombre: e.nombre }, { $set: e }, { upsert: true });
    console.log(`  ✔ ${e.nombre}${e.sensible ? '  (sensible)' : ''}`);
  }

  console.log('Organización:');
  const org = await Organizacion.findOneAndUpdate(
    { nombre: 'Organización Demo' },
    { $set: { contacto: 'contacto@demo.org', convenioOSLA: true, activa: true } },
    { upsert: true, returnDocument: 'after' }
  );
  console.log(`  ✔ ${org.nombre}`);

  console.log('Usuarios:');
  await upsertUsuario({ email: env('SEED_ADMIN_EMAIL'), password: env('SEED_ADMIN_PASSWORD'), nombre: 'Admin', apellido: 'Mano Amiga', rol: 'admin' });
  await upsertUsuario({
    email: env('SEED_COORD_EMAIL'),
    password: env('SEED_COORD_PASSWORD'),
    nombre: 'Coordinadora',
    apellido: 'Demo',
    rol: 'coordinador',
    organizacionId: org._id,
  });
  if (env('SEED_JUDICIAL_EMAIL', false)) {
    await upsertUsuario({
      email: env('SEED_JUDICIAL_EMAIL'),
      password: env('SEED_JUDICIAL_PASSWORD'),
      nombre: 'Voluntario',
      apellido: 'Judicial',
      rol: 'voluntario',
      esJudicial: true,
      horasAsignadas: 60,
    });
  }

  await db.desconectar();
  console.log('Listo.');
}

main().catch(async (err) => {
  console.error('Error en el seed:', err.message);
  await db.desconectar().catch(() => {});
  process.exit(1);
});
