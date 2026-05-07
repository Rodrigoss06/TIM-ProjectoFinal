package com.grupob.inventario.security;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Clock mutable: permite avanzar el tiempo sin esperar tiempo real.
 * Patrón: campo 'tiempoActual' que el Clock anónimo lee en cada llamada.
 */
@DisplayName("SesionManager — sesiones con Clock.fixed mutable (6.4 y 8.6 Notion)")
class SesionManagerTest {

    private static final Instant T0 = Instant.parse("2026-04-29T10:00:00Z");

    private Instant tiempoActual;

    private final Clock relojMutable = new Clock() {
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return tiempoActual; }
    };

    private SesionManager sm;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        tiempoActual = T0;
        sm = new SesionManager(relojMutable);
        usuario = new Usuario("juan", "hashFictico", Rol.GESTOR_INVENTARIO);
    }

    // ── ciclo básico ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("sesión recién iniciada es válida en t=0")
    void sesion_tCero_valida() {
        String token = sm.iniciarSesion(usuario);
        assertThat(sm.usernameDeSesion(token)).isPresent()
                .hasValue("juan");
    }

    @Test
    @DisplayName("iniciarSesion genera tokens distintos para sesiones distintas")
    void iniciarSesion_tokensDistintos() {
        String t1 = sm.iniciarSesion(usuario);
        String t2 = sm.iniciarSesion(usuario);
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("token inexistente devuelve Optional vacío")
    void usernameDeSesion_tokenInexistente_vacio() {
        assertThat(sm.usernameDeSesion("token-falso")).isEmpty();
    }

    @Test
    @DisplayName("token null devuelve Optional vacío sin excepción")
    void usernameDeSesion_tokenNull_vacio() {
        assertThat(sm.usernameDeSesion(null)).isEmpty();
    }

    // ── expiración por inactividad (RF-INV-006: 30 min) ──────────────────────

    @Test
    @DisplayName("sesión válida a t=29min (justo antes de expirar)")
    void sesion_t29min_valida() {
        String token = sm.iniciarSesion(usuario);
        tiempoActual = T0.plus(Duration.ofMinutes(29));
        assertThat(sm.usernameDeSesion(token)).isPresent();
    }

    @Test
    @DisplayName("sesión expirada a t=30min (límite exacto — RF: 30 min de inactividad)")
    void sesion_t30min_expirada() {
        String token = sm.iniciarSesion(usuario);
        tiempoActual = T0.plus(Duration.ofMinutes(30));
        assertThat(sm.usernameDeSesion(token)).isEmpty();
    }

    @Test
    @DisplayName("sesión expirada a t=31min")
    void sesion_t31min_expirada() {
        String token = sm.iniciarSesion(usuario);
        tiempoActual = T0.plus(Duration.ofMinutes(31));
        assertThat(sm.usernameDeSesion(token)).isEmpty();
    }

    @Test
    @DisplayName("sesión expirada se elimina del mapa al consultarla")
    void sesion_expirada_seEliminaAlConsultar() {
        String token = sm.iniciarSesion(usuario);
        tiempoActual = T0.plus(Duration.ofMinutes(31));
        sm.usernameDeSesion(token);
        // Segunda consulta también devuelve vacío (no resucita)
        assertThat(sm.usernameDeSesion(token)).isEmpty();
    }

    // ── tocarSesion extiende la ventana ───────────────────────────────────────

    @Test
    @DisplayName("tocarSesion a t=29min extiende: válida a t=31min (que habría expirado sin touch)")
    void tocarSesion_extiendeLaVentana() {
        String token = sm.iniciarSesion(usuario);

        tiempoActual = T0.plus(Duration.ofMinutes(29));
        sm.tocarSesion(token);

        // Sin touch, la sesión habría expirado a t=30min.
        // Con touch a t=29min, expira a t=29+30=59min.
        tiempoActual = T0.plus(Duration.ofMinutes(31));
        assertThat(sm.usernameDeSesion(token)).isPresent();
    }

    @Test
    @DisplayName("tocarSesion tras el touch: expiración se corre al nuevo límite")
    void tocarSesion_expiracionCorrida() {
        String token = sm.iniciarSesion(usuario);

        // Toca a t=29min → nueva expiración a t=59min
        tiempoActual = T0.plus(Duration.ofMinutes(29));
        sm.tocarSesion(token);

        // A t=58min: válida (58 < 59)
        tiempoActual = T0.plus(Duration.ofMinutes(58));
        assertThat(sm.usernameDeSesion(token)).isPresent();

        // A t=59min: expirada (límite exacto)
        tiempoActual = T0.plus(Duration.ofMinutes(59));
        assertThat(sm.usernameDeSesion(token)).isEmpty();
    }

    @Test
    @DisplayName("tocarSesion con token null no lanza excepción")
    void tocarSesion_tokenNull_sinExcepcion() {
        sm.tocarSesion(null);
    }

    // ── cerrarSesion ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("cerrarSesion invalida el token inmediatamente")
    void cerrarSesion_tokenInvalidado() {
        String token = sm.iniciarSesion(usuario);
        sm.cerrarSesion(token);
        assertThat(sm.usernameDeSesion(token)).isEmpty();
    }

    // ── invalidarSesionesDe ───────────────────────────────────────────────────

    @Test
    @DisplayName("invalidarSesionesDe elimina todas las sesiones del usuario (8.7 Notion)")
    void invalidarSesionesDe_eliminaTodasLasSesionesDelUsuario() {
        String t1 = sm.iniciarSesion(usuario);
        String t2 = sm.iniciarSesion(usuario);
        Usuario otro = new Usuario("maria", "hash2", Rol.ADMINISTRADOR);
        String t3 = sm.iniciarSesion(otro);

        sm.invalidarSesionesDe("juan");

        assertThat(sm.usernameDeSesion(t1)).isEmpty();
        assertThat(sm.usernameDeSesion(t2)).isEmpty();
        assertThat(sm.usernameDeSesion(t3)).isPresent(); // sesión de otro usuario intacta
    }
}
