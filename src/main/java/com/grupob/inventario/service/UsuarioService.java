package com.grupob.inventario.service;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.exception.UsuarioDuplicadoException;
import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.persistence.GestorTransacciones;
import com.grupob.inventario.repository.UsuarioRepository;
import com.grupob.inventario.security.PasswordHasher;
import com.grupob.inventario.security.PermisoChecker;
import com.grupob.inventario.security.SesionManager;
import com.grupob.inventario.util.MensajesError;
import com.grupob.inventario.validation.UsuarioValidator;

import java.util.List;
import java.util.Objects;

public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordHasher passwordHasher;
    private final UsuarioValidator validator;
    private final PermisoChecker permisoChecker;
    private final SesionManager sesionManager;
    private final GestorTransacciones txManager;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepo, PasswordHasher passwordHasher,
                           UsuarioValidator validator, PermisoChecker permisoChecker,
                           SesionManager sesionManager, GestorTransacciones txManager,
                           AuditoriaService auditoriaService) {
        this.usuarioRepo      = Objects.requireNonNull(usuarioRepo);
        this.passwordHasher   = Objects.requireNonNull(passwordHasher);
        this.validator        = Objects.requireNonNull(validator);
        this.permisoChecker   = Objects.requireNonNull(permisoChecker);
        this.sesionManager    = Objects.requireNonNull(sesionManager);
        this.txManager        = Objects.requireNonNull(txManager);
        this.auditoriaService = Objects.requireNonNull(auditoriaService);
    }

    public void crearUsuario(String username, String password, Rol rol, Rol rolEjecutor) {
        permisoChecker.requierePermiso(rolEjecutor, PermisoChecker.Accion.GESTIONAR_USUARIOS);
        validator.validarCreacion(username, password, rol);

        txManager.enTransaccion(() -> {
            if (usuarioRepo.existeUsername(username)) {
                throw new UsuarioDuplicadoException();
            }
            String hash = passwordHasher.hashear(password);
            usuarioRepo.guardar(new Usuario(username, hash, rol));
            auditoriaService.registrar(rolEjecutor.name(), TipoEvento.CREAR_USUARIO,
                    username, "rol: " + rol.name());
        });
    }

    public void cambiarRol(String username, Rol nuevoRol, Rol rolEjecutor) {
        permisoChecker.requierePermiso(rolEjecutor, PermisoChecker.Accion.GESTIONAR_USUARIOS);
        Objects.requireNonNull(nuevoRol);

        txManager.enTransaccion(() -> {
            Usuario usuario = usuarioRepo.buscarPorUsername(username)
                    .orElseThrow(() -> new InventarioException(MensajesError.PRODUCTO_NO_ENCONTRADO));

            if (usuario.getRol() == Rol.ADMINISTRADOR && nuevoRol != Rol.ADMINISTRADOR) {
                if (usuarioRepo.listarAdministradoresActivos().size() <= 1) {
                    throw new ValidacionException(MensajesError.ULTIMO_ADMIN);
                }
            }

            Rol rolAnterior = usuario.getRol();
            usuario.setRol(nuevoRol);
            usuarioRepo.guardar(usuario);
            sesionManager.invalidarSesionesDe(username);
            auditoriaService.registrar(rolEjecutor.name(), TipoEvento.CAMBIAR_ROL,
                    username, rolAnterior.name() + " → " + nuevoRol.name());
        });
    }

    public void desactivar(String username, Rol rolEjecutor) {
        permisoChecker.requierePermiso(rolEjecutor, PermisoChecker.Accion.GESTIONAR_USUARIOS);

        txManager.enTransaccion(() -> {
            Usuario usuario = usuarioRepo.buscarPorUsername(username)
                    .orElseThrow(() -> new InventarioException(MensajesError.PRODUCTO_NO_ENCONTRADO));

            if (usuario.getRol() == Rol.ADMINISTRADOR && usuario.isActivo()) {
                if (usuarioRepo.listarAdministradoresActivos().size() <= 1) {
                    throw new ValidacionException(MensajesError.ULTIMO_ADMIN);
                }
            }

            usuario.setActivo(false);
            usuarioRepo.guardar(usuario);
            sesionManager.invalidarSesionesDe(username);
            auditoriaService.registrar(rolEjecutor.name(), TipoEvento.DESACTIVAR_USUARIO,
                    username, null);
        });
    }

    public List<Usuario> listar(Rol rolEjecutor) {
        permisoChecker.requierePermiso(rolEjecutor, PermisoChecker.Accion.GESTIONAR_USUARIOS);
        return txManager.soloLectura(() -> usuarioRepo.listarTodos());
    }
}
