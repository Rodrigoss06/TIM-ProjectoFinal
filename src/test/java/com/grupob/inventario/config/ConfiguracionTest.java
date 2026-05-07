package com.grupob.inventario.config;

import com.grupob.inventario.domain.exception.ConfiguracionException;
import com.grupob.inventario.util.MensajesError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Configuracion — carga de properties, overrides de entorno y validaciones (RF-INV-010)")
class ConfiguracionTest {

    /** Properties completas con todos los valores explícitos. */
    private static Properties propsTodas() {
        Properties p = new Properties();
        p.setProperty("db.url",                    "jdbc:postgresql://localhost:5432/test");
        p.setProperty("db.usuario",                "testuser");
        p.setProperty("db.password",               "testpass");
        p.setProperty("db.pool.size",              "5");
        p.setProperty("db.pool.timeout.ms",        "15000");
        p.setProperty("sesion.expiracion.minutos", "60");
        p.setProperty("lockout.duracion.minutos",  "10");
        p.setProperty("lockout.intentos.max",      "5");
        p.setProperty("auditoria.pagina.tamano",   "25");
        return p;
    }

    /** Properties mínimas (solo obligatorias). */
    private static Properties propsMinimas() {
        Properties p = new Properties();
        p.setProperty("db.url",      "jdbc:postgresql://localhost:5432/test");
        p.setProperty("db.usuario",  "user");
        p.setProperty("db.password", "pass");
        return p;
    }

    // ── Caso positivo completo ─────────────────────────────────────────

    @Test
    @DisplayName("todas las variables seteadas → getters retornan valores correctos")
    void todasLasVarsSeteadas_gettersRetornanValores() {
        Configuracion cfg = new Configuracion(propsTodas(), Map.of());

        assertThat(cfg.getDbUrl()).isEqualTo("jdbc:postgresql://localhost:5432/test");
        assertThat(cfg.getDbUsuario()).isEqualTo("testuser");
        assertThat(cfg.getDbPassword()).isEqualTo("testpass");
        assertThat(cfg.getDbPoolSize()).isEqualTo(5);
        assertThat(cfg.getDbPoolTimeoutMs()).isEqualTo(15000L);
        assertThat(cfg.getSesionExpiracionMinutos()).isEqualTo(60);
        assertThat(cfg.getLockoutDuracionMinutos()).isEqualTo(10);
        assertThat(cfg.getLockoutIntentosMax()).isEqualTo(5);
        assertThat(cfg.getAuditoriaPaginaTamano()).isEqualTo(25);
    }

    // ── Variables opcionales con defaults ─────────────────────────────

    @Test
    @DisplayName("variables opcionales ausentes → se usan los defaults documentados")
    void variablesOpcionalesAusentes_usanDefaults() {
        Configuracion cfg = new Configuracion(propsMinimas(), Map.of());

        assertThat(cfg.getDbPoolSize()).isEqualTo(10);
        assertThat(cfg.getDbPoolTimeoutMs()).isEqualTo(30000L);
        assertThat(cfg.getSesionExpiracionMinutos()).isEqualTo(30);
        assertThat(cfg.getLockoutDuracionMinutos()).isEqualTo(15);
        assertThat(cfg.getLockoutIntentosMax()).isEqualTo(3);
        assertThat(cfg.getAuditoriaPaginaTamano()).isEqualTo(50);
    }

    // ── Variables obligatorias faltantes ──────────────────────────────

    @Test
    @DisplayName("falta db.url → ConfiguracionException con CONFIG_FALTANTE_FMT")
    void faltaDbUrl_lanzaConfiguracionException() {
        Properties p = new Properties();
        p.setProperty("db.usuario", "user");
        p.setProperty("db.password", "pass");

        Configuracion cfg = new Configuracion(p, Map.of());
        assertThatThrownBy(cfg::getDbUrl)
                .isInstanceOf(ConfiguracionException.class)
                .hasMessage(String.format(MensajesError.CONFIG_FALTANTE_FMT, "db.url"));
    }

    @Test
    @DisplayName("falta db.usuario → ConfiguracionException con CONFIG_FALTANTE_FMT")
    void faltaDbUsuario_lanzaConfiguracionException() {
        Properties p = new Properties();
        p.setProperty("db.url", "jdbc:postgresql://localhost:5432/test");
        p.setProperty("db.password", "pass");

        assertThatThrownBy(() -> new Configuracion(p, Map.of()).getDbUsuario())
                .isInstanceOf(ConfiguracionException.class)
                .hasMessage(String.format(MensajesError.CONFIG_FALTANTE_FMT, "db.usuario"));
    }

    @Test
    @DisplayName("falta db.password → ConfiguracionException con CONFIG_FALTANTE_FMT")
    void faltaDbPassword_lanzaConfiguracionException() {
        Properties p = new Properties();
        p.setProperty("db.url", "jdbc:postgresql://localhost:5432/test");
        p.setProperty("db.usuario", "user");

        assertThatThrownBy(() -> new Configuracion(p, Map.of()).getDbPassword())
                .isInstanceOf(ConfiguracionException.class)
                .hasMessage(String.format(MensajesError.CONFIG_FALTANTE_FMT, "db.password"));
    }

