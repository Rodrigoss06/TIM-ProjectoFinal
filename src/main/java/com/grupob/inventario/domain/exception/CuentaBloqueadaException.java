package com.grupob.inventario.domain.exception;

import com.grupob.inventario.util.MensajesError;

public class CuentaBloqueadaException extends InventarioException {
    private final int minutosRestantes;

    public CuentaBloqueadaException(int minutosRestantes) {
        super(String.format(MensajesError.CUENTA_BLOQUEADA_FMT, minutosRestantes));
        this.minutosRestantes = minutosRestantes;
    }

    public int getMinutosRestantes() {
        return minutosRestantes;
    }
}
