package com.grupob.inventario.validation;

import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.exception.StockInsuficienteException;
import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.util.MensajesError;

public final class StockValidator {

    public void validarMovimiento(int cantidad, int stockActual, TipoMovimiento tipo) {
        if (cantidad <= 0) {
            throw new ValidacionException(MensajesError.CANTIDAD_NO_POSITIVA);
        }
        if (tipo == TipoMovimiento.SALIDA && cantidad > stockActual) {
            throw new StockInsuficienteException(stockActual);
        }
    }
}
