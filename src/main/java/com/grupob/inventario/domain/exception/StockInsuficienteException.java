package com.grupob.inventario.domain.exception;

import com.grupob.inventario.util.MensajesError;

public class StockInsuficienteException extends InventarioException {
    private final int stockDisponible;

    public StockInsuficienteException(int stockDisponible) {
        super(String.format(MensajesError.STOCK_INSUFICIENTE_FMT, stockDisponible));
        this.stockDisponible = stockDisponible;
    }

    public int getStockDisponible() {
        return stockDisponible;
    }
}
