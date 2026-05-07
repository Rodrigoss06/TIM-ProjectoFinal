package com.grupob.inventario.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Implementación exacta de la sección 4.3 de Notion etapa 2.
 *
 * Gestiona el ciclo de vida del EntityManager con ThreadLocal.
 * Reglas clave (sección 10.2 Notion):
 * - Toda operación que escribe va en enTransaccion(...)
 * - Las lecturas van en soloLectura(...)
 * - Sin @Transactional (no usamos Spring)
 * - Excepciones runtime hacen rollback automático
 */
public class TransactionManager implements GestorTransacciones {

    private static final ThreadLocal<EntityManager> ACTUAL = new ThreadLocal<>();
    private final EntityManagerFactory emf;

    public TransactionManager(EntityManagerFactory emf) {
        this.emf = Objects.requireNonNull(emf, "EntityManagerFactory no puede ser null");
    }

    /** Ejecuta trabajo en una transacción. Rollback automático ante RuntimeException. */
    public <T> T enTransaccion(Supplier<T> trabajo) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        ACTUAL.set(em);
        try {
            tx.begin();
            T resultado = trabajo.get();
            tx.commit();
            return resultado;
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            ACTUAL.remove();
            em.close();
        }
    }

    /** Sobrecarga para operaciones void. */
    public void enTransaccion(Runnable trabajo) {
        enTransaccion(() -> { trabajo.run(); return null; });
    }

    /** Ejecuta trabajo con EntityManager propio pero sin transacción explícita (solo lectura). */
    public <T> T soloLectura(Supplier<T> trabajo) {
        EntityManager em = emf.createEntityManager();
        ACTUAL.set(em);
        try {
            return trabajo.get();
        } finally {
            ACTUAL.remove();
            em.close();
        }
    }

    /**
     * Retorna el EntityManager activo del hilo actual.
     * Lanza IllegalStateException si no hay transacción activa
     * (indica error de programación — envolver en enTransaccion/soloLectura).
     */
    public static EntityManager actual() {
        EntityManager em = ACTUAL.get();
        if (em == null) {
            throw new IllegalStateException(
                    "No hay EntityManager activo. Envuelve la operación en " +
                    "TransactionManager.enTransaccion(...) o soloLectura(...)");
        }
        return em;
    }
}
