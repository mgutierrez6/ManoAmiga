// Variables de entorno SOLO para los tests (secretos de prueba, nunca se usan en producción).
process.env.NODE_ENV = 'test';
process.env.JWT_SECRET = 'test-jwt-secret-solo-para-tests-0000000000';
process.env.JWT_REFRESH_SECRET = 'test-refresh-secret-solo-para-tests-000000';
process.env.QR_SECRET = 'test-qr-secret-solo-para-tests-00000000000';
