package com.grupob.inventario.integration;

import com.grupob.inventario.config.Configuracion;
import com.grupob.inventario.config.ContextoAplicacion;
import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.exception.PermisoDenegadoException;
import com.grupob.inventario.domain.model.EventoAuditoria;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.persistence.DataSourceFactory;
import com.grupob.inventario.persistence.EntityManagerFactoryProvider;
import com.grupob.inventario.persistence.FlywayMigrator;
import com.grupob.inventario.persistence.TransactionManager;
import com.grupob.inventario.repository.FiltroAuditoria;
import com.grupob.inventario.service.AuditoriaService;
import com.grupob.inventario.service.AutenticacionService;
import com.grupob.inventario.service.FiltroListado;
import com.grupob.inventario.service.InventarioService;
import com.grupob.inventario.service.ProductoService;
import com.grupob.inventario.service.TipoBusqueda;
import com.grupob.inventario.service.UsuarioService;
import com.grupob.inventario.util.MensajesError;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IT end-to-end con Postgres real (Testcontainers) que cubre el flujo completo
 * de los RF-INV-001 a 009 con la pila JPA real.
 *
 * Flujo: login admin → crear gestor → gestor registra producto → gestor
 * actualiza stock → gestor intenta eliminar (FALLA) → admin elimina →
 * verificar historial conservado → auditoría muestra todos los eventos.
 */
