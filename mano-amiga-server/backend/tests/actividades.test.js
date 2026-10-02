const { app, request, conectar, desconectar, escenario, crearActividad, datosActividad, bearer } = require('./helpers');

let s;
beforeAll(async () => {
  await conectar();
  s = await escenario();
});
afterAll(desconectar);

describe('Crear y editar actividades (R2)', () => {
  test('la organización se toma del token, no del body', async () => {
    const res = await request(app)
      .post('/api/v1/actividades')
      .set(bearer(s.coordA))
      .send({ ...datosActividad(), organizacionId: String(s.orgB._id) });
    expect(res.status).toBe(201);
    expect(res.body.organizacionId).toBe(String(s.orgA._id));
  });

  test('datos inválidos -> 400 (fin antes que inicio, cupo 0)', async () => {
    const inicio = new Date(Date.now() + 86400000).toISOString();
    const res = await request(app)
      .post('/api/v1/actividades')
      .set(bearer(s.coordA))
      .send(datosActividad({ fechaInicio: inicio, fechaFin: new Date().toISOString(), cupo: 0 }));
    expect(res.status).toBe(400);
    const campos = res.body.error.details.map((d) => d.campo);
    expect(campos).toEqual(expect.arrayContaining(['fechaFin', 'cupo']));
  });

  test('etiqueta inexistente -> 400', async () => {
    const res = await request(app).post('/api/v1/actividades').set(bearer(s.coordA)).send(datosActividad({ etiquetas: ['64b7f0c2a1b2c3d4e5f60718'] }));
    expect(res.status).toBe(400);
  });

  test('el coordinador edita las actividades de su organización', async () => {
    const act = await crearActividad(s.coordA);
    const res = await request(app).patch(`/api/v1/actividades/${act.id}`).set(bearer(s.coordA)).send({ titulo: 'Nuevo título' });
    expect(res.status).toBe(200);
    expect(res.body.titulo).toBe('Nuevo título');
  });
});

describe('RS3 · aislamiento entre organizaciones (A2)', () => {
  let act;
  beforeAll(async () => {
    act = await crearActividad(s.coordA);
  });

  test('otro coordinador NO puede editar la actividad', async () => {
    const res = await request(app).patch(`/api/v1/actividades/${act.id}`).set(bearer(s.coordB)).send({ titulo: 'hackeado' });
    expect(res.status).toBe(403);
  });

  test('otro coordinador NO puede cancelarla', async () => {
    const res = await request(app).delete(`/api/v1/actividades/${act.id}`).set(bearer(s.coordB));
    expect(res.status).toBe(403);
  });

  test('otro coordinador NO puede ver los inscriptos', async () => {
    const res = await request(app).get(`/api/v1/actividades/${act.id}/inscripciones`).set(bearer(s.coordB));
    expect(res.status).toBe(403);
  });

  test('otro coordinador NO puede generar el QR', async () => {
    const res = await request(app).get(`/api/v1/actividades/${act.id}/qr`).set(bearer(s.coordB));
    expect(res.status).toBe(403);
  });

  test('"mias" solo trae las de su organización', async () => {
    const res = await request(app).get('/api/v1/actividades?mias=true').set(bearer(s.coordB));
    expect(res.status).toBe(200);
    expect(res.body.data.every((a) => a.organizacionId === String(s.orgB._id))).toBe(true);
  });

  test('un voluntario no puede usar "mias"', async () => {
    const res = await request(app).get('/api/v1/actividades?mias=true').set(bearer(s.vol1));
    expect(res.status).toBe(403);
  });
});

describe('ABAC · voluntario judicial y etiquetas sensibles', () => {
  let sensible;
  let normal;
  beforeAll(async () => {
    sensible = await crearActividad(s.coordA, { titulo: 'Apoyo escolar', etiquetas: [String(s.etiquetas.sensible._id)] });
    normal = await crearActividad(s.coordA, { titulo: 'Plantación', etiquetas: [String(s.etiquetas.normal._id)] });
  });

  test('el listado del judicial NO incluye la actividad sensible (filtro en la consulta)', async () => {
    const res = await request(app).get('/api/v1/actividades?limit=50').set(bearer(s.judicial));
    const ids = res.body.data.map((a) => a.id);
    expect(ids).toContain(normal.id);
    expect(ids).not.toContain(sensible.id);
  });

  test('un voluntario común sí la ve', async () => {
    const res = await request(app).get('/api/v1/actividades?limit=50').set(bearer(s.vol1));
    expect(res.body.data.map((a) => a.id)).toContain(sensible.id);
  });

  test('pedirla por id -> 403', async () => {
    const res = await request(app).get(`/api/v1/actividades/${sensible.id}`).set(bearer(s.judicial));
    expect(res.status).toBe(403);
  });

  test('inscribirse -> 403', async () => {
    const res = await request(app).post(`/api/v1/actividades/${sensible.id}/inscripciones`).set(bearer(s.judicial));
    expect(res.status).toBe(403);
  });

  test('en la actividad normal se puede inscribir', async () => {
    const res = await request(app).post(`/api/v1/actividades/${normal.id}/inscripciones`).set(bearer(s.judicial));
    expect(res.status).toBe(201);
  });
});

describe('Validación de identificadores', () => {
  test(':id que no es ObjectId -> 400', async () => {
    const res = await request(app).get('/api/v1/actividades/123').set(bearer(s.vol1));
    expect(res.status).toBe(400);
  });

  test(':id inexistente -> 404', async () => {
    const res = await request(app).get('/api/v1/actividades/64b7f0c2a1b2c3d4e5f60718').set(bearer(s.vol1));
    expect(res.status).toBe(404);
  });
});
