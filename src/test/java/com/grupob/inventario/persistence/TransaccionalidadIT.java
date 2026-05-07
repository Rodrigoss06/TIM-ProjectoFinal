package com.grupob.inventario.persistence;

import com.grupob.inventario.config.Configuracion;
import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.exception.PersistenciaException;
import com.grupob.inventario.domain.model.EventoAuditoria;
import com.grupob.inventario.domain.model.Movimiento;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.repository.AuditoriaRepository;
import com.grupob.inventario.repository.FiltroAuditoria;
import com.grupob.inventario.repository.MovimientoRepository;
import com.grupob.inventario.repository.jpa.AuditoriaRepositoryJpa;
import com.grupob.inventario.repository.jpa.MovimientoRepositoryJpa;
import com.grupob.inventario.repository.jpa.ProductoRepositoryJpa;
import com.grupob.inventario.security.PermisoChecker;
import com.grupob.inventario.service.AuditoriaService;
import com.grupob.inventario.service.InventarioService;
import com.grupob.inventario.service.ProductoService;
import com.grupob.inventario.util.MensajesError;
import com.grupob.inventario.validation.ProductoValidator;
import com.grupob.inventario.validation.StockValidator;
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
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("TransaccionalidadIT — atomicidad multi-tabla (RF-INV-008)")
class TransaccionalidadIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventario_b")
            .withUsername("inventario")
            .withPassword("inventario_dev");

    private static HikariDataSource     dataSource;
    private static EntityManagerFactory emf;
    private static TransactionManager   txManager;

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

    @BeforeEach
    void limpiar() {
        txManager.enTransaccion(() -> {
            var em = TransactionManager.actual();
            em.createNativeQuery("DELETE FROM auditoria").executeUpdate();
            em.createNativeQuery("DELETE FROM movimientos").executeUpdate();
            em.createNativeQuery("DELETE FROM productos").executeUpdate();
        });
    }

    // ── helpers de construcción ───────────────────────────────────────

    private ProductoService productoServiceCon(AuditoriaRepository auditoriaRepo) {
        var auditSvc = new AuditoriaService(auditoriaRepo, new PermisoChecker(), Clock.systemDefaultZone(), 50, txManager);
        return new ProductoService(new ProductoRepositoryJpa(), new MovimientoRepositoryJpa(),
                new ProductoValidator(), new PermisoChecker(), Clock.systemDefaultZone(),
                txManager, auditSvc);
    }

    private InventarioService inventarioServiceCon(AuditoriaRepository auditoriaRepo,
                                                    MovimientoRepository movimientoRepo) {
        var auditSvc = new AuditoriaService(auditoriaRepo, new PermisoChecker(), Clock.systemDefaultZone(), 50, txManager);
        return new InventarioService(new ProductoRepositoryJpa(), movimientoRepo,
                new StockValidator(), new PermisoChecker(), Clock.systemDefaultZone(),
                txManager, auditSvc);
    }

    private AuditoriaRepository auditoriaFallida() {
        return new AuditoriaRepository() {
            @Override public void registrar(EventoAuditoria e) {
                throw new PersistenciaException(MensajesError.ERROR_TRANSACCION);
            }
            @Override public List<EventoAuditoria> consultar(FiltroAuditoria f, int p, int t) { return List.of(); }
        };
    }

    private MovimientoRepository movimientoFallido() {
        return new MovimientoRepository() {
            @Override public void registrar(Movimiento m) {
                throw new PersistenciaException(MensajesError.ERROR_TRANSACCION);
            }
            @Override public List<Movimiento> historialDe(String c) { return List.of(); }
            @Override public boolean tieneMovimientosRecientes(String c, Duration d, Clock cl) { return false; }
        };
    }

    // ── CASO 1: falla en auditoría revierte el producto ───────────────

    @Test
    @DisplayName("Caso 1: falla en auditoría hace rollback del producto (RF-INV-008)")
    void falla_auditoria_revierte_producto() {
        Producto p = new Producto("TXA001", "Prod Audit Falla", "desc", "Cat",
                new BigDecimal("10.00"), 5);

        assertThatThrownBy(() -> productoServiceCon(auditoriaFallida()).registrar(p, Rol.ADMINISTRADOR))
                .isInstanceOf(PersistenciaException.class);

        Number count = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM productos WHERE codigo = 'TXA001'")
                        .getSingleResult());
        assertThat(count.longValue()).isEqualTo(0L);
    }

    // ── CASO 2: falla en movimiento revierte el stock ─────────────────

    @Test
    @DisplayName("Caso 2: falla en MovimientoRepository hace rollback del stock (RF-INV-008)")
    void falla_movimiento_revierte_stock() {
        // Registrar producto exitosamente
        productoServiceCon(new AuditoriaRepositoryJpa())
                .registrar(new Producto("TXM001", "Prod Mov Falla", "desc", "Cat",
                        new BigDecimal("10.00"), 50), Rol.ADMINISTRADOR);

        // Actualizar stock con movimiento que falla
        assertThatThrownBy(() ->
                inventarioServiceCon(new AuditoriaRepositoryJpa(), movimientoFallido())
                        .actualizarStock("TXM001", TipoMovimiento.ENTRADA, 10, Rol.ADMINISTRADOR))
                .isInstanceOf(PersistenciaException.class);

        // Stock no cambió
        Number stock = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT stock FROM productos WHERE codigo = 'TXM001'")
                        .getSingleResult());
        assertThat(stock.intValue()).isEqualTo(50);

        // No hay movimiento registrado
        Number movCount = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM movimientos WHERE codigo_producto = 'TXM001'")
                        .getSingleResult());
        assertThat(movCount.longValue()).isEqualTo(0L);
    }

    // ── CASO 3: éxito persiste producto + movimiento + auditoría ──────

    @Test
    @DisplayName("Caso 3: operación exitosa persiste las 3 tablas atómicamente (RF-INV-008)")
    void exito_persiste_todas_las_tablas() {
        var auditoriaRepo = new AuditoriaRepositoryJpa();

        // Registrar producto
        productoServiceCon(auditoriaRepo)
                .registrar(new Producto("TXOK01", "Prod OK", "desc", "Cat",
                        new BigDecimal("10.00"), 20), Rol.ADMINISTRADOR);

        // Actualizar stock
        inventarioServiceCon(auditoriaRepo, new MovimientoRepositoryJpa())
                .actualizarStock("TXOK01", TipoMovimiento.ENTRADA, 5, Rol.ADMINISTRADOR);

        // Verificar stock actualizado
        Number stock = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT stock FROM productos WHERE codigo = 'TXOK01'")
                        .getSingleResult());
        assertThat(stock.intValue()).isEqualTo(25);

        // Verificar movimiento
        Number movCount = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM movimientos WHERE codigo_producto = 'TXOK01'")
                        .getSingleResult());
        assertThat(movCount.longValue()).isEqualTo(1L);

        // Verificar auditoría (CREAR_PRODUCTO + ACTUALIZAR_STOCK)
        Number audCount = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM auditoria")
                        .getSingleResult());
        assertThat(audCount.longValue()).isGreaterThanOrEqualTo(2L);
    }
}
