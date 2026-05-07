package com.grupob.inventario.domain.exception;

import com.grupob.inventario.util.MensajesError;

public class UsuarioDuplicadoException extends InventarioException {
    public UsuarioDuplicadoException() {
        super(MensajesError.USUARIO_DUPLICADO);
    }
}
