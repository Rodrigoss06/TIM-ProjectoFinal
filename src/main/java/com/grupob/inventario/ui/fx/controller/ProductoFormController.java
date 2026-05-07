package com.grupob.inventario.ui.fx.controller;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.service.ProductoService;
import com.grupob.inventario.service.TipoBusqueda;
import com.grupob.inventario.ui.fx.util.Alertas;
import com.grupob.inventario.ui.fx.util.EstadoSesion;
import com.grupob.inventario.ui.fx.util.NavegacionFx;
import com.grupob.inventario.util.MensajesError;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.function.UnaryOperator;

/**
 * Controller para producto-form.fxml (crear / editar).
 * Sección 8.2 Notion: validación de formato con TextFormatter antes de llamar al servicio.
 * Botón deshabilitado durante la operación (evita doble click, sección 12.5).
 */
public class ProductoFormController {

    @FXML private Label     tituloLabel;
    @FXML private TextField codigo;
    @FXML private TextField nombre;
    @FXML private TextField descripcion;
    @FXML private TextField categoria;
    @FXML private TextField precio;
    @FXML private TextField stockInicial;
    @FXML private Label     errorLabel;
    @FXML private Button    guardarBtn;

    private boolean guardado    = false;
    private boolean modoEdicion = false;
    private Producto productoOriginal;

    public boolean isSaved() { return guardado; }

    @FXML
    public void initialize() {
        // TextFormatter precio: solo dígitos + punto + máximo 2 decimales
        UnaryOperator<TextFormatter.Change> precioFiltro = change ->
            change.getControlNewText().matches("\\d*(\\.\\d{0,2})?") ? change : null;
        precio.setTextFormatter(new TextFormatter<>(precioFiltro));

        // TextFormatter stock: solo enteros
        UnaryOperator<TextFormatter.Change> intFiltro = change ->
            change.getControlNewText().matches("\\d*") ? change : null;
        stockInicial.setTextFormatter(new TextFormatter<>(intFiltro));

        ocultarError();
    }

    public void inicializarParaCrear() {
        modoEdicion = false;
        tituloLabel.setText("Nuevo Producto");
        codigo.setDisable(false);
        stockInicial.setDisable(false);
    }

    public void inicializarParaEditar(Producto p) {
        modoEdicion       = true;
        productoOriginal  = p;
        tituloLabel.setText("Editar Producto");
        codigo.setText(p.getCodigo());
        codigo.setDisable(true);
        nombre.setText(p.getNombre());
        descripcion.setText(p.getDescripcion() != null ? p.getDescripcion() : "");
        categoria.setText(p.getCategoria());
        precio.setText(p.getPrecioUnitario().toPlainString());
        stockInicial.setText(String.valueOf(p.getStock()));
        stockInicial.setDisable(true);
    }

    @FXML
    public void guardar(ActionEvent event) {
        // Validaciones de formato en UI antes de llamar al servicio
        if (codigo.getText().isBlank() || nombre.getText().isBlank() ||
                categoria.getText().isBlank() || precio.getText().isBlank()) {
            mostrarError(MensajesError.CAMPOS_OBLIGATORIOS);
            return;
        }

        BigDecimal precioVal;
        try {
            precioVal = new BigDecimal(precio.getText().trim());
        } catch (NumberFormatException e) {
            mostrarError(MensajesError.INPUT_NUMERICO_INVALIDO);
            return;
        }

        int stockVal = modoEdicion ? productoOriginal.getStock() : 0;
        if (!modoEdicion) {
            String stockTxt = stockInicial.getText().trim();
            try {
                stockVal = stockTxt.isEmpty() ? 0 : Integer.parseInt(stockTxt);
            } catch (NumberFormatException e) {
                mostrarError(MensajesError.INPUT_NUMERICO_INVALIDO);
                return;
            }
        }

        ocultarError();
        guardarBtn.setDisable(true);

        ProductoService svc = NavegacionFx.getContexto().getProductoService();
        Rol rol = EstadoSesion.getRol();
        final int stockFinal = stockVal;
        final BigDecimal precioFinal = precioVal;
        final boolean esEdicion = modoEdicion;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                Producto p = new Producto(
                        codigo.getText().trim(),
                        nombre.getText().trim(),
                        descripcion.getText().trim(),
                        categoria.getText().trim(),
                        precioFinal,
                        stockFinal);
                if (esEdicion) {
                    svc.actualizar(p, rol);
                } else {
                    svc.registrar(p, rol);
                }
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

        new Thread(task, "guardar-producto").start();
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
        ((Stage) guardarBtn.getScene().getWindow()).close();
    }
}
