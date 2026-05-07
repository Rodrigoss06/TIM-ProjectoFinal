package com.grupob.inventario.service;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.model.EventoAuditoria;
import com.grupob.inventario.domain.exception.CuentaBloqueadaException;
import com.grupob.inventario.domain.exception.CredencialesInvalidasException;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.persistence.TransactionManagerFake;
import com.grupob.inventario.repository.FiltroAuditoria;
import com.grupob.inventario.repository.memory.AuditoriaRepositoryEnMemoria;
import com.grupob.inventario.repository.memory.UsuarioRepositoryEnMemoria;
import com.grupob.inventario.security.PasswordHasher;
import com.grupob.inventario.security.PermisoChecker;
import com.grupob.inventario.security.SesionManager;
import com.grupob.inventario.util.MensajesError;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AutenticacionService — RF-INV-006 + superficie de ataque (sección 8.6)")
class AutenticacionServiceTest {

    private static final Instant T0 = Instant.parse("2026-04-29T10:00:00Z");
    private static String JUAN_HASH;

    // Reloj mutable: avanzamos tiempoActual en los tests que requieren control de tiempo
    private Instant tiempoActual;
    private final Clock relojMutable = new Clock() {
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return tiempoActual; }
    };

    private UsuarioRepositoryEnMemoria  usuarioRepo;
    private SesionManager              sesionManager;
    private AuditoriaRepositoryEnMemoria auditoriaRepo;
    private AutenticacionService        service;

    @BeforeAll
    static void hashearPasswordUnaVez() {
        JUAN_HASH = new PasswordHasher().hashear("password1");
    }

    @BeforeEach
    void setUp() {
        tiempoActual  = T0;
        usuarioRepo   = new UsuarioRepositoryEnMemoria();
        sesionManager = new SesionManager(relojMutable);
        auditoriaRepo = new AuditoriaRepositoryEnMemoria();
        var txFake    = new TransactionManagerFake();
        var auditSvc  = new AuditoriaService(auditoriaRepo, new PermisoChecker(), relojMutable, 50, txFake);
        service = new AutenticacionService(usuarioRepo, new PasswordHasher(), sesionManager, relojMutable, txFake, auditSvc);
        usuarioRepo.guardar(new Usuario("juan", JUAN_HASH, Rol.GESTOR_INVENTARIO));
    }

    // ── login exitoso ─────────────────────────────────────────────────

    @Test
    @DisplayName("RF-006: login exitoso con credenciales válidas devuelve token no vacío")
    void login_exitoso_retornaToken() {
        String token = service.login("juan", "password1");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("RF-006: login exitoso resetea contador de intentos fallidos")
    void login_exitosoTrasFallos_resetaIntentos() {
        try { service.login("juan", "mal"); } catch (Exception ignored) {}
        try { service.login("juan", "mal"); } catch (Exception ignored) {}
        service.login("juan", "password1");

        assertThat(usuarioRepo.buscarPorUsername("juan"))
                .isPresent()
                .hasValueSatisfying(u -> assertThat(u.getIntentosFallidos()).isEqualTo(0));
    }

    // ── credenciales inválidas — mismo mensaje (sección 6.8) ──────────

    @Test
    @DisplayName("RF-006: usuario inexistente → CredencialesInvalidasException + auditoría con username=null")
    void login_usuarioInexistente_credencialesInvalidas() {
        assertThatThrownBy(() -> service.login("noexiste", "cualquier"))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage(MensajesError.CREDENCIALES_INVALIDAS);

        // RF-INV-009: evento LOGIN_FALLIDO con username=null (usuario inexistente)
        var eventos = auditoriaRepo.consultar(new FiltroAuditoria(null, null, null, TipoEvento.LOGIN_FALLIDO), 0, 10);
        assertThat(eventos).hasSize(1)
                .extracting(EventoAuditoria::getUsername).containsNull();
    }

    @Test
    @DisplayName("RF-006: password incorrecta → CredencialesInvalidasException con mensaje exacto")
    void login_passwordIncorrecta_credencialesInvalidas() {
        assertThatThrownBy(() -> service.login("juan", "wrongpass"))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage(MensajesError.CREDENCIALES_INVALIDAS);
    }

    @Test
    @DisplayName("6.8: usuario inexistente y password incorrecta dan EXACTAMENTE el mismo mensaje")
    void login_mensajeIdentico_usuarioInexistenteYPasswordMal() {
        String msgInexistente = null;
        try { service.login("noexiste", "cualquier"); } catch (CredencialesInvalidasException e) {
            msgInexistente = e.getMessage();
        }

        String msgPasswordMal = null;
        try { service.login("juan", "malpassword"); } catch (CredencialesInvalidasException e) {
            msgPasswordMal = e.getMessage();
        }

        assertThat(msgInexistente)
                .isEqualTo(msgPasswordMal)
                .isEqualTo(MensajesError.CREDENCIALES_INVALIDAS);
    }

    @Test
    @DisplayName("RF-006: usuario inactivo también devuelve CredencialesInvalidasException")
    void login_usuarioInactivo_credencialesInvalidas() {
        usuarioRepo.buscarPorUsername("juan").ifPresent(u -> {
            u.setActivo(false);
            usuarioRepo.guardar(u);
        });
        assertThatThrownBy(() -> service.login("juan", "password1"))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage(MensajesError.CREDENCIALES_INVALIDAS);
    }

    // ── lockout de 3 intentos (sección 8.6) ──────────────────────────

    @Test
    @DisplayName("8.6: primer intento fallido → CredencialesInvalidasException (no bloqueado aún)")
    void login_primerIntentoFallido_noBloquea() {
        assertThatThrownBy(() -> service.login("juan", "mal"))
                .isInstanceOf(CredencialesInvalidasException.class);
        assertThat(usuarioRepo.buscarPorUsername("juan"))
                .isPresent().hasValueSatisfying(u -> assertThat(u.getIntentosFallidos()).isEqualTo(1));
    }

    @Test
    @DisplayName("8.6: 3 intentos → 3er bloquea + auditoría: 3 LOGIN_FALLIDO + 1 CUENTA_BLOQUEADA")
    void login_tresIntentosFallidos_terceroBloquea() {
        try { service.login("juan", "mal"); } catch (Exception ignored) {}
        try { service.login("juan", "mal"); } catch (Exception ignored) {}

        assertThatThrownBy(() -> service.login("juan", "mal"))
                .isInstanceOf(CuentaBloqueadaException.class)
                .hasMessage(String.format(MensajesError.CUENTA_BLOQUEADA_FMT, 15));

        // RF-INV-009: 3 LOGIN_FALLIDO + 1 CUENTA_BLOQUEADA
        var todos = auditoriaRepo.consultar(new FiltroAuditoria(null, null, null, null), 0, 10);
        assertThat(todos.stream().filter(e -> e.getTipoEvento() == TipoEvento.LOGIN_FALLIDO).count()).isEqualTo(3);
        assertThat(todos.stream().filter(e -> e.getTipoEvento() == TipoEvento.CUENTA_BLOQUEADA).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("8.6: 4to intento con password CORRECTA dentro de 15 min → sigue bloqueado")
    void login_4toIntentoCorrecto_dentroDe15min_bloqueado() {
        tiempoActual = T0;
        try { service.login("juan", "mal"); } catch (Exception ignored) {}
        try { service.login("juan", "mal"); } catch (Exception ignored) {}
        try { service.login("juan", "mal"); } catch (Exception ignored) {}

        tiempoActual = T0.plus(Duration.ofMinutes(14).plusSeconds(59));
        assertThatThrownBy(() -> service.login("juan", "password1"))
                .isInstanceOf(CuentaBloqueadaException.class);
    }

    @Test
    @DisplayName("8.6: exactamente a los 15 min → cuenta desbloqueada automáticamente")
    void login_exactamente15min_desbloqueado() {
        tiempoActual = T0;
        try { service.login("juan", "mal"); } catch (Exception ignored) {}
        try { service.login("juan", "mal"); } catch (Exception ignored) {}
        try { service.login("juan", "mal"); } catch (Exception ignored) {}

        tiempoActual = T0.plus(Duration.ofMinutes(15));
        assertThatCode(() -> service.login("juan", "password1"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("8.6: ya bloqueado al entrar a login → lanza CuentaBloqueadaException directamente")
    void login_cuentaYaBloqueada_excepcionInmediata() {
        tiempoActual = T0;
        try { service.login("juan", "mal"); } catch (Exception ignored) {}
        try { service.login("juan", "mal"); } catch (Exception ignored) {}
        try { service.login("juan", "mal"); } catch (Exception ignored) {}

        tiempoActual = T0.plus(Duration.ofMinutes(5));
        assertThatThrownBy(() -> service.login("juan", "mal"))
                .isInstanceOf(CuentaBloqueadaException.class);
    }

    // ── sesiones (RF-INV-006) ─────────────────────────────────────────

    @Test
    @DisplayName("RF-006: token da acceso a usuarioActual mientras la sesión es válida")
    void usuarioActual_sesionValida_retornaUsuario() {
        String token = service.login("juan", "password1");
        Usuario u = service.usuarioActual(token);
        assertThat(u.getUsername()).isEqualTo("juan");
    }

    @Test
    @DisplayName("8.6: sesión válida a t=29min, expirada a t=31min de inactividad")
    void sesion_validaA29min_expiradaA31min() {
        tiempoActual = T0;
        String token = service.login("juan", "password1");

        // A t=29min → válida
        tiempoActual = T0.plus(Duration.ofMinutes(29));
        assertThatCode(() -> service.usuarioActual(token)).doesNotThrowAnyException();

        // Nuevo token (la primera llamada extendió la sesión)
        tiempoActual = T0;
        String token2 = service.login("juan", "password1");

        // Avanzar 31 min SIN llamar usuarioActual → expira
        tiempoActual = T0.plus(Duration.ofMinutes(31));
        assertThatThrownBy(() -> service.usuarioActual(token2))
                .isInstanceOf(InventarioException.class)
                .hasMessage(MensajesError.SESION_EXPIRADA);
    }

    @Test
    @DisplayName("RF-006: logout invalida el token")
    void logout_invalidaToken() {
        String token = service.login("juan", "password1");
        service.logout(token);
        assertThatThrownBy(() -> service.usuarioActual(token))
                .isInstanceOf(InventarioException.class)
                .hasMessage(MensajesError.SESION_EXPIRADA);
    }

    @Test
    @DisplayName("8.6: BCrypt verificable — hash en repo no es el password en texto plano")
    void login_hashEnRepoNoEsTextoPlano() {
        assertThat(usuarioRepo.buscarPorUsername("juan"))
                .isPresent()
                .hasValueSatisfying(u -> {
                    assertThat(u.getPasswordHash()).isNotEqualTo("password1");
                    assertThat(u.getPasswordHash()).startsWith("$2a$");
                });
    }
}
