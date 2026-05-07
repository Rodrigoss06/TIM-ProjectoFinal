package com.grupob.inventario.ui.fx.util;

import com.grupob.inventario.util.MensajesError;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public final class Alertas {

    private Alertas() {}

    public static void error(String mensaje) {
        alerta(Alert.AlertType.ERROR, mensaje).showAndWait();
    }

    public static void info(String mensaje) {
        alerta(Alert.AlertType.INFORMATION, mensaje).showAndWait();
    }

    /**
     * Diálogo de confirmación con botón por defecto en CANCELAR (sección 8.2 Notion).
     * El usuario debe elegir activamente "Confirmar" para la acción destructiva.
     */
    public static boolean confirmar(String mensaje) {
        ButtonType confirmarBtn = new ButtonType("Confirmar", ButtonBar.ButtonData.OTHER);
        ButtonType cancelarBtn  = new ButtonType("Cancelar",  ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, mensaje, cancelarBtn, confirmarBtn);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirmarBtn;
    }

    /**
     * Excepción inesperada: loguea a stderr, muestra mensaje genérico al usuario.
     * No expone el stacktrace (sección 10.4 Notion etapa 2).
     */
    public static void inesperado(Throwable t) {
        System.err.println("Error inesperado en UI: " + t);
        t.printStackTrace(System.err);
        alerta(Alert.AlertType.ERROR, MensajesError.ERROR_INESPERADO).showAndWait();
    }

    private static Alert alerta(Alert.AlertType tipo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        return alert;
    }
}
