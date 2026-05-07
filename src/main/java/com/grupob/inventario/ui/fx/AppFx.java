package com.grupob.inventario.ui.fx;

import com.grupob.inventario.config.ContextoAplicacion;
import com.grupob.inventario.ui.fx.util.EstadoSesion;
import com.grupob.inventario.ui.fx.util.NavegacionFx;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Punto de entrada de la UI JavaFX.
 * El contexto se pasa vía variable estática porque JavaFX crea instancias
 * de Application internamente (no hay DI nativo en la API de JavaFX).
 */
public class AppFx extends Application {

    private static volatile ContextoAplicacion contexto;

    public static void setContexto(ContextoAplicacion ctx) {
        contexto = ctx;
    }

    @Override
    public void start(Stage stage) {
        NavegacionFx.init(stage, contexto);

        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setTitle("Inventario — Grupo B");

        NavegacionFx.mostrar("login");
        stage.show();

        // Logout limpio si el usuario cierra la ventana con sesión activa
        stage.setOnCloseRequest(e -> {
            if (EstadoSesion.estaActivo()) {
                try {
                    contexto.getAutenticacionService().logout(EstadoSesion.getToken());
                } catch (Exception ex) {
                    // Best-effort logout en cierre
                }
                EstadoSesion.cerrar();
            }
        });
    }
}
