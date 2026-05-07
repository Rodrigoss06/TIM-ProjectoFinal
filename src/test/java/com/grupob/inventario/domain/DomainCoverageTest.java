package com.grupob.inventario.domain;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.exception.ConfiguracionException;
import com.grupob.inventario.domain.exception.PersistenciaException;
import com.grupob.inventario.domain.model.EventoAuditoria;
import com.grupob.inventario.domain.model.Movimiento;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.util.MensajesError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests focalizados en cobertura de clases de dominio con baja cobertura.
 * Objetivo: llevar domain/ a ≥ 90% de instrucciones.
 */
@DisplayName("DomainCoverageTest — cobertura de excepciones, Movimiento y EventoAuditoria")
class DomainCoverageTest {

    private static final Instant T0 = Instant.parse("2026-05-01T10:00:00Z");
    private static final Clock   RELOJ = Clock.fixed(T0, ZoneOffset.UTC);

    // ── PersistenciaException ─────────────────────────────────────────

    @Test
    @DisplayName("PersistenciaException(msg) — mensaje correcto")
    void persistenciaException_mensaje() {
        PersistenciaException e = new PersistenciaException("falla DB");
        assertThat(e.getMessage()).isEqualTo("falla DB");
    }

    @Test
    @DisplayName("PersistenciaException(msg, cause) — mensaje y causa correctos")
    void persistenciaException_conCausa() {
        RuntimeException causa = new RuntimeException("causa raíz");
        PersistenciaException e = new PersistenciaException(MensajesError.ERROR_CONEXION_BD, causa);
        assertThat(e.getMessage()).isEqualTo(MensajesError.ERROR_CONEXION_BD);
        assertThat(e.getCause()).isEqualTo(causa);
    }

    // ── ConfiguracionException ────────────────────────────────────────

    @Test
    @DisplayName("ConfiguracionException(msg) — mensaje correcto")
    void configuracionException_mensaje() {
        ConfiguracionException e = new ConfiguracionException("falta db.url");
        assertThat(e.getMessage()).isEqualTo("falta db.url");
    }

    @Test
    @DisplayName("ConfiguracionException(msg, cause) — causa encadenada")
    void configuracionException_conCausa() {
        Exception causa = new IllegalArgumentException("arg inválido");
        ConfiguracionException e = new ConfiguracionException("error config", causa);
        assertThat(e.getMessage()).isEqualTo("error config");
        assertThat(e.getCause()).isEqualTo(causa);
    }

    // ── Movimiento — getters JavaBean y equals/hashCode ───────────────

    @Test
    @DisplayName("Movimiento — getters JavaBean cubren la clase convertida de record")
    void movimiento_gettersJavaBean() {
        Instant ahora = Instant.now();
        Movimiento m = new Movimiento("P001", TipoMovimiento.ENTRADA, 10, 5, 15, ahora);

        // JavaBean getters (nuevos en la clase, no en el record original)
        assertThat(m.getId()).isNotNull();
        assertThat(m.getCodigoProducto()).isEqualTo("P001");
        assertThat(m.getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(m.getCantidad()).isEqualTo(10);
        assertThat(m.getStockAnterior()).isEqualTo(5);
        assertThat(m.getStockNuevo()).isEqualTo(15);
        assertThat(m.getFecha()).isEqualTo(ahora);

        // Consistencia entre accessores estilo record y JavaBean
        assertThat(m.codigoProducto()).isEqualTo(m.getCodigoProducto());
        assertThat(m.tipo()).isEqualTo(m.getTipo());
        assertThat(m.cantidad()).isEqualTo(m.getCantidad());
        assertThat(m.stockAnterior()).isEqualTo(m.getStockAnterior());
        assertThat(m.stockNuevo()).isEqualTo(m.getStockNuevo());
        assertThat(m.fecha()).isEqualTo(m.getFecha());
        assertThat(m.id()).isEqualTo(m.getId());
    }

    @Test
    @DisplayName("Movimiento — equals y hashCode basados en UUID")
    void movimiento_equalsHashCode() {
        Movimiento m1 = new Movimiento("P001", TipoMovimiento.ENTRADA, 10, 0, 10, Instant.now());
        Movimiento m2 = new Movimiento("P001", TipoMovimiento.ENTRADA, 10, 0, 10, Instant.now());

        // Distintos UUIDs aunque mismos datos
        assertThat(m1).isNotEqualTo(m2);
        assertThat(m1).isEqualTo(m1);
        assertThat(m1.hashCode()).isEqualTo(m1.hashCode());
    }

    @Test
    @DisplayName("Movimiento — toString contiene información útil")
    void movimiento_toString() {
        Movimiento m = new Movimiento("P001", TipoMovimiento.SALIDA, 5, 10, 5, Instant.now());
        String s = m.toString();
        assertThat(s).contains("P001").contains("SALIDA").isNotEmpty();
    }

    // ── EventoAuditoria ───────────────────────────────────────────────

    @Test
    @DisplayName("EventoAuditoria — todos los getters accesibles")
    void eventoAuditoria_getters() {
        EventoAuditoria e = EventoAuditoria.crear("admin", TipoEvento.CREAR_PRODUCTO,
                "PROD001", "alta inicial", RELOJ);

        assertThat(e.getId()).isNotNull();
        assertThat(e.getFecha()).isEqualTo(T0);
        assertThat(e.getUsername()).isEqualTo("admin");
        assertThat(e.getTipoEvento()).isEqualTo(TipoEvento.CREAR_PRODUCTO);
        assertThat(e.getEntidadAfectada()).isEqualTo("PROD001");
        assertThat(e.getDetalle()).isEqualTo("alta inicial");
    }

    @Test
    @DisplayName("EventoAuditoria con username null (LOGIN_FALLIDO usuario inexistente)")
    void eventoAuditoria_usernameNull() {
        EventoAuditoria e = EventoAuditoria.crear(null, TipoEvento.LOGIN_FALLIDO,
                null, "usuario no encontrado", RELOJ);
        assertThat(e.getUsername()).isNull();
        assertThat(e.getEntidadAfectada()).isNull();
    }

    // ── Usuario — setIntentosFallidos setter ──────────────────────────

    @Test
    @DisplayName("Usuario.setRol actualiza el rol correctamente")
    void usuario_setRol() {
        Usuario u = new Usuario("user1", "hash", Rol.GESTOR_INVENTARIO);
        assertThat(u.getRol()).isEqualTo(Rol.GESTOR_INVENTARIO);
        u.setRol(Rol.ADMINISTRADOR);
        assertThat(u.getRol()).isEqualTo(Rol.ADMINISTRADOR);
    }
}
