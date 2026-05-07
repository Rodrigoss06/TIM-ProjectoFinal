CREATE TABLE productos (
    codigo VARCHAR(50) PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    descripcion VARCHAR(500),
    categoria VARCHAR(100) NOT NULL,
    precio_unitario NUMERIC(12, 2) NOT NULL CHECK (precio_unitario > 0),
    stock INTEGER NOT NULL CHECK (stock >= 0),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE usuarios (
    username VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(60) NOT NULL,
    rol VARCHAR(30) NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'GESTOR_INVENTARIO')),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta TIMESTAMPTZ
);

CREATE TABLE movimientos (
    id UUID PRIMARY KEY,
    codigo_producto VARCHAR(50) NOT NULL REFERENCES productos(codigo),
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('ENTRADA', 'SALIDA')),
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    stock_anterior INTEGER NOT NULL,
    stock_nuevo INTEGER NOT NULL,
    fecha TIMESTAMPTZ NOT NULL
);

CREATE TABLE auditoria (
    id UUID PRIMARY KEY,
    fecha TIMESTAMPTZ NOT NULL,
    username VARCHAR(50),
    tipo_evento VARCHAR(40) NOT NULL,
    entidad_afectada VARCHAR(100),
    detalle VARCHAR(500)
);
