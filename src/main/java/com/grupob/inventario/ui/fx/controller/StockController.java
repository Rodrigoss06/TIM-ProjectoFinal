package com.grupob.inventario.ui.fx.controller;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.service.InventarioService;
import com.grupob.inventario.ui.fx.util.Alertas;
import com.grupob.inventario.ui.fx.util.EstadoSesion;
import com.grupob.inventario.ui.fx.util.NavegacionFx;
import com.grupob.inventario.util.MensajesError;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;

import java.util.function.UnaryOperator;

public class StockController {

    @FXML private Label       infoProducto;
    @FXML private RadioButton radioEntrada;
    @FXML private RadioButton radioSalida;
    @FXML private TextField   cantidad;
    @FXML private Label       errorLabel;
    @FXML private Button      confirmarBtn;

    private Producto producto;
    private boolean  confirmado = false;

    public boolean isConfirmado() { return confirmado; }

    @FXML
    public void initialize() {
        UnaryOperator<TextFormatter.Change> intFiltro = change ->
            change.getControlNewText().matches("\\d*") ? change : null;
        cantidad.setTextFormatter(new TextFormatter<>(intFiltro));
        cantidad.setText("1");
        ocultarError();
    }

    public void inicializar(Producto p) {
        this.producto = p;
        infoProducto.setText(
            "[" + p.getCodigo() + "]  " + p.getNombre() +
            "\nStock actual: " + p.getStock() + " unidades");
    }

    @FXML
    public void confirmar(ActionEvent event) {
        int cantidadVal;
        try {
            cantidadVal = Integer.parseInt(cantidad.getText().trim());
            if (cantidadVal <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            mostrarError(MensajesError.CANTIDAD_NO_POSITIVA);
            return;
        }

        ocultarError();
        confirmarBtn.setDisable(true);

        TipoMovimiento tipo = radioEntrada.isSelected()
                ? TipoMovimiento.ENTRADA : TipoMovimiento.SALIDA;
        Rol rol = EstadoSesion.getRol();
        InventarioService svc = NavegacionFx.getContexto().getInventarioService();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                svc.actualizarStock(producto.getCodigo(), tipo, cantidadVal, rol);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            confirmado = true;
            Alertas.info("Stock actualizado correctamente.");
            cerrarVentana();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof InventarioException) mostrarError(ex.getMessage());
            else Alertas.inesperado(ex);
            confirmarBtn.setDisable(false);
        });

        new Thread(task, "stock-update").start();
    }

    @FXML
    public void cancelar(ActionEvent event) {
        cerrarVentana();
    }

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
        ((Stage) confirmarBtn.getScene().getWindow()).close();
    }
}
