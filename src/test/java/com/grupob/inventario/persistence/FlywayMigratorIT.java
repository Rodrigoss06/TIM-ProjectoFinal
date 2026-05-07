package com.grupob.inventario.persistence;

import com.grupob.inventario.config.Configuracion;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("FlywayMigratorIT — schema correcto tras migrar (V1, V2, V3)")
class FlywayMigratorIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventario_b")
            .withUsername("inventario")
            .withPassword("inventario_dev");

    private static HikariDataSource dataSource;

    @BeforeAll
    static void init() {
        Properties props = new Properties();
        props.setProperty("db.url",      postgres.getJdbcUrl());
        props.setProperty("db.usuario",  postgres.getUsername());
        props.setProperty("db.password", postgres.getPassword());
        Configuracion cfg = new Configuracion(props, Map.of());

        dataSource = DataSourceFactory.crear(cfg);
        FlywayMigrator.migrar(dataSource);
    }

    @AfterAll
    static void teardown() {
        if (dataSource != null) dataSource.close();
    }

    // ── tablas ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("V1: tabla 'productos' existe")
    void v1_tablaProductosExiste() throws Exception {
        assertThat(tablaExiste("productos")).isTrue();
    }

    @Test
    @DisplayName("V1: tabla 'usuarios' existe")
    void v1_tablaUsuariosExiste() throws Exception {
        assertThat(tablaExiste("usuarios")).isTrue();
    }

    @Test
    @DisplayName("V1: tabla 'movimientos' existe")
    void v1_tablaMovimientosExiste() throws Exception {
        assertThat(tablaExiste("movimientos")).isTrue();
    }

    @Test
    @DisplayName("V1: tabla 'auditoria' existe")
    void v1_tablaAuditoriaExiste() throws Exception {
        assertThat(tablaExiste("auditoria")).isTrue();
    }

    // ── columnas clave ────────────────────────────────────────────────

    @Test
    @DisplayName("V1: productos tiene columnas obligatorias (codigo, nombre, precio_unitario, stock, activo)")
    void v1_productosColumnas() throws Exception {
        assertThat(columnaExiste("productos", "codigo")).isTrue();
        assertThat(columnaExiste("productos", "nombre")).isTrue();
        assertThat(columnaExiste("productos", "categoria")).isTrue();
        assertThat(columnaExiste("productos", "precio_unitario")).isTrue();
        assertThat(columnaExiste("productos", "stock")).isTrue();
        assertThat(columnaExiste("productos", "activo")).isTrue();
    }

    @Test
    @DisplayName("V1: usuarios tiene columnas (username, password_hash, rol, activo, intentos_fallidos, bloqueado_hasta)")
    void v1_usuariosColumnas() throws Exception {
        assertThat(columnaExiste("usuarios", "username")).isTrue();
        assertThat(columnaExiste("usuarios", "password_hash")).isTrue();
        assertThat(columnaExiste("usuarios", "rol")).isTrue();
        assertThat(columnaExiste("usuarios", "activo")).isTrue();
        assertThat(columnaExiste("usuarios", "intentos_fallidos")).isTrue();
        assertThat(columnaExiste("usuarios", "bloqueado_hasta")).isTrue();
    }

    @Test
    @DisplayName("V1: movimientos tiene columnas (id, codigo_producto, tipo, cantidad, stock_anterior, stock_nuevo, fecha)")
    void v1_movimientosColumnas() throws Exception {
        assertThat(columnaExiste("movimientos", "id")).isTrue();
        assertThat(columnaExiste("movimientos", "codigo_producto")).isTrue();
        assertThat(columnaExiste("movimientos", "tipo")).isTrue();
        assertThat(columnaExiste("movimientos", "cantidad")).isTrue();
        assertThat(columnaExiste("movimientos", "stock_anterior")).isTrue();
        assertThat(columnaExiste("movimientos", "stock_nuevo")).isTrue();
        assertThat(columnaExiste("movimientos", "fecha")).isTrue();
    }

    @Test
    @DisplayName("V1: auditoria tiene columnas (id, fecha, username, tipo_evento, entidad_afectada, detalle)")
    void v1_auditoriaColumnas() throws Exception {
        assertThat(columnaExiste("auditoria", "id")).isTrue();
        assertThat(columnaExiste("auditoria", "fecha")).isTrue();
        assertThat(columnaExiste("auditoria", "username")).isTrue();
        assertThat(columnaExiste("auditoria", "tipo_evento")).isTrue();
        assertThat(columnaExiste("auditoria", "entidad_afectada")).isTrue();
        assertThat(columnaExiste("auditoria", "detalle")).isTrue();
    }

    // ── índices (V3) ──────────────────────────────────────────────────

    @Test
    @DisplayName("V3: idx_productos_nombre existe")
    void v3_idxProductosNombreExiste() throws Exception {
        assertThat(indiceExiste("idx_productos_nombre")).isTrue();
    }

    @Test
    @DisplayName("V3: idx_productos_categoria existe")
    void v3_idxProductosCategoriaExiste() throws Exception {
        assertThat(indiceExiste("idx_productos_categoria")).isTrue();
    }

    @Test
    @DisplayName("V3: idx_auditoria_fecha existe")
    void v3_idxAuditoriaFechaExiste() throws Exception {
        assertThat(indiceExiste("idx_auditoria_fecha")).isTrue();
    }

    @Test
    @DisplayName("V3: idx_movimientos_fecha existe")
    void v3_idxMovimientosFechaExiste() throws Exception {
        assertThat(indiceExiste("idx_movimientos_fecha")).isTrue();
    }

    // ── seed (V2) ─────────────────────────────────────────────────────

    @Test
    @DisplayName("V2: usuario 'admin' seedeado en tabla usuarios")
    void v2_adminSeedExiste() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM usuarios WHERE username = 'admin' AND rol = 'ADMINISTRADOR'");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    // ── helpers JDBC ──────────────────────────────────────────────────

    private static boolean tablaExiste(String tabla) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_name = '" + tabla + "'");
            rs.next();
            return rs.getInt(1) == 1;
        }
    }

    private static boolean columnaExiste(String tabla, String columna) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns " +
                    "WHERE table_schema = 'public' AND table_name = '" + tabla +
                    "' AND column_name = '" + columna + "'");
            rs.next();
            return rs.getInt(1) == 1;
        }
    }

    private static boolean indiceExiste(String nombreIndice) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM pg_indexes WHERE indexname = '" + nombreIndice + "'");
            rs.next();
            return rs.getInt(1) == 1;
        }
    }
}
