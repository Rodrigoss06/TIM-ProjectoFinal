package com.grupob.inventario.ui.fx.controller;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.service.UsuarioService;
import com.grupob.inventario.ui.fx.util.Alertas;
import com.grupob.inventario.ui.fx.util.EstadoSesion;
import com.grupob.inventario.ui.fx.util.NavegacionFx;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * RF-INV-007 — Gestión de usuarios.
 * Solo accesible para ADMINISTRADOR; el servicio también valida.
 */
public class UsuariosController {

    @FXML private TableView<Usuario>             tablaUsuarios;
    @FXML private TableColumn<Usuario, String>   colUsername;
    @FXML private TableColumn<Usuario, String>   colRol;
    @FXML private TableColumn<Usuario, String>   colActivo;
    @FXML private Label                          totalLabel;

    @FXML
    public void initialize() {
        colUsername.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getUsername()));
        colRol.setCellValueFactory(c ->
            new SimpleStringProperty(formatearRol(c.getValue().getRol())));
        colActivo.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().isActivo() ? "Sí" : "No"));

        cargar(null);
    }

    // ── Cargar ────────────────────────────────────────────────────────

    @FXML
    public void cargar(ActionEvent event) {
        UsuarioService svc = NavegacionFx.getContexto().getUsuarioService();
        Rol rol = EstadoSesion.getRol();

        Task<List<Usuario>> task = new Task<>() {
            @Override protected List<Usuario> call() { return svc.listar(rol); }
        };
        task.setOnSucceeded(e -> {
            List<Usuario> lista = task.getValue();
            tablaUsuarios.setItems(FXCollections.observableArrayList(lista));
            if (totalLabel != null) totalLabel.setText(lista.size() + " usuario(s)");
        });
        task.setOnFailed(e -> manejarError(task.getException()));
        new Thread(task, "listar-usuarios").start();
    }

    // ── Nuevo ─────────────────────────────────────────────────────────

    @FXML
    public void nuevo(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fx/usuario-form.fxml"));
            Parent root = loader.load();
            UsuarioFormController ctrl = loader.getController();
            ctrl.inicializar();

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Nuevo Usuario");
            modal.setScene(new Scene(root));
            modal.setResizable(false);
            modal.showAndWait();

            if (ctrl.isSaved()) cargar(null);
        } catch (Exception e) {
            Alertas.inesperado(e);
        }
    }

    // ── Cambiar rol ───────────────────────────────────────────────────

    @FXML
    public void cambiarRol(ActionEvent event) {
        Usuario sel = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (sel == null) { Alertas.error("Seleccione un usuario."); return; }

        Rol sugerido = sel.getRol() == Rol.ADMINISTRADOR
                ? Rol.GESTOR_INVENTARIO : Rol.ADMINISTRADOR;
        ChoiceDialog<Rol> dialog = new ChoiceDialog<>(sugerido, Arrays.asList(Rol.values()));
        dialog.setTitle("Cambiar Rol");
        dialog.setHeaderText("Usuario: " + sel.getUsername());
        dialog.setContentText("Nuevo rol:");

        Optional<Rol> resultado = dialog.showAndWait();
        if (resultado.isEmpty()) return;

        Rol nuevoRol = resultado.get();
        Rol rolActor = EstadoSesion.getRol();
        UsuarioService svc = NavegacionFx.getContexto().getUsuarioService();

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                svc.cambiarRol(sel.getUsername(), nuevoRol, rolActor);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            Alertas.info("Rol actualizado. La sesión activa del usuario fue invalidada.");
            cargar(null);
        });
        task.setOnFailed(e -> manejarError(task.getException()));
        new Thread(task, "cambiar-rol").start();
    }

    // ── Desactivar ────────────────────────────────────────────────────

    @FXML
    public void desactivar(ActionEvent event) {
        Usuario sel = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (sel == null) { Alertas.error("Seleccione un usuario."); return; }

        if (!Alertas.confirmar("¿Desactivar la cuenta de '" + sel.getUsername() + "'?\n" +
                "El usuario no podrá iniciar sesión.")) return;

        Rol rol = EstadoSesion.getRol();
        UsuarioService svc = NavegacionFx.getContexto().getUsuarioService();

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                svc.desactivar(sel.getUsername(), rol);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            Alertas.info("Usuario desactivado correctamente.");
            cargar(null);
        });
        task.setOnFailed(e -> manejarError(task.getException()));
        new Thread(task, "desactivar-usuario").start();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static void manejarError(Throwable ex) {
        if (ex instanceof InventarioException) Alertas.error(ex.getMessage());
        else Alertas.inesperado(ex);
    }

    private static String formatearRol(Rol r) {
        return switch (r) {
            case ADMINISTRADOR    -> "Administrador";
            case GESTOR_INVENTARIO -> "Gestor de inventario";
        };
    }
}
