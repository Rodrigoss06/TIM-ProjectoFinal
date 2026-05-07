package com.grupob.inventario.service;

import com.grupob.inventario.domain.enums.EstadoStock;
import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.exception.PermisoDenegadoException;
import com.grupob.inventario.domain.exception.ProductoDuplicadoException;
import com.grupob.inventario.domain.exception.ProductoNoEncontradoException;
import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.domain.model.Movimiento;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.model.EventoAuditoria;
import com.grupob.inventario.persistence.TransactionManagerFake;
import com.grupob.inventario.repository.FiltroAuditoria;
import com.grupob.inventario.repository.memory.AuditoriaRepositoryEnMemoria;
import com.grupob.inventario.repository.memory.MovimientoRepositoryEnMemoria;
import com.grupob.inventario.repository.memory.ProductoRepositoryEnMemoria;
import com.grupob.inventario.security.PermisoChecker;
import com.grupob.inventario.util.MensajesError;
import com.grupob.inventario.validation.ProductoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProductoService — RF-INV-001/002/004/005 + superficie de ataque (sección 8)")
class ProductoServiceTest {

    private ProductoRepositoryEnMemoria productoRepo;
    private MovimientoRepositoryEnMemoria movimientoRepo;
    private AuditoriaRepositoryEnMemoria auditoriaRepo;
    private ProductoService service;

    @BeforeEach
    void setUp() {
        productoRepo   = new ProductoRepositoryEnMemoria();
        movimientoRepo = new MovimientoRepositoryEnMemoria();
        auditoriaRepo  = new AuditoriaRepositoryEnMemoria();
        var txFake     = new TransactionManagerFake();
        var permisos   = new PermisoChecker();
        var auditSvc   = new AuditoriaService(auditoriaRepo, permisos, Clock.systemDefaultZone(), 50, txFake);
        service = new ProductoService(productoRepo, movimientoRepo,
                new ProductoValidator(), permisos, Clock.systemDefaultZone(), txFake, auditSvc);
    }

