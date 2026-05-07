package com.grupob.inventario.ui.fx.controller;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.model.Movimiento;
import com.grupob.inventario.service.InventarioService;
import com.grupob.inventario.ui.fx.util.Alertas;
import com.grupob.inventario.ui.fx.util.EstadoSesion;
import com.grupob.inventario.ui.fx.util.NavegacionFx;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MovimientosController {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @FXML private TextField   codigoFiltro;
    @FXML private ComboBox<TipoMovimiento> tipoFiltro;
    @FXML private DatePicker  desdeFiltro;
    @FXML private DatePicker  hastaFiltro;
    @FXML private Button      filtrarBtn;
    @FXML private Button      limpiarBtn;
    @FXML private Label       mensajeVacio;
    @FXML private Label       totalLabel;

    @FXML private TableView<Movimiento>   tablaMovimientos;
    @FXML private TableColumn<Movimiento, String>         colFecha;
    @FXML private TableColumn<Movimiento, String>         colCodigoProd;
    @FXML private TableColumn<Movimiento, TipoMovimiento> colTipo;
    @FXML private TableColumn<Movimiento, Number>         colCantidad;
    @FXML private TableColumn<Movimiento, Number>         colStockAnterior;
    @FXML private TableColumn<Movimiento, Number>         colStockNuevo;

    @FXML
    public void initialize() {
        configurarComboTipos();
        configurarColumnas();
        cargar();
    }

    @FXML
    public void filtrar(ActionEvent e) {
        LocalDate desde = desdeFiltro.getValue();
        LocalDate hasta = hastaFiltro.getValue();
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            Alertas.error("La fecha 'Desde' no puede ser posterior a 'Hasta'.");
            return;
        }
        cargar();
    }

    @FXML
    public void limpiar(ActionEvent e) {
        codigoFiltro.clear();
        tipoFiltro.getSelectionModel().selectFirst();
        desdeFiltro.setValue(null);
        hastaFiltro.setValue(null);
        cargar();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void configurarComboTipos() {
        tipoFiltro.setItems(FXCollections.observableArrayList(null, TipoMovimiento.ENTRADA, TipoMovimiento.SALIDA));
        tipoFiltro.setConverter(new StringConverter<>() {
            @Override public String toString(TipoMovimiento t) { return t == null ? "Todos" : t.name(); }
            @Override public TipoMovimiento fromString(String s) { return null; }
        });
        tipoFiltro.getSelectionModel().selectFirst();
    }

    private void configurarColumnas() {
        colFecha.setCellValueFactory(c ->
                new SimpleStringProperty(FORMATO_FECHA.format(c.getValue().getFecha())));
        colCodigoProd.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCodigoProducto()));
        colTipo.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getTipo()));
        colCantidad.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getCantidad()));
        colStockAnterior.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getStockAnterior()));
        colStockNuevo.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getStockNuevo()));
    }

    private void cargar() {
        String codigo = codigoFiltro.getText();
        if (codigo != null) codigo = codigo.trim();
        if (codigo != null && codigo.isEmpty()) codigo = null;

        TipoMovimiento tipo = tipoFiltro.getValue();

        Instant desde = desdeFiltro.getValue() == null
                ? null
                : desdeFiltro.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant();
        // 'hasta' es exclusivo: incluye todo el día seleccionado sumando 1 día y comparando con
        Instant hasta = hastaFiltro.getValue() == null
                ? null
                : hastaFiltro.getValue().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Rol rol = EstadoSesion.getRol();
        InventarioService svc = NavegacionFx.getContexto().getInventarioService();

        filtrarBtn.setDisable(true);
        limpiarBtn.setDisable(true);

        final String codigoF = codigo;
        Task<List<Movimiento>> task = new Task<>() {
            @Override
            protected List<Movimiento> call() {
                return svc.consultarMovimientos(codigoF, tipo, desde, hasta, rol);
            }
        };
        task.setOnSucceeded(e -> {
            List<Movimiento> r = task.getValue();
            tablaMovimientos.getItems().setAll(r);
            totalLabel.setText("Total: " + r.size() + " movimientos");
            boolean vacio = r.isEmpty();
            mensajeVacio.setVisible(vacio);
            mensajeVacio.setManaged(vacio);
            tablaMovimientos.setVisible(!vacio);
            tablaMovimientos.setManaged(!vacio);
            filtrarBtn.setDisable(false);
            limpiarBtn.setDisable(false);
        });
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            if (t instanceof InventarioException ie) {
                Alertas.error(ie.getMessage());
            } else {
                Alertas.inesperado(t);
            }
            filtrarBtn.setDisable(false);
            limpiarBtn.setDisable(false);
        });
        new Thread(task, "consultar-movimientos").start();
    }
}
