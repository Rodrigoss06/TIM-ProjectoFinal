package com.grupob.inventario.domain.exception;

import com.grupob.inventario.util.MensajesError;

public class CredencialesInvalidasException extends InventarioException {
    public CredencialesInvalidasException() {
        super(MensajesError.CREDENCIALES_INVALIDAS);
    }
}
