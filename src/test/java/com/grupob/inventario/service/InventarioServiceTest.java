package com.grupob.inventario.service;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.exception.PermisoDenegadoException;
import com.grupob.inventario.domain.exception.ProductoNoEncontradoException;
import com.grupob.inventario.domain.exception.StockInsuficienteException;
import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.domain.model.Movimiento;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.persistence.TransactionManagerFake;
import com.grupob.inventario.repository.memory.AuditoriaRepositoryEnMemoria;
import com.grupob.inventario.repository.memory.MovimientoRepositoryEnMemoria;
import com.grupob.inventario.repository.memory.ProductoRepositoryEnMemoria;
import com.grupob.inventario.security.PermisoChecker;
import com.grupob.inventario.util.MensajesError;
import com.grupob.inventario.validation.StockValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InventarioService — RF-INV-003 + superficie de ataque (sección 8.3)")
class InventarioServiceTest {

    private static final Instant T0 = Instant.parse("2026-04-29T10:00:00Z");
    private static final Clock RELOJ = Clock.fixed(T0, ZoneOffset.UTC);

    private ProductoRepositoryEnMemoria productoRepo;
    private MovimientoRepositoryEnMemoria movimientoRepo;
    private InventarioService service;

    @BeforeEach
    void setUp() {
        productoRepo   = new ProductoRepositoryEnMemoria();
        movimientoRepo = new MovimientoRepositoryEnMemoria();
        var txFake     = new TransactionManagerFake();
        var permisos   = new PermisoChecker();
        var auditSvc   = new AuditoriaService(new AuditoriaRepositoryEnMemoria(), permisos, RELOJ, 50, txFake);
        service = new InventarioService(productoRepo, movimientoRepo,
                new StockValidator(), permisos, RELOJ, txFake, auditSvc);
    }

