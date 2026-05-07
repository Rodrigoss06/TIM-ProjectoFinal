package com.grupob.inventario.repository;

import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.model.Movimiento;
import com.grupob.inventario.repository.memory.MovimientoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MovimientoRepositoryEnMemoria — registro, historial y ventana temporal")
class MovimientoRepositoryEnMemoriaTest {

    private static final Instant AHORA = Instant.parse("2026-04-29T10:00:00Z");

    private MovimientoRepositoryEnMemoria repo;

    @BeforeEach
    void setUp() {
        repo = new MovimientoRepositoryEnMemoria();
    }

    private Movimiento movimiento(String codigo, TipoMovimiento tipo, int cantidad,
                                  int anterior, int nuevo, Instant fecha) {
        return new Movimiento(codigo, tipo, cantidad, anterior, nuevo, fecha);
    }

    // ── CRUD básico ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("registrar y historialDe retornan el movimiento guardado")
    void registrar_y_historialDe_retornaMovimiento() {
        Movimiento m = movimiento("P001", TipoMovimiento.ENTRADA, 10, 0, 10, AHORA);
        repo.registrar(m);
        assertThat(repo.historialDe("P001")).hasSize(1).contains(m);
    }

    @Test
    @DisplayName("historialDe solo retorna movimientos del producto indicado")
    void historialDe_soloDelProductoIndicado() {
        repo.registrar(movimiento("P001", TipoMovimiento.ENTRADA, 10, 0, 10, AHORA));
        repo.registrar(movimiento("P002", TipoMovimiento.SALIDA, 5, 20, 15, AHORA));
        repo.registrar(movimiento("P001", TipoMovimiento.SALIDA, 3, 10, 7, AHORA));

        assertThat(repo.historialDe("P001")).hasSize(2);
        assertThat(repo.historialDe("P002")).hasSize(1);
    }

    @Test
    @DisplayName("historialDe para producto sin movimientos retorna lista vacía")
    void historialDe_sinMovimientos_vacio() {
        assertThat(repo.historialDe("NOEXISTE")).isEmpty();
    }

    @Test
    @DisplayName("historialDe es case-insensitive en el código de producto")
    void historialDe_caseInsensitive() {
        repo.registrar(movimiento("P001", TipoMovimiento.ENTRADA, 10, 0, 10, AHORA));
        assertThat(repo.historialDe("p001")).hasSize(1);
        assertThat(repo.historialDe("P001")).hasSize(1);
    }

    @Test
    @DisplayName("registrar múltiples movimientos conserva orden de inserción")
    void registrar_multiples_conservaOrden() {
        Movimiento m1 = movimiento("P001", TipoMovimiento.ENTRADA, 10, 0, 10, AHORA);
        Movimiento m2 = movimiento("P001", TipoMovimiento.SALIDA, 3, 10, 7, AHORA.plusSeconds(60));
        repo.registrar(m1);
        repo.registrar(m2);

        List<Movimiento> historial = repo.historialDe("P001");
        assertThat(historial).containsExactly(m1, m2);
    }

    // ── tieneMovimientosRecientes ─────────────────────────────────────────────

    @Test
    @DisplayName("movimiento dentro de la ventana temporal → true")
    void tieneMovimientosRecientes_dentroVentana_true() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        // movimiento 5 minutos antes de "ahora"
        Movimiento m = movimiento("P001", TipoMovimiento.ENTRADA, 10, 0, 10,
                AHORA.minus(Duration.ofMinutes(5)));
        repo.registrar(m);

        assertThat(repo.tieneMovimientosRecientes("P001", Duration.ofMinutes(30), reloj)).isTrue();
    }

    @Test
    @DisplayName("movimiento fuera de la ventana temporal → false")
    void tieneMovimientosRecientes_fueraVentana_false() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        // movimiento 60 minutos antes de "ahora" — fuera de ventana de 30 min
        Movimiento m = movimiento("P001", TipoMovimiento.ENTRADA, 10, 0, 10,
                AHORA.minus(Duration.ofMinutes(60)));
        repo.registrar(m);

        assertThat(repo.tieneMovimientosRecientes("P001", Duration.ofMinutes(30), reloj)).isFalse();
    }

    @Test
    @DisplayName("movimiento exactamente en el límite de la ventana → false (no incluye el límite)")
    void tieneMovimientosRecientes_exactamenteEnLimite_false() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        // movimiento exactamente en ahora - 30min → no es "after" el límite
        Movimiento m = movimiento("P001", TipoMovimiento.ENTRADA, 10, 0, 10,
                AHORA.minus(Duration.ofMinutes(30)));
        repo.registrar(m);

        assertThat(repo.tieneMovimientosRecientes("P001", Duration.ofMinutes(30), reloj)).isFalse();
    }

    @Test
    @DisplayName("sin movimientos para ese producto → false")
    void tieneMovimientosRecientes_sinMovimientos_false() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        assertThat(repo.tieneMovimientosRecientes("NOEXISTE", Duration.ofMinutes(30), reloj)).isFalse();
    }

    @Test
    @DisplayName("tieneMovimientosRecientes usa Clock inyectable — funciona con Clock.fixed")
    void tieneMovimientosRecientes_clockFijo_comportamientoCorrectoConTiempo() {
        Instant momento1 = Instant.parse("2026-04-29T08:00:00Z");
        Instant momento2 = Instant.parse("2026-04-29T10:00:00Z");
        Clock relojEn10h = Clock.fixed(momento2, ZoneOffset.UTC);

        // movimiento a las 08:00 → hace 2 horas respecto a las 10:00
        repo.registrar(movimiento("P001", TipoMovimiento.ENTRADA, 5, 0, 5, momento1));

        // ventana de 3h: cubre las 08:00 desde las 10:00
        assertThat(repo.tieneMovimientosRecientes("P001", Duration.ofHours(3), relojEn10h)).isTrue();
        // ventana de 1h: no cubre las 08:00 desde las 10:00
        assertThat(repo.tieneMovimientosRecientes("P001", Duration.ofHours(1), relojEn10h)).isFalse();
    }

    // ── contratos de inmutabilidad ────────────────────────────────────────────

    @Test
    @DisplayName("historialDe devuelve lista inmutable")
    void historialDe_listaInmutable() {
        repo.registrar(movimiento("P001", TipoMovimiento.ENTRADA, 10, 0, 10, AHORA));
        List<Movimiento> lista = repo.historialDe("P001");
        assertThatThrownBy(() -> lista.add(movimiento("P001", TipoMovimiento.SALIDA, 1, 10, 9, AHORA)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
