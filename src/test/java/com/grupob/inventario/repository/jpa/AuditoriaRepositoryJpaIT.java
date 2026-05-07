package com.grupob.inventario.repository.jpa;

import com.grupob.inventario.config.Configuracion;
import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.model.EventoAuditoria;
import com.grupob.inventario.persistence.DataSourceFactory;
import com.grupob.inventario.persistence.EntityManagerFactoryProvider;
import com.grupob.inventario.persistence.FlywayMigrator;
import com.grupob.inventario.persistence.TransactionManager;
import com.grupob.inventario.repository.FiltroAuditoria;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("AuditoriaRepositoryJpaIT — registro y consulta con filtros (RF-INV-009)")
class AuditoriaRepositoryJpaIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventario_b")
            .withUsername("inventario")
            .withPassword("inventario_dev");

    private static HikariDataSource      dataSource;
    private static EntityManagerFactory  emf;
    private static TransactionManager    txManager;

    private AuditoriaRepositoryJpa repo;

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
        repo = new AuditoriaRepositoryJpa();
        txManager.enTransaccion(() ->
            TransactionManager.actual()
                .createNativeQuery("DELETE FROM auditoria").executeUpdate());
    }

    private EventoAuditoria evento(String username, TipoEvento tipo, String entidad,
                                    String detalle, Instant when) {
        return EventoAuditoria.crear(username, tipo, entidad, detalle,
                Clock.fixed(when, ZoneOffset.UTC));
    }

    // Timestamps bien separados para los tests
    private static final Instant T1 = Instant.parse("2026-05-01T08:00:00Z");
    private static final Instant T2 = Instant.parse("2026-05-01T10:00:00Z");
    private static final Instant T3 = Instant.parse("2026-05-02T09:00:00Z");
    private static final Instant T4 = Instant.parse("2026-05-02T14:00:00Z");
    private static final Instant T5 = Instant.parse("2026-05-03T11:00:00Z");

    private void insertarCincoEventos() {
        txManager.enTransaccion(() -> {
            repo.registrar(evento("admin", TipoEvento.LOGIN_EXITOSO,    null,     null,           T1));
            repo.registrar(evento("admin", TipoEvento.CREAR_PRODUCTO,   "P001",   "nuevo",        T2));
            repo.registrar(evento("gestor",TipoEvento.ACTUALIZAR_STOCK, "P001",   "stock 10→15",  T3));
            repo.registrar(evento("admin", TipoEvento.CAMBIAR_ROL,      "gestor", "GESTOR→ADMIN", T4));
            repo.registrar(evento(null,    TipoEvento.LOGIN_FALLIDO,    null,     "usuario: xxx", T5));
        });
    }

    // ── sin filtros ───────────────────────────────────────────────────

    @Test
    @DisplayName("sin filtros retorna todos los eventos, ordenados fecha DESC")
    void sinFiltros_retornaTodos_ordenDescFecha() {
        insertarCincoEventos();
        List<EventoAuditoria> resultado = txManager.soloLectura(() ->
                repo.consultar(new FiltroAuditoria(null, null, null, null), 0, 10));

        assertThat(resultado).hasSize(5);
        // Verificar orden descendente
        for (int i = 0; i < resultado.size() - 1; i++) {
            assertThat(resultado.get(i).getFecha())
                    .isAfterOrEqualTo(resultado.get(i + 1).getFecha());
        }
        // El más reciente es T5
        assertThat(resultado.get(0).getFecha()).isEqualTo(T5);
    }

    // ── filtro por username ───────────────────────────────────────────

    @Test
    @DisplayName("filtrar por username retorna solo los eventos de ese usuario")
    void filtrarPorUsername_soloEventosDelUsuario() {
        insertarCincoEventos();
        List<EventoAuditoria> resultado = txManager.soloLectura(() ->
                repo.consultar(new FiltroAuditoria(null, null, "admin", null), 0, 10));

        assertThat(resultado).hasSize(3) // LOGIN_EXITOSO, CREAR_PRODUCTO, CAMBIAR_ROL
                .allSatisfy(e -> assertThat(e.getUsername()).isEqualTo("admin"));
    }

    @Test
    @DisplayName("filtrar por username=null incluye eventos con username null")
    void sinFiltroUsername_incluyeNulls() {
        insertarCincoEventos();
        List<EventoAuditoria> resultado = txManager.soloLectura(() ->
                repo.consultar(new FiltroAuditoria(null, null, null, null), 0, 10));
        // El evento de LOGIN_FALLIDO tiene username=null y debe aparecer
        assertThat(resultado).anyMatch(e -> e.getUsername() == null);
    }

    // ── filtro por rango de fechas ────────────────────────────────────

    @Test
    @DisplayName("filtrar por rango de fechas retorna solo los del rango")
    void filtrarPorRangoFechas() {
        insertarCincoEventos();
        // Rango: solo el 1 de mayo (T1 y T2)
        Instant desde = Instant.parse("2026-05-01T00:00:00Z");
        Instant hasta = Instant.parse("2026-05-01T23:59:59Z");

        List<EventoAuditoria> resultado = txManager.soloLectura(() ->
                repo.consultar(new FiltroAuditoria(desde, hasta, null, null), 0, 10));

        assertThat(resultado).hasSize(2)
                .allSatisfy(e -> {
                    assertThat(e.getFecha()).isAfterOrEqualTo(desde);
                    assertThat(e.getFecha()).isBeforeOrEqualTo(hasta);
                });
    }

    // ── filtro por tipo de evento ─────────────────────────────────────

    @Test
    @DisplayName("filtrar por tipo de evento retorna solo ese tipo")
    void filtrarPorTipoEvento() {
        insertarCincoEventos();
        List<EventoAuditoria> resultado = txManager.soloLectura(() ->
                repo.consultar(new FiltroAuditoria(null, null, null, TipoEvento.LOGIN_FALLIDO), 0, 10));

        assertThat(resultado).hasSize(1)
                .extracting(EventoAuditoria::getTipoEvento)
                .containsOnly(TipoEvento.LOGIN_FALLIDO);
    }

    // ── filtros combinados (AND) ──────────────────────────────────────

    @Test
    @DisplayName("filtros combinados: username AND tipo AND rango — intersección correcta")
    void filtrosCombinados_AND() {
        insertarCincoEventos();
        // Solo eventos de 'admin' en el día 1 de mayo de tipo no LOGIN → solo CREAR_PRODUCTO
        Instant desde = Instant.parse("2026-05-01T00:00:00Z");
        Instant hasta = Instant.parse("2026-05-01T23:59:59Z");

        List<EventoAuditoria> resultado = txManager.soloLectura(() ->
                repo.consultar(
                        new FiltroAuditoria(desde, hasta, "admin", TipoEvento.CREAR_PRODUCTO),
                        0, 10));

        assertThat(resultado).hasSize(1)
                .extracting(EventoAuditoria::getTipoEvento)
                .containsOnly(TipoEvento.CREAR_PRODUCTO);
    }

    // ── sin resultados con filtros ────────────────────────────────────

    @Test
    @DisplayName("filtros sin resultados retornan lista vacía, no excepción")
    void filtrosSinResultados_listaVacia() {
        insertarCincoEventos();
        List<EventoAuditoria> resultado = txManager.soloLectura(() ->
                repo.consultar(
                        new FiltroAuditoria(null, null, "inexistente", null),
                        0, 10));

        assertThat(resultado).isEmpty();
    }

    // ── paginación ────────────────────────────────────────────────────

    @Test
    @DisplayName("paginación: tamaño 2, página 0 retorna los 2 más recientes")
    void paginacion_retornaSubconjunto() {
        insertarCincoEventos();
        List<EventoAuditoria> pagina0 = txManager.soloLectura(() ->
                repo.consultar(new FiltroAuditoria(null, null, null, null), 0, 2));
        List<EventoAuditoria> pagina1 = txManager.soloLectura(() ->
                repo.consultar(new FiltroAuditoria(null, null, null, null), 1, 2));

        assertThat(pagina0).hasSize(2);
        assertThat(pagina1).hasSize(2);
        // Sin solapamiento
        assertThat(pagina0).doesNotContainAnyElementsOf(pagina1);
    }
}