    private Producto productoConStock(String codigo, int stock) {
        Producto p = new Producto(codigo, "Producto " + codigo, "desc", "Cat",
                new BigDecimal("10.00"), stock);
        productoRepo.guardar(p);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RF-INV-003 — ACTUALIZAR STOCK: ENTRADAS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("RF-003: ENTRADA incrementa el stock correctamente")
    void actualizarStock_entrada_incrementaStock() {
        productoConStock("P001", 50);
        service.actualizarStock("P001", TipoMovimiento.ENTRADA, 30, Rol.ADMINISTRADOR);
        assertThat(productoRepo.buscarPorCodigo("P001"))
                .isPresent().hasValueSatisfying(p -> assertThat(p.getStock()).isEqualTo(80));
    }

    @Test
    @DisplayName("RF-003: movimiento ENTRADA registrado con stockAnterior y stockNuevo correctos")
    void actualizarStock_entrada_movimientoRegistrado() {
        productoConStock("P001", 50);
        service.actualizarStock("P001", TipoMovimiento.ENTRADA, 30, Rol.ADMINISTRADOR);

        List<Movimiento> historial = service.historialDe("P001");
        assertThat(historial).hasSize(1);
        Movimiento m = historial.get(0);
        assertThat(m.tipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(m.cantidad()).isEqualTo(30);
        assertThat(m.stockAnterior()).isEqualTo(50);
        assertThat(m.stockNuevo()).isEqualTo(80);
        assertThat(m.fecha()).isEqualTo(T0);
        assertThat(m.codigoProducto()).isEqualTo("P001");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RF-INV-003 — ACTUALIZAR STOCK: SALIDAS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("RF-003: SALIDA decrementa el stock correctamente")
    void actualizarStock_salida_decrementaStock() {
        productoConStock("P001", 100);
        service.actualizarStock("P001", TipoMovimiento.SALIDA, 40, Rol.ADMINISTRADOR);
        assertThat(productoRepo.buscarPorCodigo("P001"))
                .isPresent().hasValueSatisfying(p -> assertThat(p.getStock()).isEqualTo(60));
    }

    @Test
    @DisplayName("8.3: SALIDA que deja stock en 0 es válida")
    void actualizarStock_salidaDejaStockCero_permitido() {
        productoConStock("P001", 50);
        service.actualizarStock("P001", TipoMovimiento.SALIDA, 50, Rol.ADMINISTRADOR);
        assertThat(productoRepo.buscarPorCodigo("P001"))
                .isPresent().hasValueSatisfying(p -> assertThat(p.getStock()).isEqualTo(0));
    }

    @Test
    @DisplayName("8.3: SALIDA igual al stock disponible (stock - cantidad = 0) se permite")
    void actualizarStock_salidaIgualAlStock_permitido() {
        productoConStock("P001", 7);
        service.actualizarStock("P001", TipoMovimiento.SALIDA, 7, Rol.ADMINISTRADOR);
        assertThat(productoRepo.buscarPorCodigo("P001"))
                .isPresent().hasValueSatisfying(p -> assertThat(p.getStock()).isEqualTo(0));
    }

    @Test
    @DisplayName("8.3: SALIDA = stock + 1 → StockInsuficienteException con mensaje exacto y disponible real")
    void actualizarStock_salidaStockMasUno_excepcion() {
        productoConStock("P001", 10);
        assertThatThrownBy(() -> service.actualizarStock("P001", TipoMovimiento.SALIDA, 11, Rol.ADMINISTRADOR))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessage(String.format(MensajesError.STOCK_INSUFICIENTE_FMT, 10))
                .satisfies(e -> assertThat(((StockInsuficienteException) e).getStockDisponible()).isEqualTo(10));
    }

    @Test
    @DisplayName("RF-003/8.3: movimiento SALIDA registrado correctamente")
    void actualizarStock_salida_movimientoRegistrado() {
        productoConStock("P001", 100);
        service.actualizarStock("P001", TipoMovimiento.SALIDA, 25, Rol.ADMINISTRADOR);

        Movimiento m = service.historialDe("P001").get(0);
        assertThat(m.tipo()).isEqualTo(TipoMovimiento.SALIDA);
        assertThat(m.cantidad()).isEqualTo(25);
        assertThat(m.stockAnterior()).isEqualTo(100);
        assertThat(m.stockNuevo()).isEqualTo(75);
        assertThat(m.fecha()).isEqualTo(T0);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RF-INV-003 — VALIDACIÓN DE CANTIDAD
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("RF-003/8.3: cantidad = 0 → ValidacionException CANTIDAD_NO_POSITIVA")
    void actualizarStock_cantidadCero_excepcion() {
        productoConStock("P001", 50);
        assertThatThrownBy(() -> service.actualizarStock("P001", TipoMovimiento.ENTRADA, 0, Rol.ADMINISTRADOR))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CANTIDAD_NO_POSITIVA);
    }

    @Test
    @DisplayName("8.3: cantidad negativa → ValidacionException CANTIDAD_NO_POSITIVA")
    void actualizarStock_cantidadNegativa_excepcion() {
        productoConStock("P001", 50);
        assertThatThrownBy(() -> service.actualizarStock("P001", TipoMovimiento.SALIDA, -5, Rol.ADMINISTRADOR))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CANTIDAD_NO_POSITIVA);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RF-INV-003 — PRODUCTO NO ENCONTRADO / ELIMINADO
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("RF-003/8.3: producto inexistente → ProductoNoEncontradoException con mensaje exacto")
    void actualizarStock_productoNoExiste_excepcion() {
        assertThatThrownBy(() -> service.actualizarStock("NOEXISTE", TipoMovimiento.ENTRADA, 5, Rol.ADMINISTRADOR))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessage(MensajesError.PRODUCTO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("8.3: producto eliminado lógicamente → mismo mensaje que no encontrado")
    void actualizarStock_productoEliminado_mismaMensajeQueNoEncontrado() {
        Producto p = productoConStock("P001", 50);
        p.eliminar();
        productoRepo.guardar(p);

        assertThatThrownBy(() -> service.actualizarStock("P001", TipoMovimiento.ENTRADA, 5, Rol.ADMINISTRADOR))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessage(MensajesError.PRODUCTO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("8.3: stock insuficiente — el disponible en la excepción refleja el stock real")
    void actualizarStock_stockInsuficiente_disponibleEnExcepcion() {
        productoConStock("P001", 3);
        assertThatThrownBy(() -> service.actualizarStock("P001", TipoMovimiento.SALIDA, 10, Rol.ADMINISTRADOR))
                .isInstanceOf(StockInsuficienteException.class)
                .satisfies(e -> {
                    StockInsuficienteException sie = (StockInsuficienteException) e;
                    org.assertj.core.api.Assertions.assertThat(sie.getStockDisponible()).isEqualTo(3);
                    org.assertj.core.api.Assertions.assertThat(sie.getMessage())
                            .isEqualTo(String.format(MensajesError.STOCK_INSUFICIENTE_FMT, 3));
                });
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RF-INV-003 — HISTORIAL Y MÚLTIPLES MOVIMIENTOS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("RF-003: múltiples movimientos se acumulan en el historial")
    void actualizarStock_multipleMovimientos_historialCompleto() {
        productoConStock("P001", 100);
        service.actualizarStock("P001", TipoMovimiento.ENTRADA, 50, Rol.ADMINISTRADOR);
        service.actualizarStock("P001", TipoMovimiento.SALIDA, 30, Rol.ADMINISTRADOR);

        List<Movimiento> historial = service.historialDe("P001");
        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).stockAnterior()).isEqualTo(100);
        assertThat(historial.get(0).stockNuevo()).isEqualTo(150);
        assertThat(historial.get(1).stockAnterior()).isEqualTo(150);
        assertThat(historial.get(1).stockNuevo()).isEqualTo(120);
    }

    @Test
    @DisplayName("RF-003: historialDe retorna solo movimientos del producto indicado")
    void historialDe_soloDelProducto() {
        productoConStock("P001", 100);
        productoConStock("P002", 50);
        service.actualizarStock("P001", TipoMovimiento.ENTRADA, 10, Rol.ADMINISTRADOR);
        service.actualizarStock("P002", TipoMovimiento.ENTRADA, 5, Rol.ADMINISTRADOR);

        assertThat(service.historialDe("P001")).hasSize(1)
                .allSatisfy(m -> assertThat(m.codigoProducto()).isEqualTo("P001"));
        assertThat(service.historialDe("P002")).hasSize(1)
                .allSatisfy(m -> assertThat(m.codigoProducto()).isEqualTo("P002"));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PERMISOS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Permisos: gestor puede actualizar stock")
    void actualizarStock_gestor_puede() {
        productoConStock("P001", 50);
        service.actualizarStock("P001", TipoMovimiento.ENTRADA, 10, Rol.GESTOR_INVENTARIO);
        assertThat(productoRepo.buscarPorCodigo("P001"))
                .isPresent().hasValueSatisfying(p -> assertThat(p.getStock()).isEqualTo(60));
    }

    @Test
    @DisplayName("Permisos: admin puede actualizar stock")
    void actualizarStock_admin_puede() {
        productoConStock("P001", 50);
        service.actualizarStock("P001", TipoMovimiento.SALIDA, 10, Rol.ADMINISTRADOR);
        assertThat(productoRepo.buscarPorCodigo("P001"))
                .isPresent().hasValueSatisfying(p -> assertThat(p.getStock()).isEqualTo(40));
    }
}
