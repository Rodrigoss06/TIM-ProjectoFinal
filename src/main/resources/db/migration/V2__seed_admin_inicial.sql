-- Hash de 'Admin123!' generado con BCrypt strength 12
-- En desarrollo el seed se hace también desde Código si la tabla está vacía; este es el mínimo para que la app pueda arrancar.
INSERT INTO usuarios (username, password_hash, rol, activo, intentos_fallidos)
VALUES ('admin', '$2a$12$REEMPLAZAR_CON_HASH_GENERADO', 'ADMINISTRADOR', TRUE, 0)
ON CONFLICT (username) DO NOTHING;
