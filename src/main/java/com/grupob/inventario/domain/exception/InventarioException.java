package com.grupob.inventario.domain.exception;

public class InventarioException extends RuntimeException {
    public InventarioException(String mensaje) {
        super(mensaje);
    }
}
