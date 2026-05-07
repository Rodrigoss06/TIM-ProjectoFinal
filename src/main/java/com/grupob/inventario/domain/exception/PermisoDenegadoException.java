package com.grupob.inventario.domain.exception;

import com.grupob.inventario.util.MensajesError;

public class PermisoDenegadoException extends InventarioException {
    public PermisoDenegadoException() {
        super(MensajesError.SIN_PERMISOS);
    }
}