    // ── Valores numéricos inválidos ────────────────────────────────────

    @Test
    @DisplayName("lockout.intentos.max=0 → ConfiguracionException con CONFIG_INVALIDA_FMT")
    void lockoutIntentosMaxCero_lanzaConfiguracionException() {
        Properties p = propsTodas();
        p.setProperty("lockout.intentos.max", "0");

        assertThatThrownBy(() -> new Configuracion(p, Map.of()).getLockoutIntentosMax())
                .isInstanceOf(ConfiguracionException.class)
                .hasMessage(String.format(MensajesError.CONFIG_INVALIDA_FMT, "lockout.intentos.max"));
    }

    @Test
    @DisplayName("lockout.intentos.max negativo → ConfiguracionException con CONFIG_INVALIDA_FMT")
    void lockoutIntentosMaxNegativo_lanzaConfiguracionException() {
        Properties p = propsTodas();
        p.setProperty("lockout.intentos.max", "-1");

        assertThatThrownBy(() -> new Configuracion(p, Map.of()).getLockoutIntentosMax())
                .isInstanceOf(ConfiguracionException.class)
                .hasMessage(String.format(MensajesError.CONFIG_INVALIDA_FMT, "lockout.intentos.max"));
    }

    @Test
    @DisplayName("lockout.intentos.max no numérico → ConfiguracionException con CONFIG_INVALIDA_FMT")
    void lockoutIntentosMaxTexto_lanzaConfiguracionException() {
        Properties p = propsTodas();
        p.setProperty("lockout.intentos.max", "tres");

        assertThatThrownBy(() -> new Configuracion(p, Map.of()).getLockoutIntentosMax())
                .isInstanceOf(ConfiguracionException.class)
                .hasMessage(String.format(MensajesError.CONFIG_INVALIDA_FMT, "lockout.intentos.max"));
    }

    @Test
    @DisplayName("db.pool.size=0 → ConfiguracionException (0 no es entero positivo)")
    void poolSizeCero_lanzaConfiguracionException() {
        Properties p = propsTodas();
        p.setProperty("db.pool.size", "0");

        assertThatThrownBy(() -> new Configuracion(p, Map.of()).getDbPoolSize())
                .isInstanceOf(ConfiguracionException.class)
                .hasMessage(String.format(MensajesError.CONFIG_INVALIDA_FMT, "db.pool.size"));
    }

    // ── Override por variable de entorno ──────────────────────────────

    @Test
    @DisplayName("DB_URL en entorno tiene prioridad sobre db.url en properties")
    void envDbUrl_sobreescribeProperties() {
        Properties p = propsTodas(); // tiene db.url=jdbc:postgresql://localhost:5432/test
        Map<String, String> env = Map.of("DB_URL", "jdbc:postgresql://otrohost:5432/otro");

        assertThat(new Configuracion(p, env).getDbUrl())
                .isEqualTo("jdbc:postgresql://otrohost:5432/otro");
    }

    @Test
    @DisplayName("DB_USUARIO en entorno sobrescribe db.usuario en properties")
    void envDbUsuario_sobreescribeProperties() {
        Properties p = propsTodas();
        Map<String, String> env = Map.of("DB_USUARIO", "usuario_env");

        assertThat(new Configuracion(p, env).getDbUsuario())
                .isEqualTo("usuario_env");
    }

    @Test
    @DisplayName("DB_PASSWORD en entorno sobrescribe db.password en properties")
    void envDbPassword_sobreescribeProperties() {
        Properties p = propsTodas();
        Map<String, String> env = Map.of("DB_PASSWORD", "secret_env");

        assertThat(new Configuracion(p, env).getDbPassword())
                .isEqualTo("secret_env");
    }

    @Test
    @DisplayName("LOCKOUT_INTENTOS_MAX en entorno sobrescribe lockout.intentos.max y pasa validación")
    void envLockoutIntentosMax_sobreescribeProperties() {
        Properties p = propsTodas();
        p.setProperty("lockout.intentos.max", "0"); // inválido en props
        Map<String, String> env = Map.of("LOCKOUT_INTENTOS_MAX", "5"); // válido en env

        assertThat(new Configuracion(p, env).getLockoutIntentosMax()).isEqualTo(5);
    }

    @Test
    @DisplayName("variable env provee db.url faltante en properties → no lanza excepción")
    void envDbUrl_cuentaComoObligatoria() {
        Properties p = new Properties();
        p.setProperty("db.usuario", "user");
        p.setProperty("db.password", "pass");
        Map<String, String> env = Map.of("DB_URL", "jdbc:postgresql://localhost:5432/test");

        assertThat(new Configuracion(p, env).getDbUrl())
                .isEqualTo("jdbc:postgresql://localhost:5432/test");
    }
}
