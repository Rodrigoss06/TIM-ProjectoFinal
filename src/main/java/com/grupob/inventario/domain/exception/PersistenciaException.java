package com.grupob.inventario.domain.exception;

public class PersistenciaException extends InventarioException {
    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje);
        initCause(causa);
    }
}
