package com.grupob.inventario.domain.enums;

public enum EstadoStock {
    SIN_STOCK,
    BAJO,
    NORMAL;

    public static EstadoStock desde(int stock) {
        if (stock == 0) return SIN_STOCK;
        if (stock < 5)  return BAJO;
        return NORMAL;
    }
}
