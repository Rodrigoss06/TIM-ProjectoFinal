package com.grupob.inventario.ui.fx;

import com.grupob.inventario.config.Configuracion;
import com.grupob.inventario.config.ContextoAplicacion;
import com.grupob.inventario.ui.fx.util.EstadoSesion;
import com.grupob.inventario.util.MensajesError;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Smoke test de la pantalla de login con TestFX + repos en memoria.
 * NO usa Testcontainers (demasiado lento con TestFX).
 *
 * Se omite automáticamente en entornos headless sin Monocle.
 * Con -Dtestfx.headless=true -Dglass.platform=Monocle funciona en CI headless.
 */
@DisplayName("LoginSmokeTest — UI login con repos en memoria")
@org.junit.jupiter.api.extension.ExtendWith(ApplicationExtension.class)
class LoginSmokeTest {

    private static ContextoAplicacion contexto;

    @BeforeAll
    static void configurar() {
        // Saltar si headless Y sin Monocle configurado
        boolean headlessReal = java.awt.GraphicsEnvironment.isHeadless();
        boolean monocleActivo = "true".equalsIgnoreCase(System.getProperty("testfx.headless"));
        assumeFalse(headlessReal && !monocleActivo,
                "LoginSmokeTest omitido: entorno headless sin Monocle");

        Properties props = new Properties();
        props.setProperty("db.url",      "jdbc:unused://localhost/test");
        props.setProperty("db.usuario",  "test");
        props.setProperty("db.password", "test");
        contexto = new ContextoAplicacion(new Configuracion(props, Map.of()));
    }

    @Start
    void start(Stage stage) throws Exception {
        EstadoSesion.cerrar();
        AppFx.setContexto(contexto);
        new AppFx().start(stage);
    }

    // ── Login exitoso ──────────────────────────────────────────────────

    @Test
    @DisplayName("Login exitoso → navega a pantalla main con bienvenido")
    void loginExitoso_muestraMain(FxRobot robot) {
        robot.clickOn("#usuario").write("admin");
        robot.clickOn("#contrasena").write("Admin123!");
        robot.clickOn("#entrar");

        // Esperar a que el Task complete y se cargue main.fxml (con sleep robusto)
        robot.sleep(3000);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(robot.lookup("#bienvenidoLabel").tryQuery()).isPresent();
        Label label = robot.lookup("#bienvenidoLabel").queryAs(Label.class);
        assertThat(label.getText()).contains("admin");
    }

    // ── Login fallido ──────────────────────────────────────────────────

    @Test
    @DisplayName("Login fallido → label error con MensajesError.CREDENCIALES_INVALIDAS")
    void loginFallido_muestraErrorExacto(FxRobot robot) {
        robot.clickOn("#usuario").write("admin");
        robot.clickOn("#contrasena").write("contraseñamala");
        robot.clickOn("#entrar");

        robot.sleep(3000);
        WaitForAsyncUtils.waitForFxEvents();

        Label errorLabel = robot.lookup("#error").queryAs(Label.class);
        assertThat(errorLabel.isVisible()).isTrue();
        assertThat(errorLabel.getText()).isEqualTo(MensajesError.CREDENCIALES_INVALIDAS);
    }

    // ── Campos vacíos ──────────────────────────────────────────────────

    @Test
    @DisplayName("Campos vacíos → error inline sin llamar al servicio")
    void camposVacios_errorInline(FxRobot robot) {
        robot.clickOn("#entrar");
        WaitForAsyncUtils.waitForFxEvents();

        Label errorLabel = robot.lookup("#error").queryAs(Label.class);
        assertThat(errorLabel.isVisible()).isTrue();
        assertThat(errorLabel.getText()).isNotBlank();
    }
}
