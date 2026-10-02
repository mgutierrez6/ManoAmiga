const { app, request, conectar, desconectar, escenario, crearActividad, crearUsuario, bearer, models } = require('./helpers');

let s;
beforeAll(async () => {
  await conectar();
  s = await escenario();
});
afterAll(desconectar);

const inscribir = (vol, actId) => request(app).post(`/api/v1/actividades/${actId}/inscripciones`).set(bearer(vol));

describe('Inscripción (R3)', () => {
  test('inscripción ok -> 201 pendiente y suma un inscripto', async () => {
    const act = await crearActividad(s.coordA);
    const res = await inscribir(s.vol1, act.id);
    expect(res.status).toBe(201);
    expect(res.body.estado).toBe('pendiente');
    const a = await models.Actividad.findById(act.id).lean();
    expect(a.inscriptos).toBe(1);
  });

  test('inscribirse dos veces -> 409', async () => {
    const act = await crearActividad(s.coordA);
    await inscribir(s.vol1, act.id);
    const res = await inscribir(s.vol1, act.id);
    expect(res.status).toBe(409);
  });

  test('cupo lleno -> 409 CUPO_LLENO', async () => {
    const act = await crearActividad(s.coordA, { cupo: 1 });
    expect((await inscribir(s.vol1, act.id)).status).toBe(201);
    const res = await inscribir(s.vol2, act.id);
    expect(res.status).toBe(409);
    expect(res.body.error.code).toBe('CUPO_LLENO');
  });

  test('10 pedidos simultáneos por el último cupo: solo 1 entra', async () => {
    const act = await crearActividad(s.coordA, { cupo: 1 });
    const vols = await Promise.all(Array.from({ length: 10 }, () => crearUsuario()));
    const resultados = await Promise.all(vols.map((v) => inscribir(v, act.id)));
    expect(resultados.filter((r) => r.status === 201)).toHaveLength(1);
    const a = await models.Actividad.findById(act.id).lean();
    expect(a.inscriptos).toBe(1);
  });

  test('un coordinador no puede inscribirse (rol)', async () => {
    const act = await crearActividad(s.coordA);
    expect((await inscribir(s.coordB, act.id)).status).toBe(403);
  });
});

describe('RS2 · un voluntario no accede a inscripciones ajenas (A1 · IDOR)', () => {
  let insVol1;
  beforeAll(async () => {
    const act = await crearActividad(s.coordA);
    insVol1 = (await inscribir(s.vol1, act.id)).body;
  });

  test('vol2 NO puede cancelar la inscripción de vol1', async () => {
    const res = await request(app).delete(`/api/v1/inscripciones/${insVol1.id}`).set(bearer(s.vol2));
    expect(res.status).toBe(403);
    const ins = await models.Inscripcion.findById(insVol1.id).lean();
    expect(ins.estado).toBe('pendiente');
  });

  test('vol2 NO puede descargar el certificado de vol1', async () => {
    const res = await request(app).get(`/api/v1/inscripciones/${insVol1.id}/certificado`).set(bearer(s.vol2));
    expect(res.status).toBe(403);
  });

  test('"mis inscripciones" solo trae las propias', async () => {
    const res = await request(app).get('/api/v1/usuarios/me/inscripciones').set(bearer(s.vol2));
    expect(res.status).toBe(200);
    expect(res.body.data.map((i) => i.id)).not.toContain(insVol1.id);
  });

  test('el intento denegado queda en la auditoría', async () => {
    const reg = await models.Auditoria.findOne({ usuarioId: s.vol2.usuario._id, accion: 'acceso_denegado_titularidad' }).lean();
    expect(reg).not.toBeNull();
  });

  test('el titular sí puede cancelar y se libera el cupo', async () => {
    const res = await request(app).delete(`/api/v1/inscripciones/${insVol1.id}`).set(bearer(s.vol1));
    expect(res.status).toBe(204);
    const a = await models.Actividad.findById(insVol1.actividadId).lean();
    expect(a.inscriptos).toBe(0);
  });

  test('después de cancelar se puede volver a inscribir', async () => {
    const res = await inscribir(s.vol1, insVol1.actividadId);
    expect(res.status).toBe(201);
  });
});

describe('Gestión de participantes (R4) + RS3', () => {
  let act;
  let ins;
  beforeAll(async () => {
    act = await crearActividad(s.coordA);
    ins = (await inscribir(s.vol1, act.id)).body;
  });

  test('el coordinador de la org ve los inscriptos con su contacto', async () => {
    const res = await request(app).get(`/api/v1/actividades/${act.id}/inscripciones`).set(bearer(s.coordA));
    expect(res.status).toBe(200);
    expect(res.body[0].voluntario.telefono).toBeDefined();
    expect(res.body[0].voluntario.email).toBeUndefined();
  });

  test('un coordinador de otra org NO puede aceptar', async () => {
    const res = await request(app).patch(`/api/v1/inscripciones/${ins.id}`).set(bearer(s.coordB)).send({ estado: 'aceptada' });
    expect(res.status).toBe(403);
  });

  test('un voluntario NO puede aceptarse a sí mismo', async () => {
    const res = await request(app).patch(`/api/v1/inscripciones/${ins.id}`).set(bearer(s.vol1)).send({ estado: 'aceptada' });
    expect(res.status).toBe(403);
  });

  test('estado inválido -> 400', async () => {
    const res = await request(app).patch(`/api/v1/inscripciones/${ins.id}`).set(bearer(s.coordA)).send({ estado: 'cancelada' });
    expect(res.status).toBe(400);
  });

  test('el coordinador de la org acepta -> 200', async () => {
    const res = await request(app).patch(`/api/v1/inscripciones/${ins.id}`).set(bearer(s.coordA)).send({ estado: 'aceptada' });
    expect(res.status).toBe(200);
    expect(res.body.estado).toBe('aceptada');
  });

  test('aceptar dos veces -> 409 transición inválida', async () => {
    const res = await request(app).patch(`/api/v1/inscripciones/${ins.id}`).set(bearer(s.coordA)).send({ estado: 'aceptada' });
    expect(res.status).toBe(409);
  });

  test('certificado sin asistencia validada -> 409', async () => {
    const res = await request(app).get(`/api/v1/inscripciones/${ins.id}/certificado`).set(bearer(s.vol1));
    expect(res.status).toBe(409);
  });

  test('con asistencia manual validada, el titular descarga el PDF', async () => {
    const man = await request(app).post(`/api/v1/inscripciones/${ins.id}/asistencia-manual`).set(bearer(s.coordA)).send({ horas: 2 });
    expect(man.status).toBe(201);
    const res = await request(app).get(`/api/v1/inscripciones/${ins.id}/certificado`).set(bearer(s.vol1)).buffer(true);
    expect(res.status).toBe(200);
    expect(res.headers['content-type']).toContain('application/pdf');
  });

  test('un coordinador de otra org NO descarga el certificado', async () => {
    const res = await request(app).get(`/api/v1/inscripciones/${ins.id}/certificado`).set(bearer(s.coordB));
    expect(res.status).toBe(403);
  });
});
