package com.grupob.inventario.ui.fx.controller;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.service.UsuarioService;
import com.grupob.inventario.ui.fx.util.Alertas;
import com.grupob.inventario.ui.fx.util.EstadoSesion;
import com.grupob.inventario.ui.fx.util.NavegacionFx;
import com.grupob.inventario.util.MensajesError;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class UsuarioFormController {

    @FXML private TextField     username;
    @FXML private PasswordField password;
    @FXML private PasswordField passwordConfirm;
    @FXML private ComboBox<Rol> rolCombo;
    @FXML private Label         errorLabel;
    @FXML private Button        guardarBtn;

    private boolean guardado = false;

    public boolean isSaved() { return guardado; }

    @FXML
    public void initialize() {
        rolCombo.setItems(FXCollections.observableArrayList(Rol.values()));
        rolCombo.setValue(Rol.GESTOR_INVENTARIO);
        rolCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Rol r) {
                return r == null ? "" : switch (r) {
                    case ADMINISTRADOR    -> "Administrador";
                    case GESTOR_INVENTARIO -> "Gestor de inventario";
                };
            }
            @Override public Rol fromString(String s) { return null; }
        });
        ocultarError();
    }

    public void inicializar() { /* defaults en initialize() son suficientes */ }

    @FXML
    public void guardar(ActionEvent event) {
        String user       = username.getText().trim();
        String pass       = password.getText();
        String passConf   = passwordConfirm.getText();
        Rol    rol        = rolCombo.getValue();

        // Validaciones de formato en UI
        if (user.isBlank() || pass.isEmpty() || passConf.isEmpty()) {
            mostrarError(MensajesError.CAMPOS_OBLIGATORIOS);
            return;
        }
        if (!pass.equals(passConf)) {
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        ocultarError();
        guardarBtn.setDisable(true);

        Rol rolActor = EstadoSesion.getRol();
        UsuarioService svc = NavegacionFx.getContexto().getUsuarioService();

        // La validación de longitud mínima queda al servicio → mensaje exacto de MensajesError
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                svc.crearUsuario(user, pass, rol, rolActor);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            guardado = true;
            cerrarVentana();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof InventarioException) mostrarError(ex.getMessage());
            else Alertas.inesperado(ex);
            guardarBtn.setDisable(false);
        });
        new Thread(task, "crear-usuario").start();
    }

    @FXML
    public void cancelar(ActionEvent event) { cerrarVentana(); }

    private void mostrarError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void ocultarError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void cerrarVentana() {
        ((Stage) guardarBtn.getScene().getWindow()).close();
    }
}
