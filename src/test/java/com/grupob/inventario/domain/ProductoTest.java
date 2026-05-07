package com.grupob.inventario.domain;

import com.grupob.inventario.domain.exception.StockInsuficienteException;
import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.util.MensajesError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Producto — invariantes del constructor y métodos de dominio")
class ProductoTest {

    private Producto productoValido() {
        return new Producto("P001", "Arroz", "Arroz blanco premium", "Alimentos",
                new BigDecimal("10.50"), 100);
    }

    // ── CONSTRUCTOR: campos obligatorios ──────────────────────────────────────

    @Test
    @DisplayName("código null → ValidacionException con mensaje de campos obligatorios")
    void constructor_codigoNull_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new Producto(null, "Arroz", "desc", "Alimentos", new BigDecimal("1.00"), 0))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }

    @Test
    @DisplayName("código en blanco (solo espacios) → ValidacionException con mensaje de campos obligatorios")
    void constructor_codigoBlancos_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new Producto("   ", "Arroz", "desc", "Alimentos", new BigDecimal("1.00"), 0))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }

    @Test
    @DisplayName("nombre vacío → ValidacionException con mensaje de campos obligatorios")
    void constructor_nombreVacio_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new Producto("P001", "", "desc", "Alimentos", new BigDecimal("1.00"), 0))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }

    @Test
    @DisplayName("categoría null → ValidacionException con mensaje de campos obligatorios")
    void constructor_categoriaNula_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new Producto("P001", "Arroz", "desc", null, new BigDecimal("1.00"), 0))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }

    @Test
    @DisplayName("precio null → ValidacionException con mensaje de campos obligatorios")
    void constructor_precioNull_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new Producto("P001", "Arroz", "desc", "Alimentos", null, 0))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }

    // ── CONSTRUCTOR: valores numéricos ────────────────────────────────────────

    @Test
    @DisplayName("precio negativo → ValidacionException con mensaje de valores negativos")
    void constructor_precioNegativo_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new Producto("P001", "Arroz", "desc", "Alimentos", new BigDecimal("-0.01"), 0))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.VALORES_NEGATIVOS);
    }

    @Test
    @DisplayName("precio = 0.00 → ValidacionException — el RF exige precio estrictamente positivo")
    void constructor_precioCero_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new Producto("P001", "Arroz", "desc", "Alimentos", new BigDecimal("0.00"), 0))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.VALORES_NEGATIVOS);
    }

    @Test
    @DisplayName("precio con 3 decimales → ValidacionException (escala > 2 no permitida)")
    void constructor_precioTresDecimales_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new Producto("P001", "Arroz", "desc", "Alimentos", new BigDecimal("1.001"), 0))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.VALORES_NEGATIVOS);
    }

    @Test
    @DisplayName("precio = 0.01 es válido (límite inferior positivo con 2 decimales)")
    void constructor_precioMinimo_esValido() {
        Producto p = new Producto("P001", "Arroz", "desc", "Alimentos", new BigDecimal("0.01"), 0);
        assertThat(p.getPrecioUnitario()).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    @DisplayName("stock negativo (-1) → ValidacionException con mensaje de valores negativos")
    void constructor_stockNegativo_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new Producto("P001", "Arroz", "desc", "Alimentos", new BigDecimal("1.00"), -1))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.VALORES_NEGATIVOS);
    }

    @Test
    @DisplayName("stock = 0 es válido (límite inferior permitido por RF-INV-001)")
    void constructor_stockCero_esValido() {
        Producto p = new Producto("P001", "Arroz", "desc", "Alimentos", new BigDecimal("1.00"), 0);
        assertThat(p.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("producto válido se crea con activo = true")
    void constructor_productoValido_activoPorDefecto() {
        assertThat(productoValido().isActivo()).isTrue();
    }

    // ── incrementarStock ──────────────────────────────────────────────────────

    @Test
    @DisplayName("incrementarStock con cantidad válida suma correctamente")
    void incrementarStock_cantidadValida_incrementa() {
        Producto p = productoValido(); // stock = 100
        p.incrementarStock(50);
        assertThat(p.getStock()).isEqualTo(150);
    }

    @Test
    @DisplayName("incrementarStock con cantidad = 0 → ValidacionException")
    void incrementarStock_cantidadCero_lanzaExcepcion() {
        assertThatThrownBy(() -> productoValido().incrementarStock(0))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CANTIDAD_NO_POSITIVA);
    }

    @Test
    @DisplayName("incrementarStock con cantidad negativa → ValidacionException")
    void incrementarStock_cantidadNegativa_lanzaExcepcion() {
        assertThatThrownBy(() -> productoValido().incrementarStock(-1))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CANTIDAD_NO_POSITIVA);
    }

    // ── decrementarStock ──────────────────────────────────────────────────────

    @Test
    @DisplayName("decrementarStock con cantidad válida resta correctamente")
    void decrementarStock_cantidadValida_decrementa() {
        Producto p = productoValido(); // stock = 100
        p.decrementarStock(30);
        assertThat(p.getStock()).isEqualTo(70);
    }

    @Test
    @DisplayName("decrementarStock igual al stock deja stock = 0 (límite exacto permitido)")
    void decrementarStock_igualAlStock_dejaCero() {
        Producto p = productoValido(); // stock = 100
        p.decrementarStock(100);
        assertThat(p.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("decrementarStock supera stock → StockInsuficienteException con mensaje correcto")
    void decrementarStock_superaStock_mensajeCorrecto() {
        Producto p = productoValido(); // stock = 100
        assertThatThrownBy(() -> p.decrementarStock(101))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessage(String.format(MensajesError.STOCK_INSUFICIENTE_FMT, 100));
    }

    @Test
    @DisplayName("decrementarStock supera stock → stockDisponible en la excepción refleja el stock real")
    void decrementarStock_superaStock_stockDisponibleEnExcepcion() {
        Producto p = new Producto("P001", "Arroz", "desc", "Alimentos", new BigDecimal("1.00"), 7);
        assertThatThrownBy(() -> p.decrementarStock(8))
                .isInstanceOf(StockInsuficienteException.class)
                .satisfies(e -> {
                    StockInsuficienteException sie = (StockInsuficienteException) e;
                    assertThat(sie.getStockDisponible()).isEqualTo(7);
                    assertThat(sie.getMessage())
                            .isEqualTo(String.format(MensajesError.STOCK_INSUFICIENTE_FMT, 7));
                });
    }

    @Test
    @DisplayName("decrementarStock con cantidad = 0 → ValidacionException")
    void decrementarStock_cantidadCero_lanzaExcepcion() {
        assertThatThrownBy(() -> productoValido().decrementarStock(0))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CANTIDAD_NO_POSITIVA);
    }

    @Test
    @DisplayName("decrementarStock con cantidad = stock + 1 rechaza (caso límite superior)")
    void decrementarStock_stockMasUno_lanzaExcepcion() {
        Producto p = new Producto("P001", "Arroz", "desc", "Alimentos", new BigDecimal("1.00"), 5);
        assertThatThrownBy(() -> p.decrementarStock(6))
                .isInstanceOf(StockInsuficienteException.class);
    }

    // ── eliminación lógica ────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar() pone activo = false (eliminación lógica, no física)")
    void eliminar_ponActivoFalse() {
        Producto p = productoValido();
        assertThat(p.isActivo()).isTrue();
        p.eliminar();
        assertThat(p.isActivo()).isFalse();
    }

    @Test
    @DisplayName("eliminar() dos veces no lanza excepción — el producto sigue inactivo")
    void eliminar_dobleEliminacion_sigueInactivo() {
        Producto p = productoValido();
        p.eliminar();
        p.eliminar();
        assertThat(p.isActivo()).isFalse();
    }

    // ── equals / hashCode ─────────────────────────────────────────────────────

    @Test
    @DisplayName("equals se basa en código — mismo código con distintos datos son iguales")
    void equals_mismoCodigo_sonIguales() {
        Producto p1 = new Producto("P001", "Arroz", "desc1", "Cat1", new BigDecimal("1.00"), 10);
        Producto p2 = new Producto("P001", "Fideos", "desc2", "Cat2", new BigDecimal("2.50"), 5);
        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }

    @Test
    @DisplayName("códigos distintos → productos distintos")
    void equals_codigoDistinto_noSonIguales() {
        Producto p1 = new Producto("P001", "Arroz", "desc", "Cat", new BigDecimal("1.00"), 0);
        Producto p2 = new Producto("P002", "Arroz", "desc", "Cat", new BigDecimal("1.00"), 0);
        assertThat(p1).isNotEqualTo(p2);
    }

    // ── setters y cobertura adicional ─────────────────────────────────

    @Test
    @DisplayName("setDescripcion(null) asigna string vacío internamente")
    void setDescripcion_null_asignaVacio() {
        Producto p = productoValido();
        p.setDescripcion(null);
        assertThat(p.getDescripcion()).isEmpty();
    }

    @Test
    @DisplayName("setDescripcion(valor) actualiza la descripción")
    void setDescripcion_valor_actualiza() {
        Producto p = productoValido();
        p.setDescripcion("Nueva descripción");
        assertThat(p.getDescripcion()).isEqualTo("Nueva descripción");
    }

    @Test
    @DisplayName("toString contiene código, nombre y stock — no debe ser nulo")
    void toString_contieneInfoRelevante() {
        Producto p = productoValido();
        String s = p.toString();
        assertThat(s)
                .contains("P001")
                .contains("Arroz")
                .isNotEmpty();
    }

    @Test
    @DisplayName("hashCode es consistente con equals")
    void hashCode_consistenteConEquals() {
        Producto p1 = productoValido();
        Producto p2 = productoValido();
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }
}
