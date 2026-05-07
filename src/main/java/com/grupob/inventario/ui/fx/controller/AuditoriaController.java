package com.grupob.inventario.ui.fx.controller;

import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.model.EventoAuditoria;
import com.grupob.inventario.repository.FiltroAuditoria;
import com.grupob.inventario.service.AuditoriaService;
import com.grupob.inventario.ui.fx.util.Alertas;
import com.grupob.inventario.ui.fx.util.EstadoSesion;
import com.grupob.inventario.ui.fx.util.NavegacionFx;
import com.grupob.inventario.util.MensajesError;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RF-INV-009 — Consulta de auditoría.
 * Solo accesible para ADMINISTRADOR; el servicio también valida.
 * Filtros: rango de fechas, username, tipo de evento. Paginación.
 */
public class AuditoriaController {

    @FXML private DatePicker    filtroDesde;
    @FXML private DatePicker    filtroHasta;
    @FXML private TextField     filtroUsername;
    @FXML private ComboBox<String> filtroTipo;

    @FXML private TableView<EventoAuditoria>             tablaAuditoria;
    @FXML private TableColumn<EventoAuditoria, String>   colFecha;
    @FXML private TableColumn<EventoAuditoria, String>   colUsuario;
    @FXML private TableColumn<EventoAuditoria, String>   colTipo;
    @FXML private TableColumn<EventoAuditoria, String>   colEntidad;
    @FXML private TableColumn<EventoAuditoria, String>   colDetalle;

    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Label  paginaLabel;
    @FXML private Label  mensajeSinResultados;

    private int paginaActual  = 0;
    private int tamanoPagina  = 50;

    private static final DateTimeFormatter FECHA_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                         .withZone(ZoneId.systemDefault());

    @FXML
    public void initialize() {
        tamanoPagina = NavegacionFx.getContexto().getConfiguracion().getAuditoriaPaginaTamano();

        // Columnas
        colFecha.setCellValueFactory(c ->
            new SimpleStringProperty(FECHA_FMT.format(c.getValue().getFecha())));
        colUsuario.setCellValueFactory(c ->
            new SimpleStringProperty(
                c.getValue().getUsername() != null ? c.getValue().getUsername() : "(anon)"));
        colTipo.setCellValueFactory(c ->
            new SimpleStringProperty(formatearTipo(c.getValue().getTipoEvento())));
        colEntidad.setCellValueFactory(c ->
            new SimpleStringProperty(
                c.getValue().getEntidadAfectada() != null ? c.getValue().getEntidadAfectada() : ""));
        colDetalle.setCellValueFactory(c ->
            new SimpleStringProperty(
                c.getValue().getDetalle() != null ? c.getValue().getDetalle() : ""));

        // ComboBox tipos: "Todos" + todos los valores de TipoEvento
        List<String> opciones = new ArrayList<>();
        opciones.add("Todos");
        Arrays.stream(TipoEvento.values()).map(AuditoriaController::formatearTipo).forEach(opciones::add);
        filtroTipo.setItems(FXCollections.observableArrayList(opciones));
        filtroTipo.setValue("Todos");

        // Mensaje sin resultados
        mensajeSinResultados.setText(MensajesError.SIN_REGISTROS_AUDITORIA);

        cargar();
    }

    // ── Cargar / Filtrar / Paginar ─────────────────────────────────────

    private void cargar() {
        FiltroAuditoria filtro   = construirFiltro();
        int             pagina   = paginaActual;
        AuditoriaService svc     = NavegacionFx.getContexto().getAuditoriaService();
        var             rol      = EstadoSesion.getRol();

        Task<List<EventoAuditoria>> task = new Task<>() {
            @Override protected List<EventoAuditoria> call() {
                return svc.consultar(filtro, pagina, rol);
            }
        };
        task.setOnSucceeded(e -> {
            List<EventoAuditoria> lista = task.getValue();
            tablaAuditoria.setItems(FXCollections.observableArrayList(lista));
            boolean vacio = lista.isEmpty();
            mensajeSinResultados.setVisible(vacio);
            mensajeSinResultados.setManaged(vacio);
            actualizarBotonesPagina(lista.size());
            paginaLabel.setText("Página " + (paginaActual + 1));
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof InventarioException) Alertas.error(ex.getMessage());
            else Alertas.inesperado(ex);
        });
        new Thread(task, "cargar-auditoria").start();
    }

    @FXML
    public void filtrar(ActionEvent event) { paginaActual = 0; cargar(); }

    @FXML
    public void limpiar(ActionEvent event) {
        filtroDesde.setValue(null);
        filtroHasta.setValue(null);
        filtroUsername.clear();
        filtroTipo.setValue("Todos");
        paginaActual = 0;
        cargar();
    }

    @FXML
    public void anterior(ActionEvent event) {
        if (paginaActual > 0) { paginaActual--; cargar(); }
    }

    @FXML
    public void siguiente(ActionEvent event) { paginaActual++; cargar(); }

    // ── Helpers ───────────────────────────────────────────────────────

    private FiltroAuditoria construirFiltro() {
        Instant desde = filtroDesde.getValue() != null
            ? filtroDesde.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        // Hasta: incluir todo el día seleccionado → inicio del día siguiente
        Instant hasta = filtroHasta.getValue() != null
            ? filtroHasta.getValue().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        String  user  = filtroUsername.getText().trim().isEmpty() ? null : filtroUsername.getText().trim();

        String  tipoStr = filtroTipo.getValue();
        TipoEvento tipo = "Todos".equals(tipoStr) ? null :
            Arrays.stream(TipoEvento.values())
                  .filter(t -> formatearTipo(t).equals(tipoStr))
                  .findFirst().orElse(null);

        return new FiltroAuditoria(desde, hasta, user, tipo);
    }

    private void actualizarBotonesPagina(int resultados) {
        btnAnterior.setDisable(paginaActual == 0);
        // Si recibimos exactamente tamanoPagina resultados, podría haber más
        btnSiguiente.setDisable(resultados < tamanoPagina);
    }

    /** "LOGIN_EXITOSO" → "Login exitoso" */
    private static String formatearTipo(TipoEvento t) {
        String s = t.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
