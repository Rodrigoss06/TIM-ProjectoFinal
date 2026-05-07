package com.grupob.inventario.persistence;

import java.util.function.Supplier;

public interface GestorTransacciones {
    <T> T enTransaccion(Supplier<T> trabajo);
    void enTransaccion(Runnable trabajo);
    <T> T soloLectura(Supplier<T> trabajo);
}
