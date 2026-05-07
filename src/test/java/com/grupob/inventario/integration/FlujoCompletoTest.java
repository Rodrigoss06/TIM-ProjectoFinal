package com.grupob.inventario.integration;

// App movida a AppArranqueIT (requiere Docker/Testcontainers)
import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.exception.CuentaBloqueadaException;
import com.grupob.inventario.domain.exception.CredencialesInvalidasException;
import com.grupob.inventario.domain.exception.PermisoDenegadoException;
import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.persistence.TransactionManagerFake;
import com.grupob.inventario.repository.memory.AuditoriaRepositoryEnMemoria;
import com.grupob.inventario.repository.memory.MovimientoRepositoryEnMemoria;
import com.grupob.inventario.repository.memory.ProductoRepositoryEnMemoria;
import com.grupob.inventario.repository.memory.UsuarioRepositoryEnMemoria;
import com.grupob.inventario.security.PasswordHasher;
import com.grupob.inventario.security.PermisoChecker;
import com.grupob.inventario.security.SesionManager;
import com.grupob.inventario.service.AuditoriaService;
import com.grupob.inventario.service.AutenticacionService;
import com.grupob.inventario.service.FiltroListado;
import com.grupob.inventario.service.InventarioService;
import com.grupob.inventario.service.ProductoService;
import com.grupob.inventario.service.TipoBusqueda;
import com.grupob.inventario.service.UsuarioService;
import com.grupob.inventario.util.MensajesError;
import com.grupob.inventario.validation.ProductoValidator;
import com.grupob.inventario.validation.StockValidator;
import com.grupob.inventario.validation.UsuarioValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Integración end-to-end — flujos por RF + smoke tests")
class FlujoCompletoTest {

    // ── Constantes de tiempo y hash ────────────────────────────────────

    private static final Instant T0 = Instant.parse("2026-04-29T10:00:00Z");
    private static String ADMIN_HASH;
    private static String GESTOR_HASH;

    @BeforeAll
    static void hashearPasswordsUnaVez() {
        PasswordHasher h = new PasswordHasher();
        ADMIN_HASH  = h.hashear("Admin123!");
        GESTOR_HASH = h.hashear("Gestor123");
    }

    // ── Reloj mutable (reutilizable en cada test) ──────────────────────

    private Instant tiempoActual;
    private final Clock relojMutable = new Clock() {
        @Override public ZoneId getZone()           { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId z)   { return this; }
        @Override public Instant instant()          { return tiempoActual; }
    };

    // ── Servicios ──────────────────────────────────────────────────────

    private ProductoRepositoryEnMemoria productoRepo;
    private UsuarioRepositoryEnMemoria  usuarioRepo;
    private MovimientoRepositoryEnMemoria movimientoRepo;
    private ProductoService      productoService;
    private InventarioService    inventarioService;
    private AutenticacionService autenticacionService;
    private UsuarioService       usuarioService;

    @BeforeEach
    void setUp() {
        tiempoActual   = T0;
        productoRepo   = new ProductoRepositoryEnMemoria();
        usuarioRepo    = new UsuarioRepositoryEnMemoria();
        movimientoRepo = new MovimientoRepositoryEnMemoria();

        PasswordHasher  hasher   = new PasswordHasher();
        SesionManager   sesiones = new SesionManager(relojMutable);
        PermisoChecker  permisos = new PermisoChecker();
        var txFake               = new TransactionManagerFake();
        var auditoriaRepo        = new AuditoriaRepositoryEnMemoria();
        var auditSvc             = new AuditoriaService(auditoriaRepo, permisos, relojMutable, 50, txFake);

        productoService      = new ProductoService(productoRepo, movimientoRepo,
                                   new ProductoValidator(), permisos, relojMutable, txFake, auditSvc);
        inventarioService    = new InventarioService(productoRepo, movimientoRepo,
                                   new StockValidator(), permisos, relojMutable, txFake, auditSvc);
        autenticacionService = new AutenticacionService(usuarioRepo, hasher, sesiones, relojMutable, txFake, auditSvc);
        usuarioService       = new UsuarioService(usuarioRepo, hasher,
                                   new UsuarioValidator(), permisos, sesiones, txFake, auditSvc);

        usuarioRepo.guardar(new Usuario("admin",  ADMIN_HASH,  Rol.ADMINISTRADOR));
        usuarioRepo.guardar(new Usuario("gestor", GESTOR_HASH, Rol.GESTOR_INVENTARIO));
    }

    // ══════════════════════════════════════════════════════════════════
    //  FLUJO 1 — Admin completo: crear → buscar → stock → listar →
    //             eliminar → historial conservado
    // ══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("flujoAdminCompleto: CRUD completo + historial persiste tras eliminación")
    void flujoAdminCompleto() {
        // Login
        String token = autenticacionService.login("admin", "Admin123!");
        assertThat(token).isNotNull();

        // Registrar
        Producto p = new Producto("INT001", "Producto Integración", "desc", "Cat",
                new BigDecimal("99.99"), 100);
        productoService.registrar(p, Rol.ADMINISTRADOR);

        // Buscar por código
        List<Producto> encontrados = productoService.buscar("INT001", TipoBusqueda.CODIGO, Rol.ADMINISTRADOR);
        assertThat(encontrados).hasSize(1)
                .extracting(Producto::getCodigo).containsExactly("INT001");

        // Buscar por nombre parcial
        assertThat(productoService.buscar("Integr", TipoBusqueda.NOMBRE, Rol.ADMINISTRADOR))
                .hasSize(1);

        // Actualizar stock: entrada +50 → 150, salida -20 → 130
        inventarioService.actualizarStock("INT001", TipoMovimiento.ENTRADA, 50, Rol.ADMINISTRADOR);
        inventarioService.actualizarStock("INT001", TipoMovimiento.SALIDA,  20, Rol.ADMINISTRADOR);
        assertThat(productoRepo.buscarPorCodigo("INT001"))
                .isPresent().hasValueSatisfying(pr ->
                        assertThat(pr.getStock()).isEqualTo(130));

        // Listar → aparece
        assertThat(productoService.listar(new FiltroListado(null, null, null, null), Rol.ADMINISTRADOR))
                .extracting(Producto::getCodigo).contains("INT001");

        // Eliminar
        productoService.eliminar("INT001", Rol.ADMINISTRADOR);

        // No aparece en búsquedas (8.4 Notion)
        assertThat(productoService.buscar("INT001", TipoBusqueda.CODIGO, Rol.ADMINISTRADOR)).isEmpty();
        assertThat(productoService.buscar("Integr", TipoBusqueda.NOMBRE, Rol.ADMINISTRADOR)).isEmpty();
        assertThat(productoService.listar(new FiltroListado(null, null, null, null), Rol.ADMINISTRADOR))
                .extracting(Producto::getCodigo).doesNotContain("INT001");

        // Historial conservado con 2 movimientos (8.4 Notion)
        assertThat(inventarioService.historialDe("INT001")).hasSize(2);
    }

