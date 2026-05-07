package com.grupob.inventario.persistence;

import com.grupob.inventario.config.Configuracion;
import com.grupob.inventario.domain.exception.PersistenciaException;
import com.grupob.inventario.util.MensajesError;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class DataSourceFactory {

    private DataSourceFactory() {}

    /**
     * Crea y valida un HikariDataSource desde la Configuracion.
     * Si la conexión de prueba falla, cierra el pool y lanza PersistenciaException.
     */

    public static HikariDataSource crear(Configuracion cfg) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("InventarioPool");
        config.setJdbcUrl(cfg.getDbUrl());
        config.setUsername(cfg.getDbUsuario());
        config.setPassword(cfg.getDbPassword());
        config.setMaximumPoolSize(cfg.getDbPoolSize());
        config.setConnectionTimeout(cfg.getDbPoolTimeoutMs());

        HikariDataSource ds = new HikariDataSource(config);

        // Test rápido de conexión al arrancar
        try (var conn = ds.getConnection()) {
            // OK — la conexión es válida
        } catch (Exception e) {
            ds.close();
            throw new PersistenciaException(MensajesError.ERROR_CONEXION_BD, e);
        }
        return ds;
    }
}
