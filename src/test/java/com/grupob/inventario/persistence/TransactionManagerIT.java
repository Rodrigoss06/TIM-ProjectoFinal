package com.grupob.inventario.persistence;

import com.grupob.inventario.config.Configuracion;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("TransactionManagerIT — enTransaccion, rollback y soloLectura (RF-INV-008)")
class TransactionManagerIT {

    // Hash BCrypt válido en formato (60 chars): $2a$12$ + 53 chars
    private static final String HASH_TEST =
            "$2a$12$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".substring(0, 60);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventario_b")
            .withUsername("inventario")
            .withPassword("inventario_dev");

    private static HikariDataSource    dataSource;
    private static EntityManagerFactory emf;
    private static TransactionManager  txManager;

    @BeforeAll
    static void init() {
        Properties props = new Properties();
        props.setProperty("db.url",      postgres.getJdbcUrl());
        props.setProperty("db.usuario",  postgres.getUsername());
        props.setProperty("db.password", postgres.getPassword());
        Configuracion cfg = new Configuracion(props, Map.of());

        dataSource = DataSourceFactory.crear(cfg);
        FlywayMigrator.migrar(dataSource);
        emf       = EntityManagerFactoryProvider.crear(dataSource, cfg);
        txManager = new TransactionManager(emf);
    }

    @AfterAll
    static void teardown() {
        if (emf        != null) emf.close();
        if (dataSource != null) dataSource.close();
    }

    // ── transacción exitosa ────────────────────────────────────────────

    @Test
    @DisplayName("enTransaccion exitosa persiste los datos en BD")
    void enTransaccion_exitosa_persisteDatos() {
        txManager.enTransaccion(() -> {
            TransactionManager.actual().createNativeQuery(
                "INSERT INTO usuarios (username, password_hash, rol, activo, intentos_fallidos) " +
                "VALUES ('tx_success', '" + HASH_TEST + "', 'ADMINISTRADOR', true, 0)"
            ).executeUpdate();
        });

        Number count = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM usuarios WHERE username = 'tx_success'")
                        .getSingleResult());
        assertThat(count.longValue()).isEqualTo(1L);
    }

    // ── rollback automático ────────────────────────────────────────────

    @Test
    @DisplayName("enTransaccion con RuntimeException hace rollback — ningún dato persiste")
    void enTransaccion_runtimeException_rollback() {
        assertThatThrownBy(() -> txManager.enTransaccion(() -> {
            TransactionManager.actual().createNativeQuery(
                "INSERT INTO usuarios (username, password_hash, rol, activo, intentos_fallidos) " +
                "VALUES ('tx_rollback', '" + HASH_TEST + "', 'ADMINISTRADOR', true, 0)"
            ).executeUpdate();
            throw new RuntimeException("Falla simulada — debe disparar rollback");
        })).isInstanceOf(RuntimeException.class)
           .hasMessage("Falla simulada — debe disparar rollback");

        // Verificar que el rollback funcionó
        Number count = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM usuarios WHERE username = 'tx_rollback'")
                        .getSingleResult());
        assertThat(count.longValue()).isEqualTo(0L);
    }

    @Test
    @DisplayName("doble fallo en transacción — solo el segundo insert revierte, no el primero si ya committeó")
    void enTransaccion_rollbackNoAfectaTransaccionesAnteriores() {
        // Primera transacción exitosa
        txManager.enTransaccion(() -> {
            TransactionManager.actual().createNativeQuery(
                "INSERT INTO usuarios (username, password_hash, rol, activo, intentos_fallidos) " +
                "VALUES ('tx_anterior', '" + HASH_TEST + "', 'ADMINISTRADOR', true, 0)"
            ).executeUpdate();
        });

        // Segunda transacción que falla
        try {
            txManager.enTransaccion(() -> {
                TransactionManager.actual().createNativeQuery(
                    "INSERT INTO usuarios (username, password_hash, rol, activo, intentos_fallidos) " +
                    "VALUES ('tx_rollback2', '" + HASH_TEST + "', 'ADMINISTRADOR', true, 0)"
                ).executeUpdate();
                throw new RuntimeException("Rollback 2");
            });
        } catch (RuntimeException ignored) {}

        // La transacción anterior sigue persistida
        Number anterior = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM usuarios WHERE username = 'tx_anterior'")
                        .getSingleResult());
        assertThat(anterior.longValue()).isEqualTo(1L);

        // La segunda transacción fue revertida
        Number rollbacked = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM usuarios WHERE username = 'tx_rollback2'")
                        .getSingleResult());
        assertThat(rollbacked.longValue()).isEqualTo(0L);
    }

    // ── soloLectura ────────────────────────────────────────────────────

    @Test
    @DisplayName("soloLectura devuelve datos sin abrir transacción explícita")
    void soloLectura_retornaDatos_sinTransaccionExplicita() {
        // Insertar datos para consultar
        txManager.enTransaccion(() -> {
            TransactionManager.actual().createNativeQuery(
                "INSERT INTO usuarios (username, password_hash, rol, activo, intentos_fallidos) " +
                "VALUES ('lectura_test', '" + HASH_TEST + "', 'GESTOR_INVENTARIO', true, 0)"
            ).executeUpdate();
        });

        Number count = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM usuarios WHERE username = 'lectura_test'")
                        .getSingleResult());
        assertThat(count.longValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("actual() sin contexto activo lanza IllegalStateException")
    void actual_sinContexto_lanzaIllegalState() {
        assertThatThrownBy(TransactionManager::actual)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay EntityManager activo");
    }

    // ── enTransaccion con valor de retorno ─────────────────────────────

    @Test
    @DisplayName("enTransaccion(Supplier) retorna el valor del Supplier")
    void enTransaccion_supplier_retornaValor() {
        txManager.enTransaccion(() -> {
            TransactionManager.actual().createNativeQuery(
                "INSERT INTO productos (codigo, nombre, categoria, precio_unitario, stock, activo) " +
                "VALUES ('PROD-TX-01', 'Producto TX', 'Cat', 10.00, 0, true)"
            ).executeUpdate();
        });

        String nombre = (String) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT nombre FROM productos WHERE codigo = 'PROD-TX-01'")
                        .getSingleResult());
        assertThat(nombre).isEqualTo("Producto TX");
    }
}
