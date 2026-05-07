package com.grupob.inventario.ui.fx.util;

import com.grupob.inventario.config.ContextoAplicacion;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public final class NavegacionFx {

    private static Stage             stagePrincipal;
    private static ContextoAplicacion contexto;

    private NavegacionFx() {}

    /** Inicializar una sola vez desde AppFx.start(). */
    public static void init(Stage stage, ContextoAplicacion ctx) {
        stagePrincipal = stage;
        contexto       = ctx;
    }

    public static ContextoAplicacion getContexto() { return contexto; }

    /**
     * Carga un FXML de /fx/<nombre>.fxml y lo retorna.
     * Los controllers acceden al contexto via NavegacionFx.getContexto().
     */
    public static Parent cargar(String nombre, ContextoAplicacion ctx) {
        try {
            URL url = NavegacionFx.class.getResource("/fx/" + nombre + ".fxml");
            if (url == null) throw new IllegalArgumentException("FXML no encontrado: " + nombre);
            return new FXMLLoader(url).load();
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar FXML: " + nombre, e);
        }
    }

    public static Parent cargar(String nombre) {
        return cargar(nombre, contexto);
    }

    /** Reemplaza el root de la escena existente (o crea nueva escena si aún no hay). */
    public static void mostrar(String nombre) {
        try {
            Parent root  = cargar(nombre);
            Scene  scene = stagePrincipal.getScene();
            if (scene == null) {
                scene = new Scene(root, 900, 600);
                URL css = NavegacionFx.class.getResource("/fx/styles.css");
                if (css != null) scene.getStylesheets().add(css.toExternalForm());
                stagePrincipal.setScene(scene);
            } else {
                scene.setRoot(root);
            }
        } catch (Exception e) {
            System.err.println("[NavegacionFx] Error al mostrar: " + nombre);
            e.printStackTrace(System.err);
            Alertas.inesperado(e);
        }
    }
}
