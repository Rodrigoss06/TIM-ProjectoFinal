package com.grupob.inventario.repository.jpa;

import com.grupob.inventario.config.Configuracion;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.persistence.DataSourceFactory;
import com.grupob.inventario.persistence.EntityManagerFactoryProvider;
import com.grupob.inventario.persistence.FlywayMigrator;
import com.grupob.inventario.persistence.TransactionManager;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("ProductoRepositoryJpaIT — CRUD JPA contra Postgres real")
class ProductoRepositoryJpaIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventario_b")
            .withUsername("inventario")
            .withPassword("inventario_dev");

    private static HikariDataSource      dataSource;
    private static EntityManagerFactory  emf;
    private static TransactionManager    txManager;

    private ProductoRepositoryJpa repo;

    @BeforeAll
    static void init() {
        Properties props = new Properties();
        props.setProperty("db.url",      postgres.getJdbcUrl());
        props.setProperty("db.usuario",  postgres.getUsername());
        props.setProperty("db.password", postgres.getPassword());
        Configuracion cfg = new Configuracion(props, Map.of());

        dataSource = DataSourceFactory.crear(cfg);
        FlywayMigrator.migrar(dataSource);
        emf        = EntityManagerFactoryProvider.crear(dataSource, cfg);
        txManager  = new TransactionManager(emf);
    }

    @AfterAll
    static void teardown() {
        if (emf        != null) emf.close();
        if (dataSource != null) dataSource.close();
    }

    @BeforeEach
    void setUp() {
        repo = new ProductoRepositoryJpa();
        // Limpiar tabla entre tests (Flyway no hace cleanup)
        txManager.enTransaccion(() ->
            TransactionManager.actual()
                .createNativeQuery("DELETE FROM movimientos").executeUpdate());
        txManager.enTransaccion(() ->
            TransactionManager.actual()
                .createNativeQuery("DELETE FROM auditoria").executeUpdate());
        txManager.enTransaccion(() ->
            TransactionManager.actual()
                .createNativeQuery("DELETE FROM productos").executeUpdate());
    }

    private Producto producto(String codigo, String nombre, String categoria, int stock) {
        return new Producto(codigo, nombre, "desc", categoria, new BigDecimal("10.00"), stock);
    }

    // ── guardar + buscarPorCodigo ─────────────────────────────────────

    @Test
    @DisplayName("guardar y buscarPorCodigo retornan el mismo producto")
    void guardar_y_buscarPorCodigo() {
        txManager.enTransaccion(() -> repo.guardar(producto("P001", "Arroz", "Alimentos", 100)));

        assertThat(txManager.soloLectura(() -> repo.buscarPorCodigo("P001")))
                .isPresent()
                .hasValueSatisfying(p -> {
                    assertThat(p.getCodigo()).isEqualTo("P001");
                    assertThat(p.getNombre()).isEqualTo("Arroz");
                    assertThat(p.getStock()).isEqualTo(100);
                });
    }

    @Test
    @DisplayName("buscarPorCodigo es case-insensitive: 'p001' encuentra 'P001'")
    void buscarPorCodigo_caseInsensitive() {
        txManager.enTransaccion(() -> repo.guardar(producto("P001", "Arroz", "Alimentos", 10)));

        assertThat(txManager.soloLectura(() -> repo.buscarPorCodigo("p001"))).isPresent();
        assertThat(txManager.soloLectura(() -> repo.buscarPorCodigo("P001"))).isPresent();
    }

    @Test
    @DisplayName("buscarPorCodigo con código inexistente devuelve Optional vacío")
    void buscarPorCodigo_noExiste_vacio() {
        assertThat(txManager.soloLectura(() -> repo.buscarPorCodigo("NOEXISTE"))).isEmpty();
    }

    // ── búsqueda por nombre ───────────────────────────────────────────

    @Test
    @DisplayName("buscarPorNombre case-insensitive y parcial")
    void buscarPorNombre_caseInsensitiveYParcial() {
        txManager.enTransaccion(() -> {
            repo.guardar(producto("P001", "Camisa Blanca",     "Ropa", 5));
            repo.guardar(producto("P002", "Camiseta Deportiva","Ropa", 8));
            repo.guardar(producto("P003", "Pantalón",          "Ropa", 3));
        });

        List<Producto> resultado = txManager.soloLectura(
                () -> repo.buscarPorNombre("CAMIS"));
        assertThat(resultado).hasSize(2)
                .extracting(Producto::getNombre)
                .containsExactlyInAnyOrder("Camisa Blanca", "Camiseta Deportiva");
    }

    @Test
    @DisplayName("buscarPorNombre vacío retorna todos los activos")
    void buscarPorNombre_vacio_retornaTodos() {
        txManager.enTransaccion(() -> {
            repo.guardar(producto("P001", "Arroz", "Alimentos", 5));
            repo.guardar(producto("P002", "Fideos", "Alimentos", 3));
        });
        assertThat(txManager.soloLectura(() -> repo.buscarPorNombre(""))).hasSize(2);
    }

    // ── listado solo activos ──────────────────────────────────────────

    @Test
    @DisplayName("listarActivos excluye productos con activo=false")
    void listarActivos_excluyeInactivos() {
        txManager.enTransaccion(() -> {
            repo.guardar(producto("P001", "Arroz", "Alimentos", 5));
            repo.guardar(producto("P002", "Fideos","Alimentos", 3));
            repo.eliminar("P001");
        });

        List<Producto> activos = txManager.soloLectura(() -> repo.listarActivos());
        assertThat(activos).hasSize(1)
                .extracting(Producto::getCodigo).containsExactly("P002");
    }

    @Test
    @DisplayName("listarTodos incluye activos e inactivos")
    void listarTodos_incluyeInactivos() {
        txManager.enTransaccion(() -> {
            repo.guardar(producto("P001", "Arroz", "Alimentos", 5));
            repo.guardar(producto("P002", "Fideos","Alimentos", 3));
            repo.eliminar("P001");
        });
        assertThat(txManager.soloLectura(() -> repo.listarTodos())).hasSize(2);
    }

    // ── eliminación lógica ────────────────────────────────────────────

    @Test
    @DisplayName("eliminar: activo=false en BD pero producto sigue ahí")
    void eliminar_logica_productoSiqueEnBD() {
        txManager.enTransaccion(() -> repo.guardar(producto("P001", "Arroz", "Alimentos", 5)));
        txManager.enTransaccion(() -> repo.eliminar("P001"));

        // Aparece en buscarPorCodigo (no filtra por activo)
        assertThat(txManager.soloLectura(() -> repo.buscarPorCodigo("P001")))
                .isPresent().hasValueSatisfying(p -> assertThat(p.isActivo()).isFalse());

        // No aparece en búsqueda activa
        assertThat(txManager.soloLectura(() -> repo.buscarPorNombre("Arroz"))).isEmpty();
        assertThat(txManager.soloLectura(() -> repo.listarActivos())).isEmpty();
    }

    // ── tildes y caracteres UTF-8 ─────────────────────────────────────

    @Test
    @DisplayName("tildes y ñ se persisten y recuperan correctamente (encoding UTF-8)")
    void tildes_y_enie_sePersisteCorrectamente() {
        String nombreConTildes = "Camiseta Niño";
        txManager.enTransaccion(() ->
                repo.guardar(producto("T001", nombreConTildes, "Ropa", 10)));

        String nombreRecuperado = txManager.soloLectura(() ->
                repo.buscarPorCodigo("T001").orElseThrow().getNombre());

        assertThat(nombreRecuperado).isEqualTo(nombreConTildes);
    }
}
