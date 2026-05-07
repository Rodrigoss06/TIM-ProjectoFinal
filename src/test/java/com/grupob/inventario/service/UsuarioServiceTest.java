package com.grupob.inventario.service;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.exception.PermisoDenegadoException;
import com.grupob.inventario.domain.exception.UsuarioDuplicadoException;
import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.persistence.TransactionManagerFake;
import com.grupob.inventario.repository.memory.AuditoriaRepositoryEnMemoria;
import com.grupob.inventario.repository.memory.UsuarioRepositoryEnMemoria;
import com.grupob.inventario.security.PasswordHasher;
import com.grupob.inventario.security.PermisoChecker;
import com.grupob.inventario.security.SesionManager;
import com.grupob.inventario.util.MensajesError;
import com.grupob.inventario.validation.UsuarioValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UsuarioService — RF-INV-007 + superficie de ataque (sección 8.7)")
class UsuarioServiceTest {

    private static String ADMIN_HASH;

    private UsuarioRepositoryEnMemoria usuarioRepo;
    private SesionManager sesionManager;
    private UsuarioService service;
    private PasswordHasher passwordHasher;

    @BeforeAll
    static void hashearPasswordUnaVez() {
        ADMIN_HASH = new PasswordHasher().hashear("admin123!");
    }

    @BeforeEach
    void setUp() {
        usuarioRepo    = new UsuarioRepositoryEnMemoria();
        passwordHasher = new PasswordHasher();
        sesionManager  = new SesionManager(Clock.systemDefaultZone());
        var txFake     = new TransactionManagerFake();
        var permisos   = new PermisoChecker();
        var auditSvc   = new AuditoriaService(new AuditoriaRepositoryEnMemoria(), permisos, Clock.systemDefaultZone(), 50, txFake);
        service = new UsuarioService(usuarioRepo, passwordHasher,
                new UsuarioValidator(), permisos, sesionManager, txFake, auditSvc);
        usuarioRepo.guardar(new Usuario("admin", ADMIN_HASH, Rol.ADMINISTRADOR));
    }

    // ── crearUsuario ──────────────────────────────────────────────────

    @Test
    @DisplayName("RF-007: admin puede crear usuario nuevo")
    void crearUsuario_admin_exitoso() {
        service.crearUsuario("nuevo", "password12", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR);
        assertThat(usuarioRepo.existeUsername("nuevo")).isTrue();
    }

    @Test
    @DisplayName("RF-007: no se puede crear usuario duplicado → UsuarioDuplicadoException")
    void crearUsuario_duplicado_excepcion() {
        service.crearUsuario("nuevo", "password12", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR);
        assertThatThrownBy(() ->
                service.crearUsuario("nuevo", "otrapass1", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR))
                .isInstanceOf(UsuarioDuplicadoException.class)
                .hasMessage(MensajesError.USUARIO_DUPLICADO);
    }

