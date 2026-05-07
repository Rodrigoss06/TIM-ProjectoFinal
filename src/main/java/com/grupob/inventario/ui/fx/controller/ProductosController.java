package com.grupob.inventario.ui.fx.controller;

import com.grupob.inventario.domain.enums.EstadoStock;
import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.service.FiltroListado;
import com.grupob.inventario.service.ProductoService;
import com.grupob.inventario.service.TipoBusqueda;
import com.grupob.inventario.ui.fx.util.Alertas;
import com.grupob.inventario.ui.fx.util.EstadoSesion;
import com.grupob.inventario.ui.fx.util.NavegacionFx;
import com.grupob.inventario.util.MensajesError;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.List;

public class ProductosController {

    @FXML private TextField      filtroCategoria;
    @FXML private TextField      filtroPrecioMin;
    @FXML private TextField      filtroPrecioMax;
    @FXML private ComboBox<String> filtroEstado;
    @FXML private TableView<Producto>   tablaProductos;
    @FXML private TableColumn<Producto, String> colCodigo;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colPrecio;
    @FXML private TableColumn<Producto, String> colStock;
    @FXML private TableColumn<Producto, String> colEstado;
    @FXML private Button  btnEliminar;
    @FXML private Label   total;

    private FiltroListado activeFiltro = new FiltroListado(null, null, null, null);

