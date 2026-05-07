package com.grupob.inventario.service;

import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.exception.CuentaBloqueadaException;
import com.grupob.inventario.domain.exception.CredencialesInvalidasException;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.persistence.GestorTransacciones;
import com.grupob.inventario.repository.UsuarioRepository;
import com.grupob.inventario.security.PasswordHasher;
import com.grupob.inventario.security.SesionManager;
import com.grupob.inventario.util.MensajesError;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class AutenticacionService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordHasher passwordHasher;
    private final SesionManager sesionManager;
    private final Clock clock;
    private final GestorTransacciones txManager;
    private final AuditoriaService auditoriaService;

    public AutenticacionService(UsuarioRepository usuarioRepo, PasswordHasher passwordHasher,
                                 SesionManager sesionManager, Clock clock,
                                 GestorTransacciones txManager, AuditoriaService auditoriaService) {
        this.usuarioRepo      = Objects.requireNonNull(usuarioRepo);
        this.passwordHasher   = Objects.requireNonNull(passwordHasher);
        this.sesionManager    = Objects.requireNonNull(sesionManager);
        this.clock            = Objects.requireNonNull(clock);
        this.txManager        = Objects.requireNonNull(txManager);
        this.auditoriaService = Objects.requireNonNull(auditoriaService);
    }

    /**
     * RF-INV-006. Mismo mensaje para usuario inexistente y password incorrecta (sección 6.8).
     * Cada intento fallido genera su propio evento de auditoría dentro de una transacción.
     */
    public String login(String username, String password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);

        Optional<Usuario> opt = txManager.soloLectura(() -> usuarioRepo.buscarPorUsername(username));

        // Usuario no existe o está inactivo → mismo mensaje que password incorrecta (6.8)
        if (opt.isEmpty() || !opt.get().isActivo()) {
            txManager.enTransaccion(() ->
                auditoriaService.registrar(null, TipoEvento.LOGIN_FALLIDO, null,
                        "usuario no encontrado: " + username));
            throw new CredencialesInvalidasException();
        }

        Usuario usuario = opt.get();

        // Cuenta ya bloqueada antes del intento
        if (usuario.estaBloqueado(clock)) {
            throw new CuentaBloqueadaException(minutosRestantes(usuario));
        }

        // Password incorrecta
        if (!passwordHasher.verificar(password, usuario.getPasswordHash())) {
            txManager.enTransaccion(() -> {
                usuario.registrarIntentoFallido(clock);
                usuarioRepo.guardar(usuario);
                auditoriaService.registrar(username, TipoEvento.LOGIN_FALLIDO, null,
                        "intento fallido: " + usuario.getIntentosFallidos());
                if (usuario.estaBloqueado(clock)) {
                    auditoriaService.registrar(username, TipoEvento.CUENTA_BLOQUEADA, null,
                            "bloqueado por 15 min");
                }
            });

            if (usuario.estaBloqueado(clock)) {
                throw new CuentaBloqueadaException(minutosRestantes(usuario));
            }
            throw new CredencialesInvalidasException();
        }

        // Login exitoso
        String token = txManager.enTransaccion(() -> {
            usuario.resetIntentos();
            usuarioRepo.guardar(usuario);
            String t = sesionManager.iniciarSesion(usuario);
            auditoriaService.registrar(username, TipoEvento.LOGIN_EXITOSO, null, null);
            return t;
        });

        return token;
    }

    public void logout(String token) {
        String username = sesionManager.usernameDeSesion(token).orElse(null);
        sesionManager.cerrarSesion(token);
        if (username != null) {
            txManager.enTransaccion(() ->
                auditoriaService.registrar(username, TipoEvento.LOGOUT, null, null));
        }
    }

    public Usuario usuarioActual(String token) {
        String username = sesionManager.usernameDeSesion(token)
                .orElseThrow(() -> new InventarioException(MensajesError.SESION_EXPIRADA));
        sesionManager.tocarSesion(token);
        return txManager.soloLectura(() ->
                usuarioRepo.buscarPorUsername(username)
                        .orElseThrow(() -> new InventarioException(MensajesError.SESION_EXPIRADA)));
    }

    private int minutosRestantes(Usuario usuario) {
        long segundos = Duration.between(Instant.now(clock), usuario.getBloqueadoHasta()).getSeconds();
        return (int) Math.max(1, (long) Math.ceil(segundos / 60.0));
    }
}
