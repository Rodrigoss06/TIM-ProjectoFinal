package com.grupob.inventario.domain.exception;

import com.grupob.inventario.util.MensajesError;

public class ProductoDuplicadoException extends InventarioException {
    public ProductoDuplicadoException() {
        super(MensajesError.CODIGO_DUPLICADO);
    }
}
