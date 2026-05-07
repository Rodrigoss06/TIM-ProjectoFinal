package com.grupob.inventario.domain.exception;

public class ConfiguracionException extends InventarioException {
    public ConfiguracionException(String mensaje) {
        super(mensaje);
    }

    public ConfiguracionException(String mensaje, Throwable causa) {
        super(mensaje);
        initCause(causa);
    }
}