    // ══════════════════════════════════════════════════════════════════
    //  FLUJO 2 — Gestor bloqueado en operación prohibida (8.4 Notion)
    // ══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("flujoGestorBloqueado: intenta eliminar → SIN_PERMISOS; puede actualizar stock")
    void flujoGestorBloqueado() {
        Producto p = new Producto("G001", "Prod Gestor", "desc", "Cat",
                new BigDecimal("20.00"), 50);
        productoService.registrar(p, Rol.GESTOR_INVENTARIO); // gestor puede registrar

        // Eliminar bloqueado con mensaje exacto
        assertThatThrownBy(() -> productoService.eliminar("G001", Rol.GESTOR_INVENTARIO))
                .isInstanceOf(PermisoDenegadoException.class)
                .hasMessage(MensajesError.SIN_PERMISOS);

        // Producto NO fue eliminado
        assertThat(productoService.buscar("G001", TipoBusqueda.CODIGO, Rol.GESTOR_INVENTARIO))
                .hasSize(1);

        // Actualizar stock sí puede
        inventarioService.actualizarStock("G001", TipoMovimiento.ENTRADA, 10, Rol.GESTOR_INVENTARIO);
        assertThat(productoRepo.buscarPorCodigo("G001"))
                .isPresent().hasValueSatisfying(pr ->
                        assertThat(pr.getStock()).isEqualTo(60));
    }

    // ══════════════════════════════════════════════════════════════════
    //  FLUJO 3 — Lockout 3 intentos → 4to bloqueado → 16 min → ok (8.6)
    // ══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("flujoAutenticacionLockout: 3 fallos → bloqueado → 4to correcto falla → 16 min → ok")
    void flujoAutenticacionLockout() {
        tiempoActual = T0;

        // 2 intentos fallidos → CredencialesInvalidasException
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> autenticacionService.login("admin", "mal"))
                    .isInstanceOf(CredencialesInvalidasException.class)
                    .hasMessage(MensajesError.CREDENCIALES_INVALIDAS);
        }

        // 3er intento → CuentaBloqueadaException (mensaje con 15 min)
        assertThatThrownBy(() -> autenticacionService.login("admin", "mal"))
                .isInstanceOf(CuentaBloqueadaException.class)
                .hasMessage(String.format(MensajesError.CUENTA_BLOQUEADA_FMT, 15));

        // 4to intento con password CORRECTA dentro de 15 min → sigue bloqueado (8.6)
        tiempoActual = T0.plus(Duration.ofMinutes(14));
        assertThatThrownBy(() -> autenticacionService.login("admin", "Admin123!"))
                .isInstanceOf(CuentaBloqueadaException.class);

        // A los 16 min → desbloqueado automáticamente
        tiempoActual = T0.plus(Duration.ofMinutes(16));
        String token = autenticacionService.login("admin", "Admin123!");
        assertThat(token).isNotNull();

        // Contador reseteado tras login exitoso
        assertThat(usuarioRepo.buscarPorUsername("admin"))
                .isPresent()
                .hasValueSatisfying(u -> assertThat(u.getIntentosFallidos()).isEqualTo(0));
    }

    // ══════════════════════════════════════════════════════════════════
    //  FLUJO 4 — Único admin: no se puede degradar/desactivar (8.7)
    // ══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("flujoUltimoAdmin: único admin no puede desactivarse → crear admin2 → ahora sí")
    void flujoUltimoAdmin() {
        // Eliminar gestor para simplificar (solo deja a admin)
        usuarioService.desactivar("gestor", Rol.ADMINISTRADOR);

        // Intentar desactivar al único admin → ULTIMO_ADMIN
        assertThatThrownBy(() -> usuarioService.desactivar("admin", Rol.ADMINISTRADOR))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.ULTIMO_ADMIN);

        // Intentar degradar al único admin → ULTIMO_ADMIN
        assertThatThrownBy(() ->
                usuarioService.cambiarRol("admin", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.ULTIMO_ADMIN);

        // Crear admin2 → ahora sí puede desactivar admin1
        usuarioService.crearUsuario("admin2", "Admin2pass!", Rol.ADMINISTRADOR, Rol.ADMINISTRADOR);

        assertThatCode(() -> usuarioService.desactivar("admin", Rol.ADMINISTRADOR))
                .doesNotThrowAnyException();

        assertThat(usuarioRepo.buscarPorUsername("admin"))
                .isPresent()
                .hasValueSatisfying(u -> assertThat(u.isActivo()).isFalse());
    }

}
