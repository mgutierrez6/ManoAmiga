const { app, request, conectar, desconectar, crearUsuario, crearOrganizacion, bearer, models } = require('./helpers');

beforeAll(conectar);
afterAll(desconectar);

const registro = (extra = {}) => ({
  nombre: 'Ana',
  apellido: 'Pérez',
  email: `ana-${Math.random().toString(36).slice(2)}@mail.com`,
  password: 'Password123',
  telefono: '099 123 456',
  ...extra,
});

describe('Registro (5.1 · mass assignment)', () => {
  test('crea la cuenta siempre como voluntario aunque mande rol/admin/judicial', async () => {
    const res = await request(app)
      .post('/api/v1/auth/register')
      .send(registro({ rol: 'admin', esJudicial: true, organizacionId: '64b7f0c2a1b2c3d4e5f60718', horasAsignadas: 999 }));
    expect(res.status).toBe(201);
    expect(res.body.usuario.rol).toBe('voluntario');
    const u = await models.Usuario.findById(res.body.usuario.id).select('+passwordHash').lean();
    expect(u.rol).toBe('voluntario');
    expect(u.esJudicial).toBe(false);
    expect(u.organizacionId).toBeNull();
    expect(u.horasAsignadas).toBe(0);
    expect(u.passwordHash).toMatch(/^\$2[aby]\$/); // hash bcrypt, nunca la contraseña
  });

  test('email repetido -> 409', async () => {
    const datos = registro();
    await request(app).post('/api/v1/auth/register').send(datos);
    const res = await request(app).post('/api/v1/auth/register').send(datos);
    expect(res.status).toBe(409);
  });

  test('contraseña corta -> 400 con detalle del campo', async () => {
    const res = await request(app).post('/api/v1/auth/register').send(registro({ password: '123' }));
    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe('VALIDACION');
    expect(res.body.error.details[0].campo).toBe('password');
  });
});

describe('Login', () => {
  test('ok devuelve access + refresh token y no devuelve el hash', async () => {
    const { usuario, password } = await crearUsuario();
    const res = await request(app).post('/api/v1/auth/login').send({ email: usuario.email, password });
    expect(res.status).toBe(200);
    expect(res.body.accessToken).toBeDefined();
    expect(res.body.refreshToken).toHaveLength(128);
    expect(JSON.stringify(res.body)).not.toContain('passwordHash');
  });

  test('mensaje genérico: mismo código si el email no existe o la contraseña es incorrecta', async () => {
    const { usuario } = await crearUsuario();
    const a = await request(app).post('/api/v1/auth/login').send({ email: usuario.email, password: 'incorrecta1' });
    const b = await request(app).post('/api/v1/auth/login').send({ email: 'noexiste@mail.com', password: 'incorrecta1' });
    expect(a.status).toBe(401);
    expect(b.status).toBe(401);
    expect(a.body.error.code).toBe(b.body.error.code);
  });

  test('inyección NoSQL en el login no funciona', async () => {
    const { usuario } = await crearUsuario();
    const res = await request(app).post('/api/v1/auth/login').send({ email: usuario.email, password: { $ne: null } });
    expect([400, 401]).toContain(res.status);
    expect(res.body.accessToken).toBeUndefined();
  });

  test('se registra en la auditoría (éxito y fallo)', async () => {
    const { usuario, password } = await crearUsuario();
    await request(app).post('/api/v1/auth/login').send({ email: usuario.email, password: 'mal-password' });
    await request(app).post('/api/v1/auth/login').send({ email: usuario.email, password });
    const registros = await models.Auditoria.find({ usuarioId: usuario._id, accion: 'login' }).lean();
    expect(registros.map((r) => r.resultado).sort()).toEqual(['fallo', 'ok']);
  });

  test('más de 5 intentos fallidos -> 429', async () => {
    const { usuario } = await crearUsuario();
    let ultimo;
    for (let i = 0; i < 6; i++) {
      ultimo = await request(app).post('/api/v1/auth/login').send({ email: usuario.email, password: 'incorrecta1' });
    }
    expect(ultimo.status).toBe(429);
  });
});