@Testcontainers
@DisplayName("FlujoCompletoIT — RF-INV-001..009 con Postgres real")
class FlujoCompletoIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventario_b")
            .withUsername("inventario")
            .withPassword("inventario_dev");

    private static HikariDataSource      ds;
    private static EntityManagerFactory  emf;
    private static TransactionManager    txManager;
    private static ContextoAplicacion    ctx;

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
        ctx       = new ContextoAplicacion(cfg, txManager);
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
            em.createNativeQuery("DELETE FROM usuarios WHERE username != 'admin'").executeUpdate();
        });
    }

    // ── helpers ──────────────────────────────────────────────────────

    private AutenticacionService auth()      { return ctx.getAutenticacionService(); }
    private ProductoService      productos() { return ctx.getProductoService(); }
    private InventarioService    inventario(){ return ctx.getInventarioService(); }
    private UsuarioService       usuarios()  { return ctx.getUsuarioService(); }
    private AuditoriaService     auditoria() { return ctx.getAuditoriaService(); }

    // ── FLUJO COMPLETO ─────────────────────────────────────────────────

    @Test
    @DisplayName("Flujo RF-001..007: login admin → gestor → producto → stock → eliminar → auditoría")
    void flujoCompletoRF001A009() {
        /* ─ 1. Login admin ─────────────────────────────────────── */
        String tokenAdmin = auth().login("admin", "Admin123!");
        assertThat(tokenAdmin).isNotNull();

        /* ─ 2. Admin crea gestor ─────────────────────────────────── */
        usuarios().crearUsuario("gestor1", "Gestor1pass!", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR);

        /* ─ 3. Login como gestor ─────────────────────────────────── */
        String tokenGestor = auth().login("gestor1", "Gestor1pass!");
        Usuario gestor = auth().usuarioActual(tokenGestor);
        assertThat(gestor.getRol()).isEqualTo(Rol.GESTOR_INVENTARIO);

        /* ─ 4. Gestor registra producto ──────────────────────────── */
        Producto prod = new Producto("ITPROD1", "Producto IT", "desc", "Cat",
                new BigDecimal("25.00"), 50);
        productos().registrar(prod, Rol.GESTOR_INVENTARIO);

        /* ─ 5. Gestor actualiza stock — ENTRADA ──────────────────── */
        inventario().actualizarStock("ITPROD1", TipoMovimiento.ENTRADA, 10, Rol.GESTOR_INVENTARIO);

        /* ─ 6. Gestor actualiza stock — SALIDA ───────────────────── */
        inventario().actualizarStock("ITPROD1", TipoMovimiento.SALIDA, 5, Rol.GESTOR_INVENTARIO);

        /* ─ 7. Gestor intenta eliminar → FALLA (sin permiso) ────── */
        assertThatThrownBy(() -> productos().eliminar("ITPROD1", Rol.GESTOR_INVENTARIO))
                .isInstanceOf(PermisoDenegadoException.class)
                .hasMessage(MensajesError.SIN_PERMISOS);

        /* ─ 8. Admin elimina (eliminación lógica) ────────────────── */
        productos().eliminar("ITPROD1", Rol.ADMINISTRADOR);

        /* ─ 9. Producto no aparece en búsquedas activas ─────────── */
        assertThat(productos().buscar("ITPROD1", TipoBusqueda.CODIGO, Rol.ADMINISTRADOR)).isEmpty();
        assertThat(productos().listar(new FiltroListado(null, null, null, null), Rol.ADMINISTRADOR))
                .extracting(Producto::getCodigo).doesNotContain("ITPROD1");

        /* ─ 10. Historial de movimientos se conserva ─────────────── */
        assertThat(inventario().historialDe("ITPROD1")).hasSize(2);

        /* ─ 11. Login fallido con usuario inexistente → username null en auditoría ─ */
        try { auth().login("noexiste", "cualquiera"); } catch (Exception ignored) {}

        /* ─ 12. Auditoría: verificar eventos generados ──────────── */
        auth().logout(tokenAdmin);
        String tokenAdmin2 = auth().login("admin", "Admin123!");

        List<EventoAuditoria> eventos = auditoria().consultar(
                new FiltroAuditoria(null, null, null, null), 0, Rol.ADMINISTRADOR);

        assertThat(eventos.size()).isGreaterThanOrEqualTo(8);

        // Login fallido con usuario inexistente → username=null
        assertThat(eventos).anyMatch(e ->
                e.getTipoEvento() == TipoEvento.LOGIN_FALLIDO && e.getUsername() == null);

        // CREAR_PRODUCTO registrado
        assertThat(eventos).anyMatch(e ->
                e.getTipoEvento() == TipoEvento.CREAR_PRODUCTO
                && "ITPROD1".equals(e.getEntidadAfectada()));

        // ACTUALIZAR_STOCK (dos veces)
        assertThat(eventos.stream()
                .filter(e -> e.getTipoEvento() == TipoEvento.ACTUALIZAR_STOCK)
                .count()).isGreaterThanOrEqualTo(2);

        // ELIMINAR_PRODUCTO registrado
        assertThat(eventos).anyMatch(e ->
                e.getTipoEvento() == TipoEvento.ELIMINAR_PRODUCTO);

        // CREAR_USUARIO para gestor1
        assertThat(eventos).anyMatch(e ->
                e.getTipoEvento() == TipoEvento.CREAR_USUARIO
                && "gestor1".equals(e.getEntidadAfectada()));

        // Orden cronológico descendente
        for (int i = 0; i < eventos.size() - 1; i++) {
            assertThat(eventos.get(i).getFecha())
                    .isAfterOrEqualTo(eventos.get(i + 1).getFecha());
        }

        /* ─ 13. Gestor no puede consultar auditoría ──────────────── */
        assertThatThrownBy(() ->
                auditoria().consultar(new FiltroAuditoria(null, null, null, null), 0, Rol.GESTOR_INVENTARIO))
                .isInstanceOf(PermisoDenegadoException.class)
                .hasMessage(MensajesError.SIN_PERMISOS);

        /* ─ 14. Filtros combinados en auditoría ──────────────────── */
        List<EventoAuditoria> soloProductos = auditoria().consultar(
                new FiltroAuditoria(null, null, null, TipoEvento.CREAR_PRODUCTO), 0, Rol.ADMINISTRADOR);
        assertThat(soloProductos).allMatch(e -> e.getTipoEvento() == TipoEvento.CREAR_PRODUCTO);
    }
}
