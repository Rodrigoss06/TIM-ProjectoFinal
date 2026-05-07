package com.grupob.inventario.repository;

import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.model.Movimiento;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface MovimientoRepository {
    void registrar(Movimiento movimiento);
    List<Movimiento> historialDe(String codigoProducto);
    boolean tieneMovimientosRecientes(String codigoProducto, Duration ventana, Clock clock);

    List<Movimiento> consultar(String codigoProducto, TipoMovimiento tipo, Instant desde, Instant hasta);
}
