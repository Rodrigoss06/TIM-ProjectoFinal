package com.grupob.inventario.repository.jpa;

import com.grupob.inventario.domain.model.Movimiento;
import com.grupob.inventario.persistence.TransactionManager;
import com.grupob.inventario.repository.MovimientoRepository;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class MovimientoRepositoryJpa implements MovimientoRepository {

    @Override
    public void registrar(Movimiento movimiento) {
        Objects.requireNonNull(movimiento);
        TransactionManager.actual().persist(movimiento);
    }

    @Override
    public List<Movimiento> historialDe(String codigoProducto) {
        Objects.requireNonNull(codigoProducto);
        return List.copyOf(
            TransactionManager.actual()
                .createQuery(
                    "SELECT m FROM Movimiento m " +
                    "WHERE UPPER(m.codigoProducto) = :codigo " +
                    "ORDER BY m.fecha DESC",
                    Movimiento.class)
                .setParameter("codigo", codigoProducto.trim().toUpperCase())
                .getResultList());
    }

    @Override
    public boolean tieneMovimientosRecientes(String codigoProducto, Duration ventana, Clock clock) {
        Objects.requireNonNull(codigoProducto);
        Instant limite = Instant.now(clock).minus(ventana);
        Long count = TransactionManager.actual()
            .createQuery(
                "SELECT COUNT(m) FROM Movimiento m " +
                "WHERE UPPER(m.codigoProducto) = :codigo AND m.fecha > :limite",
                Long.class)
            .setParameter("codigo", codigoProducto.trim().toUpperCase())
            .setParameter("limite", limite)
            .getSingleResult();
        return count > 0;
    }
}
