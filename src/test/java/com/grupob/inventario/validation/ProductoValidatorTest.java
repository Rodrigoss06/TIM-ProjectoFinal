package com.grupob.inventario.validation;

import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.util.MensajesError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ProductoValidator — equivalencia y valores límite (7.2 Notion)")
class ProductoValidatorTest {

    private ProductoValidator validator;
    private Producto mockProducto;

    @BeforeEach
    void setUp() {
        validator = new ProductoValidator();
        mockProducto = mock(Producto.class);
    }

    private Producto productoValido() {
        return new Producto("P001", "Arroz", "desc", "Alimentos", new BigDecimal("1.00"), 10);
    }

    // ── validarRegistro: campos obligatorios ──────────────────────────────────

    @Test
    @DisplayName("producto válido no lanza excepción")
    void validarRegistro_productoValido_sinExcepcion() {
        assertThatCode(() -> validator.validarRegistro(productoValido()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("código null → ValidacionException (CAMPOS_OBLIGATORIOS)")
    void validarRegistro_codigoNull_lanzaExcepcion() {
        when(mockProducto.getCodigo()).thenReturn(null);
        assertThatThrownBy(() -> validator.validarRegistro(mockProducto))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }

    @Test
    @DisplayName("código en blanco → ValidacionException (CAMPOS_OBLIGATORIOS)")
    void validarRegistro_codigoBlancos_lanzaExcepcion() {
        when(mockProducto.getCodigo()).thenReturn("   ");
        assertThatThrownBy(() -> validator.validarRegistro(mockProducto))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }

    @Test
    @DisplayName("nombre vacío → ValidacionException (CAMPOS_OBLIGATORIOS)")
    void validarRegistro_nombreVacio_lanzaExcepcion() {
        Producto p = productoValido();
        p.setNombre("");
        assertThatThrownBy(() -> validator.validarRegistro(p))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }

    @Test
    @DisplayName("categoría null → ValidacionException (CAMPOS_OBLIGATORIOS)")
    void validarRegistro_categoriaNula_lanzaExcepcion() {
        Producto p = productoValido();
        p.setCategoria(null);
        assertThatThrownBy(() -> validator.validarRegistro(p))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }

    @Test
    @DisplayName("precio null → ValidacionException (CAMPOS_OBLIGATORIOS)")
    void validarRegistro_precioNull_lanzaExcepcion() {
        Producto p = productoValido();
        p.setPrecioUnitario(null);
        assertThatThrownBy(() -> validator.validarRegistro(p))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }

    // ── validarRegistro: valores numéricos — valores límite explícitos ─────────

    @Test
    @DisplayName("precio = -0.01 → VALORES_NEGATIVOS (partición inválida)")
    void validarRegistro_precioNegativo_lanzaExcepcion() {
        Producto p = productoValido();
        p.setPrecioUnitario(new BigDecimal("-0.01"));
        assertThatThrownBy(() -> validator.validarRegistro(p))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.VALORES_NEGATIVOS);
    }

    @Test
    @DisplayName("precio = 0.00 → VALORES_NEGATIVOS (límite inferior rechazado — RF exige > 0)")
    void validarRegistro_precioCero_lanzaExcepcion() {
        Producto p = productoValido();
        p.setPrecioUnitario(new BigDecimal("0.00"));
        assertThatThrownBy(() -> validator.validarRegistro(p))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.VALORES_NEGATIVOS);
    }

    @Test
    @DisplayName("precio = 0.01 → válido (límite inferior aceptado)")
    void validarRegistro_precioMinimo_sinExcepcion() {
        Producto p = productoValido();
        p.setPrecioUnitario(new BigDecimal("0.01"));
        assertThatCode(() -> validator.validarRegistro(p)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("precio con 3 decimales → VALORES_NEGATIVOS")
    void validarRegistro_precioTresDecimales_lanzaExcepcion() {
        Producto p = productoValido();
        p.setPrecioUnitario(new BigDecimal("1.001"));
        assertThatThrownBy(() -> validator.validarRegistro(p))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.VALORES_NEGATIVOS);
    }

    @Test
    @DisplayName("stock = -1 → VALORES_NEGATIVOS (límite inferior rechazado)")
    void validarRegistro_stockNegativo_lanzaExcepcion() {
        when(mockProducto.getCodigo()).thenReturn("P001");
        when(mockProducto.getNombre()).thenReturn("Arroz");
        when(mockProducto.getCategoria()).thenReturn("Alimentos");
        when(mockProducto.getPrecioUnitario()).thenReturn(new BigDecimal("1.00"));
        when(mockProducto.getStock()).thenReturn(-1);

        assertThatThrownBy(() -> validator.validarRegistro(mockProducto))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.VALORES_NEGATIVOS);
    }

    @Test
    @DisplayName("stock = 0 → válido (límite inferior aceptado, RF-INV-001)")
    void validarRegistro_stockCero_sinExcepcion() {
        assertThatCode(() -> validator.validarRegistro(
                new Producto("P001", "Arroz", "desc", "Alimentos", new BigDecimal("1.00"), 0)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("stock = 1 → válido")
    void validarRegistro_stockUno_sinExcepcion() {
        assertThatCode(() -> validator.validarRegistro(
                new Producto("P001", "Arroz", "desc", "Alimentos", new BigDecimal("1.00"), 1)))
                .doesNotThrowAnyException();
    }

    // ── validarActualizacion ──────────────────────────────────────────────────

    @Test
    @DisplayName("validarActualizacion con producto válido no lanza excepción")
    void validarActualizacion_productoValido_sinExcepcion() {
        assertThatCode(() -> validator.validarActualizacion(productoValido()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarActualizacion permite stock = 0 (producto sin unidades)")
    void validarActualizacion_stockCero_sinExcepcion() {
        assertThatCode(() -> validator.validarActualizacion(
                new Producto("P001", "Arroz", "desc", "Alimentos", new BigDecimal("1.00"), 0)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarActualizacion con nombre en blanco lanza la misma excepción que registro")
    void validarActualizacion_nombreBlancos_lanzaExcepcion() {
        Producto p = productoValido();
        p.setNombre("  ");
        assertThatThrownBy(() -> validator.validarActualizacion(p))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }
}
