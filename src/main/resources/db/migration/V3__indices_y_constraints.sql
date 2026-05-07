CREATE INDEX idx_productos_nombre ON productos (LOWER(nombre));
CREATE INDEX idx_productos_categoria ON productos (categoria);
CREATE INDEX idx_productos_activo ON productos (activo);
CREATE INDEX idx_movimientos_codigo_producto ON movimientos (codigo_producto);
CREATE INDEX idx_movimientos_fecha ON movimientos (fecha DESC);
CREATE INDEX idx_auditoria_fecha ON auditoria (fecha DESC);
CREATE INDEX idx_auditoria_username ON auditoria (username);
CREATE INDEX idx_auditoria_tipo_evento ON auditoria (tipo_evento);

-- La auditoría es append-only: revocamos UPDATE y DELETE al rol de la app
-- (esto se hace fuera de Flyway porque depende del usuario real de la BD)
