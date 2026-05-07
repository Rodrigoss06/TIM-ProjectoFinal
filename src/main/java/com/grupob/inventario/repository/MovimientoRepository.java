package com.grupob.inventario.repository;

import com.grupob.inventario.domain.model.Movimiento;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

public interface MovimientoRepository {

    void registrar(Movimiento movimiento);

    List<Movimiento> historialDe(String codigoProducto);

    boolean tieneMovimientosRecientes(String codigoProducto, Duration ventana, Clock clock);
}