describe('Refresh token con rotación (5.2)', () => {
  test('rota el token y detecta el reuso de uno viejo (cierra todas las sesiones)', async () => {
    const { usuario, password } = await crearUsuario();
    const login = await request(app).post('/api/v1/auth/login').send({ email: usuario.email, password });
    const viejo = login.body.refreshToken;

    const r1 = await request(app).post('/api/v1/auth/refresh').send({ refreshToken: viejo });
    expect(r1.status).toBe(200);
    expect(r1.body.refreshToken).not.toBe(viejo);

    // Un atacante reusa el token viejo
    const robo = await request(app).post('/api/v1/auth/refresh').send({ refreshToken: viejo });
    expect(robo.status).toBe(401);

    // Como hubo reuso, también se invalidó el token nuevo
    const r2 = await request(app).post('/api/v1/auth/refresh').send({ refreshToken: r1.body.refreshToken });
    expect(r2.status).toBe(401);
  });

  test('logout borra las sesiones', async () => {
    const { usuario, password } = await crearUsuario();
    const login = await request(app).post('/api/v1/auth/login').send({ email: usuario.email, password });
    const out = await request(app).post('/api/v1/auth/logout').set('Authorization', `Bearer ${login.body.accessToken}`).send({ refreshToken: login.body.refreshToken });
    expect(out.status).toBe(204);
    const r = await request(app).post('/api/v1/auth/refresh').send({ refreshToken: login.body.refreshToken });
    expect(r.status).toBe(401);
  });
});

describe('RS5 (base) · los permisos se releen de la base en cada pedido', () => {
  test('un usuario desactivado pierde el acceso en el siguiente pedido, aunque su JWT no venció', async () => {
    const u = await crearUsuario();
    expect((await request(app).get('/api/v1/usuarios/me').set(bearer(u))).status).toBe(200);
    await models.Usuario.updateOne({ _id: u.usuario._id }, { activo: false });
    expect((await request(app).get('/api/v1/usuarios/me').set(bearer(u))).status).toBe(401);
  });

  test('si se da de baja la organización, su coordinador pierde el acceso inmediatamente', async () => {
    const org = await crearOrganizacion();
    const coord = await crearUsuario({ rol: 'coordinador', organizacionId: org._id });
    expect((await request(app).get('/api/v1/actividades?mias=true').set(bearer(coord))).status).toBe(200);
    await models.Organizacion.updateOne({ _id: org._id }, { activa: false });
    expect((await request(app).get('/api/v1/actividades?mias=true').set(bearer(coord))).status).toBe(401);
  });

  test('si le cambian el rol, el cambio aplica sin esperar a que venza el token', async () => {
    const org = await crearOrganizacion();
    const coord = await crearUsuario({ rol: 'coordinador', organizacionId: org._id });
    await models.Usuario.updateOne({ _id: coord.usuario._id }, { rol: 'voluntario', organizacionId: null });
    const res = await request(app).post('/api/v1/actividades').set(bearer(coord)).send({});
    expect(res.status).toBe(403);
  });
});

describe('Perfil propio', () => {
  test('GET /usuarios/me devuelve solo los datos del token, sin hash ni push tokens', async () => {
    const u = await crearUsuario();
    const res = await request(app).get('/api/v1/usuarios/me').set(bearer(u));
    expect(res.status).toBe(200);
    expect(res.body.id).toBe(String(u.usuario._id));
    expect(res.body.passwordHash).toBeUndefined();
    expect(res.body.pushTokens).toBeUndefined();
  });

  test('push token inválido -> 400; válido -> 204', async () => {
    const u = await crearUsuario();
    const mal = await request(app).post('/api/v1/usuarios/me/push-token').set(bearer(u)).send({ token: 'cualquier-cosa' });
    expect(mal.status).toBe(400);
    const ok = await request(app).post('/api/v1/usuarios/me/push-token').set(bearer(u)).send({ token: 'ExponentPushToken[abcdefghijklmnop]' });
    expect(ok.status).toBe(204);
  });
});
