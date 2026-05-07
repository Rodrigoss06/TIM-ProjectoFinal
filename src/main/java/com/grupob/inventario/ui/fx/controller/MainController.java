package com.grupob.inventario.ui.fx.controller;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.service.AutenticacionService;
import com.grupob.inventario.ui.fx.util.Alertas;
import com.grupob.inventario.ui.fx.util.EstadoSesion;
import com.grupob.inventario.ui.fx.util.NavegacionFx;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Controller para main.fxml — shell principal con menú lateral.
 * Reglas (sección 8.2 Notion etapa 2):
 * - Ocultar opciones admin para GESTOR (visible=false + managed=false).
 * - Logout en Task de fondo; limpia EstadoSesion antes de navegar.
 */
public class MainController {

    @FXML private Label      bienvenidoLabel;
    @FXML private Button     usuariosBtn;
    @FXML private Button     auditoriaBtn;
    @FXML private StackPane  contenido;

    @FXML
    public void initialize() {
        Usuario usuario = EstadoSesion.getUsuarioActual();
        if (usuario != null) {
            bienvenidoLabel.setText(
                    "Bienvenido, " + usuario.getUsername() +
                    " (" + usuario.getRol() + ")");
        }

        // Ocultar acciones de admin para GESTOR_INVENTARIO (sección 8.2 Notion)
        if (EstadoSesion.getRol() == Rol.GESTOR_INVENTARIO) {
            ocultarBoton(usuariosBtn);
            ocultarBoton(auditoriaBtn);
        }

        // Carga la pantalla de productos por defecto
        cargarEnContenido("productos");
    }

    // ── Botones de menú lateral ────────────────────────────────────────

    @FXML
    public void verProductos(ActionEvent e) { cargarEnContenido("productos"); }

    @FXML
    public void verStock(ActionEvent e) { cargarEnContenido("stock"); }

    @FXML
    public void verUsuarios(ActionEvent e) { cargarEnContenido("usuarios"); }

    @FXML
    public void verAuditoria(ActionEvent e) { cargarEnContenido("auditoria"); }

    // ── Cerrar sesión ──────────────────────────────────────────────────

    @FXML
    public void cerrarSesion(ActionEvent event) {
        String token = EstadoSesion.getToken();
        if (token == null) {
            NavegacionFx.mostrar("login");
            return;
        }

        AutenticacionService authSvc = NavegacionFx.getContexto().getAutenticacionService();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                authSvc.logout(token);
                EstadoSesion.cerrar();
                return null;
            }
        };

        task.setOnSucceeded(e -> NavegacionFx.mostrar("login"));
        task.setOnFailed(e -> {
            EstadoSesion.cerrar();       // limpia sesión aunque falle el logout
            NavegacionFx.mostrar("login");
        });

        new Thread(task, "logout-task").start();
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private void cargarEnContenido(String nombre) {
        try {
            Parent vista = NavegacionFx.cargar(nombre);
            contenido.getChildren().setAll(vista);
        } catch (Exception e) {
            Alertas.inesperado(e);
        }
    }

    private static void ocultarBoton(Button btn) {
        btn.setVisible(false);
        btn.setManaged(false);
    }
}
