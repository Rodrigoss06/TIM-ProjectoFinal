package com.grupob.inventario.persistence;

import com.grupob.inventario.domain.exception.PersistenciaException;
import com.grupob.inventario.util.MensajesError;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

public final class FlywayMigrator {

    private FlywayMigrator() {}

    /**
     * Aplica todas las migraciones pendientes en db/migration/.
     * Flyway es idempotente — ya aplicadas no se repiten.
     * Si falla, lanza PersistenciaException (la app no debe arrancar con schema incorrecto).
     */
    public static void migrar(DataSource dataSource) {
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
        } catch (Exception e) {
            throw new PersistenciaException(MensajesError.ERROR_TRANSACCION, e);
        }
    }
}