    @FXML
    public void initialize() {
        // Columnas con lambda (no PropertyValueFactory para evitar reflection issues)
        colCodigo.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue().getCodigo()));
        colNombre.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue().getNombre()));
        colCategoria.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue().getCategoria()));
        colPrecio.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(
                c.getValue().getPrecioUnitario().toPlainString()));
        colStock.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(
                String.valueOf(c.getValue().getStock())));

        // Columna Estado con celda custom (sección 12.5 Notion: marcadores visuales)
        colEstado.setCellValueFactory(c -> {
            int s = c.getValue().getStock();
            return new javafx.beans.property.SimpleStringProperty(
                s == 0 ? "[SIN STOCK]" : s < 5 ? "[BAJO]" : "");
        });
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "[SIN STOCK]" ->
                        setStyle("-fx-background-color: #fecaca; -fx-text-fill: #991b1b; -fx-font-weight: bold;");
                    case "[BAJO]" ->
                        setStyle("-fx-background-color: #fef9c3; -fx-text-fill: #78350f; -fx-font-weight: bold;");
                    default -> setStyle("");
                }
            }
        });

        // ComboBox de estado
        filtroEstado.setItems(FXCollections.observableArrayList(
                "Todos", "Sin stock", "Bajo", "Normal"));
        filtroEstado.setValue("Todos");

        // Gestores no ven botón Eliminar (defensa visual + servicio bloquea también)
        if (EstadoSesion.getRol() == Rol.GESTOR_INVENTARIO) {
            btnEliminar.setVisible(false);
            btnEliminar.setManaged(false);
        }

        cargar(null);
    }

    // ── Cargar / Filtrar ───────────────────────────────────────────────

    @FXML
    public void cargar(ActionEvent event) {
        ProductoService svc = NavegacionFx.getContexto().getProductoService();
        Rol rol = EstadoSesion.getRol();
        FiltroListado filtro = activeFiltro;

        Task<List<Producto>> task = new Task<>() {
            @Override protected List<Producto> call() {
                return svc.listar(filtro, rol);
            }
        };
        task.setOnSucceeded(e -> {
            List<Producto> lista = task.getValue();
            tablaProductos.setItems(FXCollections.observableArrayList(lista));
            int n = lista.size();
            total.setText("Total: " + n + " producto" + (n == 1 ? "" : "s"));
        });
        task.setOnFailed(e -> Alertas.inesperado(task.getException()));
        new Thread(task, "listar-productos").start();
    }

    @FXML
    public void filtrar(ActionEvent event) {
        String cat  = filtroCategoria.getText().trim();
        BigDecimal pMin = parseBigDecimal(filtroPrecioMin.getText());
        BigDecimal pMax = parseBigDecimal(filtroPrecioMax.getText());
        EstadoStock estado = switch (filtroEstado.getValue()) {
            case "Sin stock" -> EstadoStock.SIN_STOCK;
            case "Bajo"      -> EstadoStock.BAJO;
            case "Normal"    -> EstadoStock.NORMAL;
            default          -> null;
        };
        try {
            activeFiltro = new FiltroListado(cat.isEmpty() ? null : cat, pMin, pMax, estado);
        } catch (InventarioException e) {
            Alertas.error(e.getMessage());
            return;
        }
        cargar(null);
    }

    @FXML
    public void limpiar(ActionEvent event) {
        filtroCategoria.clear();
        filtroPrecioMin.clear();
        filtroPrecioMax.clear();
        filtroEstado.setValue("Todos");
        activeFiltro = new FiltroListado(null, null, null, null);
        cargar(null);
    }

    // ── CRUD ──────────────────────────────────────────────────────────

    @FXML
    public void nuevo(ActionEvent event) {
        abrirFormulario(null);
    }

    @FXML
    public void editar(ActionEvent event) {
        Producto sel = tablaProductos.getSelectionModel().getSelectedItem();
        if (sel == null) { Alertas.error("Seleccione un producto para editar."); return; }
        abrirFormulario(sel);
    }

    @FXML
    public void stock(ActionEvent event) {
        Producto sel = tablaProductos.getSelectionModel().getSelectedItem();
        if (sel == null) { Alertas.error("Seleccione un producto para actualizar stock."); return; }
        abrirModalStock(sel);
    }

    @FXML
    public void eliminar(ActionEvent event) {
        Producto sel = tablaProductos.getSelectionModel().getSelectedItem();
        if (sel == null) { Alertas.error("Seleccione un producto para eliminar."); return; }

        // Defensa doble: el botón ya está oculto para Gestor, pero verificamos por si acaso
        if (EstadoSesion.getRol() == Rol.GESTOR_INVENTARIO) {
            Alertas.error(MensajesError.SIN_PERMISOS);
            return;
        }

        ProductoService svc = NavegacionFx.getContexto().getProductoService();

        // Verificar movimientos recientes (RF-INV-004 Notion)
        try {
            if (svc.tieneMovimientosRecientes(sel.getCodigo())) {
                Alertas.info(MensajesError.MOVIMIENTOS_RECIENTES_ADVERTENCIA);
            }
        } catch (Exception e) {
            Alertas.inesperado(e);
            return;
        }

        String msg = "¿Eliminar [" + sel.getCodigo() + "] " + sel.getNombre() + "?\n" +
                     "La eliminación es lógica — el historial se conserva.";
        if (!Alertas.confirmar(msg)) return;

        btnEliminar.setDisable(true);
        Rol rol = EstadoSesion.getRol();

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                svc.eliminar(sel.getCodigo(), rol);
                return null;
            }
        };
        task.setOnSucceeded(e -> { btnEliminar.setDisable(false); cargar(null); });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof InventarioException) Alertas.error(ex.getMessage());
            else Alertas.inesperado(ex);
            btnEliminar.setDisable(false);
        });
        new Thread(task, "eliminar-producto").start();
    }

    // ── Helpers de modal ──────────────────────────────────────────────

    private void abrirFormulario(Producto productoEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fx/producto-form.fxml"));
            Parent root = loader.load();
            ProductoFormController ctrl = loader.getController();
            if (productoEditar == null) ctrl.inicializarParaCrear();
            else ctrl.inicializarParaEditar(productoEditar);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle(productoEditar == null ? "Nuevo Producto" : "Editar Producto");
            modal.setScene(new Scene(root));
            modal.setResizable(false);
            modal.showAndWait();

            if (ctrl.isSaved()) cargar(null);
        } catch (Exception e) {
            Alertas.inesperado(e);
        }
    }

    private void abrirModalStock(Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fx/stock.fxml"));
            Parent root = loader.load();
            StockController ctrl = loader.getController();
            ctrl.inicializar(producto);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Actualizar Stock — " + producto.getCodigo());
            modal.setScene(new Scene(root));
            modal.setResizable(false);
            modal.showAndWait();

            if (ctrl.isConfirmado()) cargar(null);
        } catch (Exception e) {
            Alertas.inesperado(e);
        }
    }

    // ── Helpers de parseo ─────────────────────────────────────────────

    private static BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
