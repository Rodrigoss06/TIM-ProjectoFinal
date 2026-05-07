package com.grupob.inventario.persistence;

import java.util.function.Supplier;

/**
 * Implementación fake de GestorTransacciones para tests unitarios.
 *
 * Decisión de diseño (Opción A):
 * Los tests de servicios usan este fake en lugar de un EntityManagerFactory real.
 * Ventaja: tests rápidos, sin Docker/PostgreSQL, sin JPA overhead.
 * Los tests de integración (*IT.java) usan TransactionManager real con Testcontainers.
 */
public class TransactionManagerFake implements GestorTransacciones {

    @Override
    public <T> T enTransaccion(Supplier<T> trabajo) {
        return trabajo.get();
    }

    @Override
    public void enTransaccion(Runnable trabajo) {
        trabajo.run();
    }

    @Override
    public <T> T soloLectura(Supplier<T> trabajo) {
        return trabajo.get();
    }
}