    private Producto p(String codigo, String nombre, String categoria, int stock) {
        return new Producto(codigo, nombre, "desc", categoria, new BigDecimal("10.00"), stock);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RF-INV-001 — REGISTRAR PRODUCTO
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("RF-001: registro exitoso — producto guardado y consultable")
    void registrar_productoValido_guardado() {
        service.registrar(p("P001", "Arroz", "Alimentos", 100), Rol.ADMINISTRADOR);
        assertThat(service.buscar("P001", TipoBusqueda.CODIGO, Rol.ADMINISTRADOR)).hasSize(1);

        // RF-INV-009: debe haber un evento CREAR_PRODUCTO en auditoría
        var eventos = auditoriaRepo.consultar(new FiltroAuditoria(null, null, null, TipoEvento.CREAR_PRODUCTO), 0, 10);
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).getEntidadAfectada()).isEqualTo("P001");
    }

    @Test
    @DisplayName("RF-001: código duplicado → ProductoDuplicadoException con mensaje exacto")
    void registrar_codigoDuplicado_excepcion() {
        service.registrar(p("P001", "Arroz", "Alimentos", 10), Rol.ADMINISTRADOR);
        assertThatThrownBy(() -> service.registrar(p("P001", "Fideos", "Alimentos", 5), Rol.ADMINISTRADOR))
                .isInstanceOf(ProductoDuplicadoException.class)
                .hasMessage(MensajesError.CODIGO_DUPLICADO);
    }

    @Test
    @DisplayName("8.1: código duplicado case-insensitive (P001 vs p001) → lanza excepción")
    void registrar_codigoDuplicadoCaseInsensitive_excepcion() {
        service.registrar(p("P001", "Arroz", "Alimentos", 10), Rol.ADMINISTRADOR);
        assertThatThrownBy(() -> service.registrar(p("p001", "Fideos", "Alimentos", 5), Rol.ADMINISTRADOR))
                .isInstanceOf(ProductoDuplicadoException.class)
                .hasMessage(MensajesError.CODIGO_DUPLICADO);
    }

    @Test
    @DisplayName("8.1: precio = 0.00 rechaza — RF exige positivo")
    void registrar_precioCero_excepcion() {
        Producto p = p("P001", "Arroz", "Alimentos", 10);
        p.setPrecioUnitario(new BigDecimal("0.00"));
        assertThatThrownBy(() -> service.registrar(p, Rol.ADMINISTRADOR))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.VALORES_NEGATIVOS);
    }

    @Test
    @DisplayName("8.1: precio negativo rechaza")
    void registrar_precioNegativo_excepcion() {
        Producto p = p("P001", "Arroz", "Alimentos", 10);
        p.setPrecioUnitario(new BigDecimal("-1.00"));
        assertThatThrownBy(() -> service.registrar(p, Rol.ADMINISTRADOR))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.VALORES_NEGATIVOS);
    }

    @Test
    @DisplayName("8.1: precio con 3 decimales rechaza")
    void registrar_precioTresDecimales_excepcion() {
        Producto p = p("P001", "Arroz", "Alimentos", 10);
        p.setPrecioUnitario(new BigDecimal("1.001"));
        assertThatThrownBy(() -> service.registrar(p, Rol.ADMINISTRADOR))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.VALORES_NEGATIVOS);
    }

    @Test
    @DisplayName("8.1: stock inicial = 0 se permite (RF-INV-001: stock ≥ 0)")
    void registrar_stockCero_OK() {
        assertThatCode(() -> service.registrar(p("P001", "Arroz", "Alimentos", 0), Rol.ADMINISTRADOR))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("8.1: caracteres Unicode en nombre/categoría se registran sin error")
    void registrar_unicodeNombreCategoria_OK() {
        assertThatCode(() -> service.registrar(
                new Producto("U001", "Té de Árbol del Diablo ñoño", "desc", "Bebidas 🍵",
                        new BigDecimal("5.00"), 10), Rol.ADMINISTRADOR))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("8.1: strings muy largos (1000+ chars) se registran sin error")
    void registrar_stringsLargos_OK() {
        String nombre1000 = "A".repeat(1000);
        assertThatCode(() -> service.registrar(
                new Producto("L001", nombre1000, "desc", "Cat", new BigDecimal("1.00"), 0),
                Rol.ADMINISTRADOR))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RF-001: gestor puede registrar productos")
    void registrar_gestor_puede() {
        assertThatCode(() -> service.registrar(p("G001", "Arroz", "Cat", 5), Rol.GESTOR_INVENTARIO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RF-001: admin puede registrar productos")
    void registrar_admin_puede() {
        assertThatCode(() -> service.registrar(p("A001", "Fideos", "Cat", 5), Rol.ADMINISTRADOR))
                .doesNotThrowAnyException();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RF-INV-002 — BUSCAR PRODUCTO
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("RF-002: búsqueda por código exacto retorna producto correcto")
    void buscar_porCodigo_retornaProducto() {
        service.registrar(p("P001", "Arroz", "Alimentos", 10), Rol.ADMINISTRADOR);
        List<Producto> resultado = service.buscar("P001", TipoBusqueda.CODIGO, Rol.ADMINISTRADOR);
        assertThat(resultado).hasSize(1)
                .extracting(Producto::getCodigo).containsExactly("P001");
    }

    @Test
    @DisplayName("8.1/8.2: búsqueda por código case-insensitive ('p001' encuentra 'P001')")
    void buscar_porCodigo_caseInsensitive() {
        service.registrar(p("P001", "Arroz", "Alimentos", 10), Rol.ADMINISTRADOR);
        assertThat(service.buscar("p001", TipoBusqueda.CODIGO, Rol.ADMINISTRADOR)).hasSize(1);
        assertThat(service.buscar("P001", TipoBusqueda.CODIGO, Rol.ADMINISTRADOR)).hasSize(1);
    }

    @Test
    @DisplayName("RF-002: búsqueda por nombre parcial encuentra coincidencias (contiene)")
    void buscar_porNombre_parcial() {
        service.registrar(p("P001", "Camisa Blanca", "Ropa", 5), Rol.ADMINISTRADOR);
        service.registrar(p("P002", "Camiseta Deportiva", "Ropa", 8), Rol.ADMINISTRADOR);
        service.registrar(p("P003", "Pantalon", "Ropa", 3), Rol.ADMINISTRADOR);

        List<Producto> resultado = service.buscar("camis", TipoBusqueda.NOMBRE, Rol.ADMINISTRADOR);
        assertThat(resultado).hasSize(2)
                .extracting(Producto::getNombre)
                .containsExactlyInAnyOrder("Camisa Blanca", "Camiseta Deportiva");
    }

    @Test
    @DisplayName("8.2: búsqueda por nombre case-insensitive")
    void buscar_porNombre_caseInsensitive() {
        service.registrar(p("P001", "Arroz", "Alimentos", 5), Rol.ADMINISTRADOR);
        assertThat(service.buscar("ARROZ", TipoBusqueda.NOMBRE, Rol.ADMINISTRADOR)).hasSize(1);
        assertThat(service.buscar("arroz", TipoBusqueda.NOMBRE, Rol.ADMINISTRADOR)).hasSize(1);
    }

    @Test
    @DisplayName("8.2: búsqueda con espacios al inicio/final hace trim")
    void buscar_criterioConEspacios_trim() {
        service.registrar(p("P001", "Arroz", "Alimentos", 5), Rol.ADMINISTRADOR);
        assertThat(service.buscar("  Arroz  ", TipoBusqueda.NOMBRE, Rol.ADMINISTRADOR)).hasSize(1);
    }

    @Test
    @DisplayName("RF-002: búsqueda por categoría retorna todos los de esa categoría")
    void buscar_porCategoria_retornaCategoria() {
        service.registrar(p("P001", "Arroz", "Alimentos", 5), Rol.ADMINISTRADOR);
        service.registrar(p("P002", "Fideos", "Alimentos", 3), Rol.ADMINISTRADOR);
        service.registrar(p("P003", "Camisa", "Ropa", 10), Rol.ADMINISTRADOR);

        assertThat(service.buscar("Alimentos", TipoBusqueda.CATEGORIA, Rol.ADMINISTRADOR)).hasSize(2);
        assertThat(service.buscar("Ropa", TipoBusqueda.CATEGORIA, Rol.ADMINISTRADOR)).hasSize(1);
    }

    @Test
    @DisplayName("RF-002: criterio vacío retorna todos los activos (equivale a listar)")
    void buscar_criterioVacio_retornaTodosActivos() {
        service.registrar(p("P001", "Arroz", "Alimentos", 5), Rol.ADMINISTRADOR);
        service.registrar(p("P002", "Fideos", "Alimentos", 3), Rol.ADMINISTRADOR);

        assertThat(service.buscar("", TipoBusqueda.NOMBRE, Rol.ADMINISTRADOR)).hasSize(2);
        assertThat(service.buscar(null, TipoBusqueda.CODIGO, Rol.ADMINISTRADOR)).hasSize(2);
    }

    @Test
    @DisplayName("RF-002: búsqueda sin resultados retorna lista vacía (UI muestra el mensaje)")
    void buscar_sinResultados_listaVacia() {
        assertThat(service.buscar("NOEXISTE", TipoBusqueda.CODIGO, Rol.ADMINISTRADOR)).isEmpty();
        assertThat(service.buscar("zzz", TipoBusqueda.NOMBRE, Rol.ADMINISTRADOR)).isEmpty();
    }

    @Test
    @DisplayName("8.2: productos eliminados no aparecen en búsqueda")
    void buscar_productoEliminado_noAparece() {
        service.registrar(p("P001", "Arroz", "Alimentos", 5), Rol.ADMINISTRADOR);
        service.eliminar("P001", Rol.ADMINISTRADOR);

        assertThat(service.buscar("P001", TipoBusqueda.CODIGO, Rol.ADMINISTRADOR)).isEmpty();
        assertThat(service.buscar("Arroz", TipoBusqueda.NOMBRE, Rol.ADMINISTRADOR)).isEmpty();
        assertThat(service.buscar("", TipoBusqueda.NOMBRE, Rol.ADMINISTRADOR)).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RF-INV-004 — ELIMINAR PRODUCTO
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("RF-004: admin puede eliminar — eliminación lógica exitosa")
    void eliminar_admin_exitoso() {
        service.registrar(p("P001", "Arroz", "Alimentos", 5), Rol.ADMINISTRADOR);
        service.eliminar("P001", Rol.ADMINISTRADOR);

        assertThat(service.listar(new FiltroListado(null, null, null, null), Rol.ADMINISTRADOR))
                .extracting(Producto::getCodigo).doesNotContain("P001");
    }

    @Test
    @DisplayName("8.4: gestor intenta eliminar → PermisoDenegadoException con mensaje exacto")
    void eliminar_gestor_sinPermiso() {
        service.registrar(p("P001", "Arroz", "Alimentos", 5), Rol.ADMINISTRADOR);
        assertThatThrownBy(() -> service.eliminar("P001", Rol.GESTOR_INVENTARIO))
                .isInstanceOf(PermisoDenegadoException.class)
                .hasMessage(MensajesError.SIN_PERMISOS);
    }

    @Test
    @DisplayName("RF-004: producto inexistente → ProductoNoEncontradoException con mensaje exacto")
    void eliminar_productoNoExiste_excepcion() {
        assertThatThrownBy(() -> service.eliminar("NOEXISTE", Rol.ADMINISTRADOR))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessage(MensajesError.PRODUCTO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("8.4: doble eliminación → ProductoNoEncontradoException en la segunda")
    void eliminar_doble_excepcionEnSegunda() {
        service.registrar(p("P001", "Arroz", "Alimentos", 5), Rol.ADMINISTRADOR);
        service.eliminar("P001", Rol.ADMINISTRADOR);
        assertThatThrownBy(() -> service.eliminar("P001", Rol.ADMINISTRADOR))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessage(MensajesError.PRODUCTO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("8.4: movimientos del producto eliminado se conservan en historial")
    void eliminar_movimientosConservados() {
        service.registrar(p("P001", "Arroz", "Alimentos", 5), Rol.ADMINISTRADOR);
        // Registra un movimiento directo en el repo (no necesita InventarioService aquí)
        movimientoRepo.registrar(new Movimiento("P001", TipoMovimiento.ENTRADA, 5, 0, 5, Instant.now()));

        service.eliminar("P001", Rol.ADMINISTRADOR);

        assertThat(movimientoRepo.historialDe("P001")).hasSize(1);
    }

    @Test
    @DisplayName("RF-004/8.4: tieneMovimientosRecientes detecta movimiento previo")
    void tieneMovimientosRecientes_conMovimientosRecientes_true() {
        service.registrar(p("P001", "Arroz", "Alimentos", 5), Rol.ADMINISTRADOR);
        movimientoRepo.registrar(new Movimiento("P001", TipoMovimiento.ENTRADA, 5, 0, 5, Instant.now()));
        assertThat(service.tieneMovimientosRecientes("P001")).isTrue();
    }

    @Test
    @DisplayName("RF-004: tieneMovimientosRecientes sin movimientos previos devuelve false")
    void tieneMovimientosRecientes_sinMovimientos_false() {
        service.registrar(p("P001", "Arroz", "Alimentos", 5), Rol.ADMINISTRADOR);
        assertThat(service.tieneMovimientosRecientes("P001")).isFalse();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RF-INV-005 — LISTAR PRODUCTOS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("RF-005: sin filtros retorna todos los productos activos")
    void listar_sinFiltros_todosActivos() {
        service.registrar(p("P001", "Arroz", "Alimentos", 10), Rol.ADMINISTRADOR);
        service.registrar(p("P002", "Fideos", "Alimentos", 5), Rol.ADMINISTRADOR);
        assertThat(service.listar(new FiltroListado(null, null, null, null), Rol.ADMINISTRADOR))
                .hasSize(2);
    }

    @Test
    @DisplayName("RF-005: inventario vacío retorna lista vacía (UI muestra SIN_PRODUCTOS)")
    void listar_inventarioVacio_listaVacia() {
        assertThat(service.listar(new FiltroListado(null, null, null, null), Rol.ADMINISTRADOR))
                .isEmpty();
    }

    @Test
    @DisplayName("RF-005: filtro de categoría")
    void listar_filtroCategoria() {
        service.registrar(p("P001", "Arroz", "Alimentos", 10), Rol.ADMINISTRADOR);
        service.registrar(p("P002", "Fideos", "Alimentos", 5), Rol.ADMINISTRADOR);
        service.registrar(p("P003", "Camisa", "Ropa", 8), Rol.ADMINISTRADOR);

        assertThat(service.listar(new FiltroListado("Alimentos", null, null, null), Rol.ADMINISTRADOR))
                .hasSize(2)
                .allSatisfy(p -> assertThat(p.getCategoria()).isEqualToIgnoringCase("Alimentos"));
    }

    @Test
    @DisplayName("RF-005: filtro de rango de precio")
    void listar_filtroPrecio() {
        service.registrar(new Producto("P001", "Barato", "d", "Cat", new BigDecimal("5.00"), 10), Rol.ADMINISTRADOR);
        service.registrar(new Producto("P002", "Medio", "d", "Cat", new BigDecimal("15.00"), 10), Rol.ADMINISTRADOR);
        service.registrar(new Producto("P003", "Caro", "d", "Cat", new BigDecimal("50.00"), 10), Rol.ADMINISTRADOR);

        List<Producto> resultado = service.listar(
                new FiltroListado(null, new BigDecimal("10.00"), new BigDecimal("20.00"), null),
                Rol.ADMINISTRADOR);
        assertThat(resultado).hasSize(1)
                .extracting(Producto::getNombre).containsExactly("Medio");
    }

    @Test
    @DisplayName("RF-005: filtro de estado de stock")
    void listar_filtroEstadoStock() {
        service.registrar(new Producto("P001", "SinStock", "d", "Cat", new BigDecimal("1.00"), 0), Rol.ADMINISTRADOR);
        service.registrar(new Producto("P002", "Bajo", "d", "Cat", new BigDecimal("1.00"), 3), Rol.ADMINISTRADOR);
        service.registrar(new Producto("P003", "Normal", "d", "Cat", new BigDecimal("1.00"), 20), Rol.ADMINISTRADOR);

        assertThat(service.listar(new FiltroListado(null, null, null, EstadoStock.SIN_STOCK), Rol.ADMINISTRADOR))
                .hasSize(1).extracting(Producto::getNombre).containsExactly("SinStock");
        assertThat(service.listar(new FiltroListado(null, null, null, EstadoStock.BAJO), Rol.ADMINISTRADOR))
                .hasSize(1).extracting(Producto::getNombre).containsExactly("Bajo");
        assertThat(service.listar(new FiltroListado(null, null, null, EstadoStock.NORMAL), Rol.ADMINISTRADOR))
                .hasSize(1).extracting(Producto::getNombre).containsExactly("Normal");
    }

    @Test
    @DisplayName("RF-005: filtros combinados (categoría + rango precio + estado)")
    void listar_filtrosCombinados() {
        service.registrar(new Producto("P001", "ArrBarato", "d", "Alimentos", new BigDecimal("8.00"), 3), Rol.ADMINISTRADOR);
        service.registrar(new Producto("P002", "ArrCaro", "d", "Alimentos", new BigDecimal("50.00"), 3), Rol.ADMINISTRADOR);
        service.registrar(new Producto("P003", "RopaBarata", "d", "Ropa", new BigDecimal("8.00"), 3), Rol.ADMINISTRADOR);
        service.registrar(new Producto("P004", "ArrNormal", "d", "Alimentos", new BigDecimal("8.00"), 20), Rol.ADMINISTRADOR);

        List<Producto> resultado = service.listar(
                new FiltroListado("Alimentos", new BigDecimal("5.00"), new BigDecimal("10.00"), EstadoStock.BAJO),
                Rol.ADMINISTRADOR);
        assertThat(resultado).hasSize(1)
                .extracting(Producto::getCodigo).containsExactly("P001");
    }

    @Test
    @DisplayName("8.5: precio min > max → ValidacionException con mensaje de valores negativos")
    void listar_precioMinMayorQueMax_excepcion() {
        assertThatThrownBy(() -> new FiltroListado(null, new BigDecimal("100.00"), new BigDecimal("10.00"), null))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.VALORES_NEGATIVOS);
    }

    @Test
    @DisplayName("8.5: stock = 4 → BAJO (frontera: < 5 es bajo)")
    void listar_stock4_marcadoBAJO_frontera() {
        service.registrar(new Producto("P001", "A", "d", "Cat", new BigDecimal("1.00"), 4), Rol.ADMINISTRADOR);
        service.registrar(new Producto("P002", "B", "d", "Cat", new BigDecimal("1.00"), 5), Rol.ADMINISTRADOR);

        List<Producto> bajos = service.listar(
                new FiltroListado(null, null, null, EstadoStock.BAJO), Rol.ADMINISTRADOR);
        assertThat(bajos).hasSize(1)
                .extracting(Producto::getCodigo).containsExactly("P001");
        assertThat(EstadoStock.desde(4)).isEqualTo(EstadoStock.BAJO);
    }

    @Test
    @DisplayName("8.5: stock = 5 → NORMAL, NO es BAJO (frontera estricta del RF-INV-005)")
    void listar_stock5_esNORMAL_noBAJO_fronteraEstricta() {
        service.registrar(new Producto("P001", "A", "d", "Cat", new BigDecimal("1.00"), 5), Rol.ADMINISTRADOR);

        assertThat(service.listar(new FiltroListado(null, null, null, EstadoStock.BAJO), Rol.ADMINISTRADOR))
                .isEmpty();
        assertThat(EstadoStock.desde(5)).isEqualTo(EstadoStock.NORMAL);
    }

    @Test
    @DisplayName("8.5: stock = 0 → SIN_STOCK")
    void listar_stock0_esSinStock() {
        service.registrar(new Producto("P001", "A", "d", "Cat", new BigDecimal("1.00"), 0), Rol.ADMINISTRADOR);

        assertThat(service.listar(new FiltroListado(null, null, null, EstadoStock.SIN_STOCK), Rol.ADMINISTRADOR))
                .hasSize(1);
        assertThat(EstadoStock.desde(0)).isEqualTo(EstadoStock.SIN_STOCK);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PERMISOS — tabla de decisión
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Permisos: gestor puede REGISTRAR, BUSCAR y LISTAR — no puede ELIMINAR")
    void permisos_gestor_tabla() {
        service.registrar(p("P001", "Arroz", "Cat", 10), Rol.ADMINISTRADOR);

        assertThatCode(() -> service.registrar(p("G001", "Fideos", "Cat", 5), Rol.GESTOR_INVENTARIO))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.buscar("G001", TipoBusqueda.CODIGO, Rol.GESTOR_INVENTARIO))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.listar(new FiltroListado(null, null, null, null), Rol.GESTOR_INVENTARIO))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> service.eliminar("P001", Rol.GESTOR_INVENTARIO))
                .isInstanceOf(PermisoDenegadoException.class)
                .hasMessage(MensajesError.SIN_PERMISOS);
    }

    @Test
    @DisplayName("Permisos: admin puede todas las operaciones de producto")
    void permisos_admin_puedeTodasLasOperaciones() {
        assertThatCode(() -> {
            service.registrar(p("A001", "Arroz", "Cat", 10), Rol.ADMINISTRADOR);
            service.buscar("A001", TipoBusqueda.CODIGO, Rol.ADMINISTRADOR);
            service.listar(new FiltroListado(null, null, null, null), Rol.ADMINISTRADOR);
            service.eliminar("A001", Rol.ADMINISTRADOR);
        }).doesNotThrowAnyException();
    }
}
