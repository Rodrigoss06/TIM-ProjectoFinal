package com.grupob.inventario.domain.exception;

import com.grupob.inventario.util.MensajesError;

public class ProductoNoEncontradoException extends InventarioException {
    public ProductoNoEncontradoException() {
        super(MensajesError.PRODUCTO_NO_ENCONTRADO);
    }
}