    @Test
    @DisplayName("8.7: password de 7 chars rechazada → ValidacionException PASSWORD_CORTA")
    void crearUsuario_password7Chars_excepcion() {
        assertThatThrownBy(() ->
                service.crearUsuario("x", "1234567", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.PASSWORD_CORTA);
    }

    @Test
    @DisplayName("8.7: password de 8 chars aceptada (límite inferior)")
    void crearUsuario_password8Chars_aceptada() {
        assertThatCode(() ->
                service.crearUsuario("x", "12345678", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RF-007: gestor no puede crear usuarios → PermisoDenegadoException")
    void crearUsuario_gestor_sinPermiso() {
        assertThatThrownBy(() ->
                service.crearUsuario("x", "password12", Rol.GESTOR_INVENTARIO, Rol.GESTOR_INVENTARIO))
                .isInstanceOf(PermisoDenegadoException.class)
                .hasMessage(MensajesError.SIN_PERMISOS);
    }

    @Test
    @DisplayName("RF-007/6.7: password se almacena como BCrypt hash — nunca texto plano")
    void crearUsuario_passwordHasheada_noTextoPlano() {
        service.crearUsuario("nuevo", "miPassword1", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR);
        assertThat(usuarioRepo.buscarPorUsername("nuevo"))
                .isPresent().hasValueSatisfying(u -> {
                    assertThat(u.getPasswordHash()).isNotEqualTo("miPassword1");
                    assertThat(u.getPasswordHash()).startsWith("$2a$");
                    assertThat(new PasswordHasher().verificar("miPassword1", u.getPasswordHash())).isTrue();
                });
    }

    // ── cambiarRol ────────────────────────────────────────────────────

    @Test
    @DisplayName("8.7: degradar al ÚNICO admin → ValidacionException ULTIMO_ADMIN")
    void cambiarRol_unicoAdmin_excepcion() {
        assertThatThrownBy(() ->
                service.cambiarRol("admin", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.ULTIMO_ADMIN);
    }

    @Test
    @DisplayName("8.7: degradar admin cuando hay otro → permitido")
    void cambiarRol_conOtroAdmin_OK() {
        service.crearUsuario("admin2", "admin2pass1", Rol.ADMINISTRADOR, Rol.ADMINISTRADOR);
        assertThatCode(() ->
                service.cambiarRol("admin", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR))
                .doesNotThrowAnyException();
        assertThat(usuarioRepo.buscarPorUsername("admin"))
                .isPresent().hasValueSatisfying(u -> assertThat(u.getRol()).isEqualTo(Rol.GESTOR_INVENTARIO));
    }

    @Test
    @DisplayName("8.7: promover gestor a admin → efecto inmediato (cambio en repo)")
    void cambiarRol_gestorAAdmin_efectoInmediato() {
        service.crearUsuario("gestor1", "gestpass1", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR);
        service.cambiarRol("gestor1", Rol.ADMINISTRADOR, Rol.ADMINISTRADOR);
        assertThat(usuarioRepo.buscarPorUsername("gestor1"))
                .isPresent().hasValueSatisfying(u -> assertThat(u.getRol()).isEqualTo(Rol.ADMINISTRADOR));
    }

    @Test
    @DisplayName("8.7: cambiar rol invalida sesiones activas del usuario afectado")
    void cambiarRol_invalidaSesionesDelUsuario() {
        service.crearUsuario("gestor1", "gestpass1", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR);
        // Simula sesión activa del gestor (inicio directo — sin BCrypt extra)
        Usuario gestor = usuarioRepo.buscarPorUsername("gestor1").get();
        String token = sesionManager.iniciarSesion(gestor);
        assertThat(sesionManager.usernameDeSesion(token)).isPresent();

        service.cambiarRol("gestor1", Rol.ADMINISTRADOR, Rol.ADMINISTRADOR);

        assertThat(sesionManager.usernameDeSesion(token)).isEmpty();
    }

    @Test
    @DisplayName("RF-007: cambio de rol de gestor a admin no afecta sesiones de otros usuarios")
    void cambiarRol_noAfectaSesionesDeOtros() {
        service.crearUsuario("gestor1", "gestpass1", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR);
        service.crearUsuario("gestor2", "gestpass2", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR);

        Usuario g2 = usuarioRepo.buscarPorUsername("gestor2").get();
        String tokenG2 = sesionManager.iniciarSesion(g2);

        service.cambiarRol("gestor1", Rol.ADMINISTRADOR, Rol.ADMINISTRADOR);

        assertThat(sesionManager.usernameDeSesion(tokenG2)).isPresent();
    }

    // ── desactivar ────────────────────────────────────────────────────

    @Test
    @DisplayName("8.7: desactivar al único admin → ValidacionException ULTIMO_ADMIN")
    void desactivar_unicoAdmin_excepcion() {
        assertThatThrownBy(() -> service.desactivar("admin", Rol.ADMINISTRADOR))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.ULTIMO_ADMIN);
    }

    @Test
    @DisplayName("8.7: desactivar admin cuando hay otro → permitido")
    void desactivar_conOtroAdmin_OK() {
        service.crearUsuario("admin2", "admin2pass1", Rol.ADMINISTRADOR, Rol.ADMINISTRADOR);
        assertThatCode(() -> service.desactivar("admin", Rol.ADMINISTRADOR))
                .doesNotThrowAnyException();
        assertThat(usuarioRepo.buscarPorUsername("admin"))
                .isPresent().hasValueSatisfying(u -> assertThat(u.isActivo()).isFalse());
    }

    @Test
    @DisplayName("RF-007: desactivar invalida sesiones del usuario desactivado")
    void desactivar_invalidaSesiones() {
        service.crearUsuario("admin2", "admin2pass1", Rol.ADMINISTRADOR, Rol.ADMINISTRADOR);
        Usuario adminUsuario = usuarioRepo.buscarPorUsername("admin").get();
        String token = sesionManager.iniciarSesion(adminUsuario);

        service.desactivar("admin", Rol.ADMINISTRADOR);

        assertThat(sesionManager.usernameDeSesion(token)).isEmpty();
    }

    @Test
    @DisplayName("RF-007: gestor no puede desactivar usuarios → PermisoDenegadoException")
    void desactivar_gestor_sinPermiso() {
        assertThatThrownBy(() -> service.desactivar("admin", Rol.GESTOR_INVENTARIO))
                .isInstanceOf(PermisoDenegadoException.class)
                .hasMessage(MensajesError.SIN_PERMISOS);
    }

    // ── listar ────────────────────────────────────────────────────────

    @Test
    @DisplayName("RF-007: admin puede listar usuarios")
    void listar_admin_OK() {
        service.crearUsuario("gestor1", "gestpass1", Rol.GESTOR_INVENTARIO, Rol.ADMINISTRADOR);
        assertThat(service.listar(Rol.ADMINISTRADOR)).hasSize(2);
    }

    @Test
    @DisplayName("RF-007: gestor no puede listar usuarios → PermisoDenegadoException")
    void listar_gestor_sinPermiso() {
        assertThatThrownBy(() -> service.listar(Rol.GESTOR_INVENTARIO))
                .isInstanceOf(PermisoDenegadoException.class)
                .hasMessage(MensajesError.SIN_PERMISOS);
    }
}
