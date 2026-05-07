package com.grupob.inventario.validation;

import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.exception.StockInsuficienteException;
import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.util.MensajesError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StockValidator — equivalencia y valores límite en movimientos")
class StockValidatorTest {

    private StockValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StockValidator();
    }

    // ── cantidad ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cantidad = 0 → CANTIDAD_NO_POSITIVA (límite rechazado)")
    void validarMovimiento_cantidadCero_lanzaExcepcion() {
        assertThatThrownBy(() -> validator.validarMovimiento(0, 100, TipoMovimiento.ENTRADA))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CANTIDAD_NO_POSITIVA);
    }

    @Test
    @DisplayName("cantidad = -1 → CANTIDAD_NO_POSITIVA")
    void validarMovimiento_cantidadNegativa_lanzaExcepcion() {
        assertThatThrownBy(() -> validator.validarMovimiento(-1, 100, TipoMovimiento.SALIDA))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CANTIDAD_NO_POSITIVA);
    }

    @Test
    @DisplayName("cantidad = 1 → válido (límite inferior aceptado)")
    void validarMovimiento_cantidadUno_sinExcepcion() {
        assertThatCode(() -> validator.validarMovimiento(1, 100, TipoMovimiento.ENTRADA))
                .doesNotThrowAnyException();
    }

    // ── ENTRADA no tiene restricción de stock ─────────────────────────────────

    @Test
    @DisplayName("ENTRADA con cantidad > stock → válido (entradas siempre permitidas)")
    void validarMovimiento_entradaMayorQueStock_sinExcepcion() {
        assertThatCode(() -> validator.validarMovimiento(1000, 5, TipoMovimiento.ENTRADA))
                .doesNotThrowAnyException();
    }

    // ── SALIDA: valores límite ────────────────────────────────────────────────

    @Test
    @DisplayName("SALIDA igual al stock disponible → válido (deja stock en 0)")
    void validarMovimiento_salidaIgualAlStock_sinExcepcion() {
        assertThatCode(() -> validator.validarMovimiento(50, 50, TipoMovimiento.SALIDA))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SALIDA = stock + 1 → StockInsuficienteException (límite superior rechazado)")
    void validarMovimiento_salidaStockMasUno_lanzaExcepcion() {
        assertThatThrownBy(() -> validator.validarMovimiento(51, 50, TipoMovimiento.SALIDA))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessage(String.format(MensajesError.STOCK_INSUFICIENTE_FMT, 50));
    }

    @Test
    @DisplayName("SALIDA supera stock — stockDisponible en la excepción refleja el stock real")
    void validarMovimiento_salidaSuperaStock_stockDisponibleCorrecto() {
        assertThatThrownBy(() -> validator.validarMovimiento(10, 3, TipoMovimiento.SALIDA))
                .isInstanceOf(StockInsuficienteException.class)
                .satisfies(e -> {
                    int disponible = ((StockInsuficienteException) e).getStockDisponible();
                    org.assertj.core.api.Assertions.assertThat(disponible).isEqualTo(3);
                });
    }

    @Test
    @DisplayName("SALIDA con stock = 0 y cualquier cantidad → StockInsuficienteException con disponible=0")
    void validarMovimiento_salidaConStockCero_lanzaExcepcion() {
        assertThatThrownBy(() -> validator.validarMovimiento(1, 0, TipoMovimiento.SALIDA))
                .isInstanceOf(StockInsuficienteException.class)
                .satisfies(e -> {
                    int disponible = ((StockInsuficienteException) e).getStockDisponible();
                    org.assertj.core.api.Assertions.assertThat(disponible).isEqualTo(0);
                });
    }
}
