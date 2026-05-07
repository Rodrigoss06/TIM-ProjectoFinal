package com.grupob.inventario.service;

import com.grupob.inventario.domain.enums.EstadoStock;
import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.util.MensajesError;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public record FiltroListado(
        String categoria,
        BigDecimal precioMin,
        BigDecimal precioMax,
        EstadoStock estado
) {
    public FiltroListado {
        if (precioMin != null && precioMax != null
                && precioMin.compareTo(precioMax) > 0) {
            throw new ValidacionException(MensajesError.VALORES_NEGATIVOS);
        }
    }

    public List<Producto> aplicarA(List<Producto> productos) {
        return List.copyOf(productos.stream()
                .filter(p -> categoria == null || p.getCategoria().equalsIgnoreCase(categoria))
                .filter(p -> precioMin == null || p.getPrecioUnitario().compareTo(precioMin) >= 0)
                .filter(p -> precioMax == null || p.getPrecioUnitario().compareTo(precioMax) <= 0)
                .filter(p -> estado == null || EstadoStock.desde(p.getStock()) == estado)
                .collect(Collectors.toList()));
    }
}
