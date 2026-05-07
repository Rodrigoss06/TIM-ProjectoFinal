package com.grupob.inventario.validation;

import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.util.MensajesError;

import java.math.BigDecimal;
import java.util.Objects;

public final class ProductoValidator {

    public void validarRegistro(Producto producto) {
        Objects.requireNonNull(producto, "producto no puede ser null");
        validarCamposObligatorios(producto);
        validarNumericos(producto);
    }

    public void validarActualizacion(Producto producto) {
        Objects.requireNonNull(producto, "producto no puede ser null");
        validarCamposObligatorios(producto);
        validarNumericos(producto);
    }

    private void validarCamposObligatorios(Producto p) {
        if (esBlancoONulo(p.getCodigo()) || esBlancoONulo(p.getNombre())
                || esBlancoONulo(p.getCategoria()) || p.getPrecioUnitario() == null) {
            throw new ValidacionException(MensajesError.CAMPOS_OBLIGATORIOS);
        }
    }

    private void validarNumericos(Producto p) {
        if (p.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0
                || p.getPrecioUnitario().scale() > 2
                || p.getStock() < 0) {
            throw new ValidacionException(MensajesError.VALORES_NEGATIVOS);
        }
    }

    private static boolean esBlancoONulo(String s) {
        return s == null || s.isBlank();
    }
}
