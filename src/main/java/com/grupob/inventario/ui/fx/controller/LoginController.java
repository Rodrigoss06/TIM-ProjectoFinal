package com.grupob.inventario.ui.fx.controller;

import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.service.AutenticacionService;
import com.grupob.inventario.ui.fx.util.Alertas;
import com.grupob.inventario.ui.fx.util.EstadoSesion;
import com.grupob.inventario.ui.fx.util.NavegacionFx;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller para login.fxml.
 * Reglas (sección 8.2 Notion etapa 2):
 * - No tiene reglas de negocio: delega en AutenticacionService.
 * - Mensajes de error = e.getMessage() (vienen de MensajesError).
 * - Login en Task de fondo para no bloquear el FX thread.
 * - Botón deshabilitado mientras la operación está en curso.
 */
public class LoginController {

    @FXML private TextField     usuario;
    @FXML private PasswordField contrasena;
    @FXML private Button        entrar;
    @FXML private Label         error;

    @FXML
    public void initialize() {
        error.setVisible(false);
        error.setManaged(false);
        // Enter en el campo de contraseña dispara el login
        contrasena.setOnAction(this::entrar);
    }

    @FXML
    public void entrar(ActionEvent event) {
        String username = usuario.getText().trim();
        String password = contrasena.getText();

        if (username.isBlank() || password.isEmpty()) {
            mostrarError("Complete usuario y contraseña.");
            return;
        }

        ocultarError();
        entrar.setDisable(true);

        AutenticacionService authSvc = NavegacionFx.getContexto().getAutenticacionService();

        Task<Void> task = new Task<>() {
            private String  tokenObtenido;
            private Usuario usuarioObtenido;

            @Override
            protected Void call() {
                tokenObtenido   = authSvc.login(username, password);
                usuarioObtenido = authSvc.usuarioActual(tokenObtenido);
                EstadoSesion.iniciar(tokenObtenido, usuarioObtenido);
                return null;
            }
        };

        task.setOnSucceeded(e -> NavegacionFx.mostrar("main"));

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof InventarioException) {
                mostrarError(ex.getMessage());
            } else {
                Alertas.inesperado(ex);
            }
            entrar.setDisable(false);
        });

        new Thread(task, "login-task").start();
    }

    private void mostrarError(String msg) {
        error.setText(msg);
        error.setVisible(true);
        error.setManaged(true);
    }

    private void ocultarError() {
        error.setVisible(false);
        error.setManaged(false);
    }
}
