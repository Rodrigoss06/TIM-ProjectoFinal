package com.grupob.inventario.repository.memory;

import com.grupob.inventario.domain.model.Movimiento;
import com.grupob.inventario.repository.MovimientoRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class MovimientoRepositoryEnMemoria implements MovimientoRepository {

    private final List<Movimiento> movimientos = new CopyOnWriteArrayList<>();

    @Override
    public void registrar(Movimiento movimiento) {
        Objects.requireNonNull(movimiento);
        movimientos.add(movimiento);
    }

    @Override
    public List<Movimiento> historialDe(String codigoProducto) {
        Objects.requireNonNull(codigoProducto);
        return List.copyOf(movimientos.stream()
                .filter(m -> m.codigoProducto().equalsIgnoreCase(codigoProducto))
                .collect(Collectors.toList()));
    }

    @Override
    public boolean tieneMovimientosRecientes(String codigoProducto, Duration ventana, Clock clock) {
        Objects.requireNonNull(codigoProducto);
        Objects.requireNonNull(ventana);
        Objects.requireNonNull(clock);
        Instant limite = Instant.now(clock).minus(ventana);
        return movimientos.stream()
                .filter(m -> m.codigoProducto().equalsIgnoreCase(codigoProducto))
                .anyMatch(m -> m.fecha().isAfter(limite));
    }
}
