const jwt = require('jsonwebtoken');
const { app, request, conectar, desconectar, escenario, crearActividad, bearer } = require('./helpers');

let s;
let act;
let insVol1;
beforeAll(async () => {
  await conectar();
  s = await escenario();
  act = await crearActividad(s.coordA);
  insVol1 = (await request(app).post(`/api/v1/actividades/${act.id}/inscripciones`).set(bearer(s.vol1))).body;
  await request(app).post(`/api/v1/actividades/${act.id}/inscripciones`).set(bearer(s.vol2)); // queda pendiente
  await request(app).patch(`/api/v1/inscripciones/${insVol1.id}`).set(bearer(s.coordA)).send({ estado: 'aceptada' });
});
afterAll(desconectar);

const pedirQR = async () => (await request(app).get(`/api/v1/actividades/${act.id}/qr`).set(bearer(s.coordA))).body.qr;
const escanear = (vol, qr, actId = act.id) => request(app).post(`/api/v1/actividades/${actId}/asistencias`).set(bearer(vol)).send({ qr });

describe('Registro de horas con QR (2.3 · 5.8)', () => {
  test('el coordinador obtiene un QR que vence en 60 s', async () => {
    const res = await request(app).get(`/api/v1/actividades/${act.id}/qr`).set(bearer(s.coordA));
    expect(res.status).toBe(200);
    expect(res.headers['cache-control']).toBe('no-store');
    const payload = jwt.decode(res.body.qr);
    expect(payload.exp - payload.iat).toBe(60);
  });

  test('un inscripto NO aceptado no puede marcar asistencia', async () => {
    const res = await escanear(s.vol2, await pedirQR());
    expect(res.status).toBe(403);
  });

  test('QR falsificado (firmado con otro secreto) -> 400', async () => {
    const falso = jwt.sign({ act: act.id, typ: 'qr' }, 'otro-secreto', { expiresIn: 60, audience: 'mano-amiga-qr', issuer: 'mano-amiga-api' });
    const res = await escanear(s.vol1, falso);
    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe('QR_INVALIDO');
  });

  test('QR vencido -> 400 QR_VENCIDO', async () => {
    const vencido = jwt.sign({ act: act.id, typ: 'qr', iat: Math.floor(Date.now() / 1000) - 120 }, process.env.QR_SECRET, {
      expiresIn: 60,
      audience: 'mano-amiga-qr',
      issuer: 'mano-amiga-api',
    });
    const res = await escanear(s.vol1, vencido);
    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe('QR_VENCIDO');
  });

  test('QR de otra actividad -> 400', async () => {
    const otra = await crearActividad(s.coordA);
    const qrOtra = (await request(app).get(`/api/v1/actividades/${otra.id}/qr`).set(bearer(s.coordA))).body.qr;
    const res = await escanear(s.vol1, qrOtra);
    expect(res.status).toBe(400);
  });

  test('aceptado + QR válido -> 201 check-in', async () => {
    const res = await escanear(s.vol1, await pedirQR());
    expect(res.status).toBe(201);
    expect(res.body.estado).toBe('abierta');
    expect(res.body.metodo).toBe('qr');
  });

  test('escanear otra vez -> 409', async () => {
    const res = await escanear(s.vol1, await pedirQR());
    expect(res.status).toBe(409);
  });

  test('un coordinador de otra org NO puede validar la asistencia', async () => {
    const lista = await request(app).get(`/api/v1/actividades/${act.id}/asistencias`).set(bearer(s.coordA));
    const res = await request(app).patch(`/api/v1/asistencias/${lista.body[0].id}/validar`).set(bearer(s.coordB));
    expect(res.status).toBe(403);
  });

  test('el coordinador de la org valida -> se calculan las horas', async () => {
    const lista = await request(app).get(`/api/v1/actividades/${act.id}/asistencias`).set(bearer(s.coordA));
    const res = await request(app).patch(`/api/v1/asistencias/${lista.body[0].id}/validar`).set(bearer(s.coordA));
    expect(res.status).toBe(200);
    expect(res.body.estado).toBe('validada');
    expect(res.body.horas).toBeGreaterThanOrEqual(0);
  });

  test('validar dos veces -> 409', async () => {
    const lista = await request(app).get(`/api/v1/actividades/${act.id}/asistencias`).set(bearer(s.coordA));
    const res = await request(app).patch(`/api/v1/asistencias/${lista.body[0].id}/validar`).set(bearer(s.coordA));
    expect(res.status).toBe(409);
  });

  test('QR fuera del horario de la actividad -> 409', async () => {
    const futura = await crearActividad(s.coordA, {
      fechaInicio: new Date(Date.now() + 5 * 86400000).toISOString(),
      fechaFin: new Date(Date.now() + 5 * 86400000 + 3600000).toISOString(),
    });
    const res = await request(app).get(`/api/v1/actividades/${futura.id}/qr`).set(bearer(s.coordA));
    expect(res.status).toBe(409);
  });
});

describe('Horas del voluntario judicial', () => {
  test('un voluntario común no tiene resumen de horas -> 403', async () => {
    const res = await request(app).get('/api/v1/usuarios/me/horas').set(bearer(s.vol1));
    expect(res.status).toBe(403);
  });

  test('el judicial ve horas asignadas, cumplidas (calculadas) y restantes', async () => {
    const act2 = await crearActividad(s.coordA);
    const ins = (await request(app).post(`/api/v1/actividades/${act2.id}/inscripciones`).set(bearer(s.judicial))).body;
    await request(app).patch(`/api/v1/inscripciones/${ins.id}`).set(bearer(s.coordA)).send({ estado: 'aceptada' });
    await request(app).post(`/api/v1/inscripciones/${ins.id}/asistencia-manual`).set(bearer(s.coordA)).send({ horas: 2.5 });

    const res = await request(app).get('/api/v1/usuarios/me/horas').set(bearer(s.judicial));
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ horasAsignadas: 30, horasCumplidas: 2.5, horasRestantes: 27.5 });
  });

  test('horas manuales mayores a la duración -> 400', async () => {
    const act3 = await crearActividad(s.coordA);
    const ins = (await request(app).post(`/api/v1/actividades/${act3.id}/inscripciones`).set(bearer(s.vol2))).body;
    await request(app).patch(`/api/v1/inscripciones/${ins.id}`).set(bearer(s.coordA)).send({ estado: 'aceptada' });
    const res = await request(app).post(`/api/v1/inscripciones/${ins.id}/asistencia-manual`).set(bearer(s.coordA)).send({ horas: 20 });
    expect(res.status).toBe(400);
  });
});
