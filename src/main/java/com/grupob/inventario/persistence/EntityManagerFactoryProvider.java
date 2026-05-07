package com.grupob.inventario.persistence;

import com.grupob.inventario.config.Configuracion;
import com.grupob.inventario.domain.exception.PersistenciaException;
import com.grupob.inventario.util.MensajesError;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public final class EntityManagerFactoryProvider {

    private EntityManagerFactoryProvider() {}

    /**
     * Crea el EntityManagerFactory inyectando el DataSource en runtime.
     *
     * Reglas (sección 10.1 Notion etapa 2):
     * - hbm2ddl.auto=validate: Hibernate solo verifica el schema, Flyway lo controla.
     * - Sin caché de segundo nivel.
     * - Sin lazy loading suelto (las queries usan JOIN FETCH explícito cuando necesitan joins).
     */
    public static EntityManagerFactory crear(DataSource dataSource, Configuracion cfg) {
        try {
            Map<String, Object> props = new HashMap<>();

            // DataSource inyectado en runtime — las credenciales NO van en persistence.xml
            props.put("hibernate.connection.datasource", dataSource);

            // Dialecto
            props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

            // Flyway controla el schema; Hibernate solo valida (sección 10.1 Notion)
            props.put("hibernate.hbm2ddl.auto", "validate");

            // Sin ruido en logs de producción
            props.put("hibernate.show_sql", "false");
            props.put("hibernate.format_sql", "false");

            // Sin caché de segundo nivel (sección 10.1 Notion)
            props.put("hibernate.cache.use_second_level_cache", "false");
            props.put("hibernate.cache.use_query_cache", "false");

            return Persistence.createEntityManagerFactory("inventario-pu", props);
        } catch (Exception e) {
            throw new PersistenciaException(MensajesError.ERROR_CONEXION_BD, e);
        }
    }
}
