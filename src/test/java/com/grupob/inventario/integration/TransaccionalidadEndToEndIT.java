package com.grupob.inventario.integration;

import com.grupob.inventario.config.Configuracion;
import com.grupob.inventario.config.ContextoAplicacion;
import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.exception.PersistenciaException;
import com.grupob.inventario.domain.model.EventoAuditoria;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.persistence.DataSourceFactory;
import com.grupob.inventario.persistence.EntityManagerFactoryProvider;
import com.grupob.inventario.persistence.FlywayMigrator;
import com.grupob.inventario.persistence.GestorTransacciones;
import com.grupob.inventario.persistence.TransactionManager;
import com.grupob.inventario.repository.AuditoriaRepository;
import com.grupob.inventario.repository.FiltroAuditoria;
import com.grupob.inventario.repository.jpa.AuditoriaRepositoryJpa;
import com.grupob.inventario.repository.jpa.MovimientoRepositoryJpa;
import com.grupob.inventario.repository.jpa.ProductoRepositoryJpa;
import com.grupob.inventario.security.PermisoChecker;
import com.grupob.inventario.service.AuditoriaService;
import com.grupob.inventario.service.ProductoService;
import com.grupob.inventario.util.MensajesError;
import com.grupob.inventario.validation.ProductoValidator;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verificación de transaccionalidad: el número de productos en BD siempre debe
 * coincidir con el número de eventos CREAR_PRODUCTO en auditoría.
 *
 * Si la transaccionalidad funciona, cada producto registrado tiene exactamente
 * un evento de auditoría, y cada rollback revierte ambos. Los conteos siempre coinciden.
 */
@Testcontainers
@DisplayName("TransaccionalidadEndToEndIT — RF-INV-008 consistencia producto↔auditoría")
class TransaccionalidadEndToEndIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventario_b")
            .withUsername("inventario")
            .withPassword("inventario_dev");

    private static HikariDataSource      ds;
    private static EntityManagerFactory  emf;
    private static TransactionManager    txManager;

    @BeforeAll
    static void init() {
        Properties props = new Properties();
        props.setProperty("db.url",      postgres.getJdbcUrl());
        props.setProperty("db.usuario",  postgres.getUsername());
        props.setProperty("db.password", postgres.getPassword());
        Configuracion cfg = new Configuracion(props, Map.of());
        ds        = DataSourceFactory.crear(cfg);
        FlywayMigrator.migrar(ds);
        emf       = EntityManagerFactoryProvider.crear(ds, cfg);
        txManager = new TransactionManager(emf);
    }

    @AfterAll
    static void teardown() {
        if (emf != null) emf.close();
        if (ds  != null) ds.close();
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

    // ── Caso 1: falla en auditoría revierte el producto ───────────────

    @Test
    @DisplayName("Caso 1: rollback — producto NO persiste si auditoría falla")
    void falla_auditoria_revierte_producto() {
        AuditoriaRepository auditoriaFallida = new AuditoriaRepository() {
            @Override public void registrar(EventoAuditoria e) {
                throw new PersistenciaException(MensajesError.ERROR_TRANSACCION);
            }
            @Override public List<EventoAuditoria> consultar(FiltroAuditoria f, int p, int t) { return List.of(); }
        };

        ProductoService svc = armarProductoService(auditoriaFallida);
        Producto p = new Producto("TXEND1", "Prod TX", "desc", "Cat", new BigDecimal("10.00"), 5);

        assertThatThrownBy(() -> svc.registrar(p, Rol.ADMINISTRADOR))
                .isInstanceOf(PersistenciaException.class);

        Number count = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM productos WHERE codigo = 'TXEND1'")
                        .getSingleResult());
        assertThat(count.longValue()).isEqualTo(0L);
    }

    // ── Caso 2: éxito persiste todo consistentemente ──────────────────

    @Test
    @DisplayName("Caso 2: productos en BD = eventos CREAR_PRODUCTO en auditoría (20 ops exitosas)")
    void consistencia_productos_vs_auditoria_exitosas() {
        AuditoriaRepositoryJpa auditoriaRepo = new AuditoriaRepositoryJpa();
        ProductoService svc = armarProductoService(auditoriaRepo);

        int total = 20;
        for (int i = 0; i < total; i++) {
            String codigo = "CONSIST" + String.format("%03d", i);
            svc.registrar(new Producto(codigo, "Prod " + i, "", "Cat",
                    new BigDecimal("5.00"), 0), Rol.ADMINISTRADOR);
        }

        Number prodCount = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM productos WHERE codigo LIKE 'CONSIST%'")
                        .getSingleResult());
        Number audCount = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM auditoria WHERE tipo_evento = 'CREAR_PRODUCTO'")
                        .getSingleResult());

        assertThat(prodCount.longValue()).isEqualTo(total);
        assertThat(audCount.longValue()).isEqualTo(total);
        // Invariante: siempre deben coincidir
        assertThat(prodCount.longValue()).isEqualTo(audCount.longValue());
    }

    // ── Caso 3: mezcla éxitos + fallos — conteos siempre coinciden ────

    @Test
    @DisplayName("Caso 3: mezcla éxito+fallo — conteo producto = conteo auditoría SIEMPRE")
    void consistencia_con_mezcla_exitos_y_fallos() {
        AtomicInteger fallos = new AtomicInteger(0);

        // AuditoriaRepository que falla en registros pares (simula fallos aleatorios)
        AtomicInteger llamadas = new AtomicInteger(0);
        AuditoriaRepository auditoriaSemiFallida = new AuditoriaRepository() {
            @Override public void registrar(EventoAuditoria e) {
                if (llamadas.incrementAndGet() % 3 == 0) {
                    throw new PersistenciaException("Falla simulada");
                }
                new AuditoriaRepositoryJpa().registrar(e);
            }
            @Override public List<EventoAuditoria> consultar(FiltroAuditoria f, int p, int t) {
                return new AuditoriaRepositoryJpa().consultar(f, p, t);
            }
        };

        ProductoService svc = armarProductoService(auditoriaSemiFallida);

        int total = 30;
        for (int i = 0; i < total; i++) {
            String codigo = "MIX" + String.format("%03d", i);
            try {
                svc.registrar(new Producto(codigo, "Prod " + i, "", "Cat",
                        new BigDecimal("5.00"), 0), Rol.ADMINISTRADOR);
            } catch (Exception e) {
                fallos.incrementAndGet();
            }
        }

        // Sin importar cuántos fallos hubo, los conteos DEBEN coincidir
        Number prodCount = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM productos WHERE codigo LIKE 'MIX%'")
                        .getSingleResult());
        Number audCount = (Number) txManager.soloLectura(() ->
                TransactionManager.actual()
                        .createNativeQuery("SELECT COUNT(*) FROM auditoria WHERE tipo_evento = 'CREAR_PRODUCTO'")
                        .getSingleResult());

        assertThat(prodCount.longValue()).isEqualTo(audCount.longValue());
        assertThat(prodCount.longValue()).isEqualTo(total - fallos.get());
    }

    // ── helper ────────────────────────────────────────────────────────

    private ProductoService armarProductoService(AuditoriaRepository auditoriaRepo) {
        var permisos   = new PermisoChecker();
        var auditSvc   = new AuditoriaService(auditoriaRepo, permisos,
                java.time.Clock.systemDefaultZone(), 50, txManager);
        return new ProductoService(new ProductoRepositoryJpa(), new MovimientoRepositoryJpa(),
                new ProductoValidator(), permisos,
                java.time.Clock.systemDefaultZone(), txManager, auditSvc);
    }
}
