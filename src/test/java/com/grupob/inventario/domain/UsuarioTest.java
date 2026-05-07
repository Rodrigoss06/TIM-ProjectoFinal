package com.grupob.inventario.domain;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Usuario — bloqueo de cuenta, relojes y seguridad de toString")
class UsuarioTest {

    private static final Instant T0 = Instant.parse("2026-04-29T10:00:00Z");
    private static final Clock RELOJ_T0 = Clock.fixed(T0, ZoneOffset.UTC);

    private Usuario usuarioNuevo() {
        return new Usuario("juan", "$2a$12$hashFicticioParaTest", Rol.GESTOR_INVENTARIO);
    }

    // ── estado inicial ────────────────────────────────────────────────────────

    @Test
    @DisplayName("usuario recién creado no está bloqueado")
    void usuarioNuevo_noBloqueado() {
        assertThat(usuarioNuevo().estaBloqueado(RELOJ_T0)).isFalse();
    }

    @Test
    @DisplayName("usuario recién creado tiene 0 intentos fallidos")
    void usuarioNuevo_ceroIntentosFallidos() {
        assertThat(usuarioNuevo().getIntentosFallidos()).isEqualTo(0);
    }

    // ── bloqueo progresivo ────────────────────────────────────────────────────

    @Test
    @DisplayName("un intento fallido no bloquea la cuenta")
    void unIntentoFallido_noBloqueado() {
        Usuario u = usuarioNuevo();
        u.registrarIntentoFallido(RELOJ_T0);
        assertThat(u.estaBloqueado(RELOJ_T0)).isFalse();
        assertThat(u.getIntentosFallidos()).isEqualTo(1);
    }

    @Test
    @DisplayName("dos intentos fallidos no bloquean la cuenta (límite inferior del bloqueo)")
    void dosIntentosFallidos_noBloqueado() {
        Usuario u = usuarioNuevo();
        u.registrarIntentoFallido(RELOJ_T0);
        u.registrarIntentoFallido(RELOJ_T0);
        assertThat(u.estaBloqueado(RELOJ_T0)).isFalse();
        assertThat(u.getIntentosFallidos()).isEqualTo(2);
    }

    @Test
    @DisplayName("tercer intento fallido bloquea la cuenta (RF-INV-006: 3 intentos)")
    void tresIntentosFallidos_bloqueado() {
        Usuario u = usuarioNuevo();
        u.registrarIntentoFallido(RELOJ_T0);
        u.registrarIntentoFallido(RELOJ_T0);
        u.registrarIntentoFallido(RELOJ_T0);
        assertThat(u.estaBloqueado(RELOJ_T0)).isTrue();
    }

    // ── duración del bloqueo ──────────────────────────────────────────────────

    @Test
    @DisplayName("14 min 59 seg después del bloqueo la cuenta sigue bloqueada")
    void bloqueado_antes15min_sigueBloquado() {
        Usuario u = usuarioNuevo();
        u.registrarIntentoFallido(RELOJ_T0);
        u.registrarIntentoFallido(RELOJ_T0);
        u.registrarIntentoFallido(RELOJ_T0);

        Clock relojCasi15Min = Clock.fixed(T0.plus(Duration.ofMinutes(14).plusSeconds(59)), ZoneOffset.UTC);
        assertThat(u.estaBloqueado(relojCasi15Min)).isTrue();
    }

    @Test
    @DisplayName("exactamente 15 min después del bloqueo la cuenta se desbloquea automáticamente")
    void bloqueado_exactamente15min_desbloqueado() {
        Usuario u = usuarioNuevo();
        u.registrarIntentoFallido(RELOJ_T0);
        u.registrarIntentoFallido(RELOJ_T0);
        u.registrarIntentoFallido(RELOJ_T0);

        Clock reloj15Min = Clock.fixed(T0.plus(Duration.ofMinutes(15)), ZoneOffset.UTC);
        assertThat(u.estaBloqueado(reloj15Min)).isFalse();
    }

    // ── resetIntentos (login exitoso) ─────────────────────────────────────────

    @Test
    @DisplayName("resetIntentos limpia contador y bloqueo tras login exitoso")
    void resetIntentos_limpiaContadorYBloqueo() {
        Usuario u = usuarioNuevo();
        u.registrarIntentoFallido(RELOJ_T0);
        u.registrarIntentoFallido(RELOJ_T0);
        u.registrarIntentoFallido(RELOJ_T0);
        assertThat(u.estaBloqueado(RELOJ_T0)).isTrue();

        u.resetIntentos();

        assertThat(u.getIntentosFallidos()).isEqualTo(0);
        assertThat(u.getBloqueadoHasta()).isNull();
        assertThat(u.estaBloqueado(RELOJ_T0)).isFalse();
    }

    @Test
    @DisplayName("resetIntentos sobre cuenta sin bloqueo no lanza excepción")
    void resetIntentos_sinBloqueo_sinExcepcion() {
        Usuario u = usuarioNuevo();
        u.resetIntentos();
        assertThat(u.getIntentosFallidos()).isEqualTo(0);
    }

    // ── seguridad: toString ───────────────────────────────────────────────────

    @Test
    @DisplayName("toString no expone el passwordHash (RF-INV-006 y 6.7)")
    void toString_noContienePasswordHash() {
        String hash = "$2a$12$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ0123456";
        Usuario u = new Usuario("admin", hash, Rol.ADMINISTRADOR);
        assertThat(u.toString()).doesNotContain(hash);
        assertThat(u.toString()).contains("admin");
    }

    // ── cobertura adicional ───────────────────────────────────────────

    @Test
    @DisplayName("bloquearPor asigna bloqueadoHasta correctamente")
    void bloquearPor_asignaBloqueadoHasta() {
        Usuario u = usuarioNuevo();
        u.bloquearPor(Duration.ofMinutes(10), RELOJ_T0);
        assertThat(u.getBloqueadoHasta())
                .isEqualTo(T0.plus(Duration.ofMinutes(10)));
        assertThat(u.estaBloqueado(RELOJ_T0)).isTrue();
    }

    @Test
    @DisplayName("hashCode es consistente con equals (basado en username)")
    void hashCode_consistenteConEquals() {
        Usuario u1 = new Usuario("mismo", "hash1", Rol.ADMINISTRADOR);
        Usuario u2 = new Usuario("mismo", "hash2", Rol.GESTOR_INVENTARIO);
        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }

    @Test
    @DisplayName("CuentaBloqueadaException.getMinutosRestantes retorna valor correcto")
    void cuentaBloqueadaException_minutosRestantes() {
        var ex = new com.grupob.inventario.domain.exception.CuentaBloqueadaException(15);
        assertThat(ex.getMinutosRestantes()).isEqualTo(15);
        assertThat(ex.getMessage())
                .isEqualTo(String.format(com.grupob.inventario.util.MensajesError.CUENTA_BLOQUEADA_FMT, 15));
    }
}
