package com.grupob.inventario.config;

import com.grupob.inventario.domain.exception.ConfiguracionException;
import com.grupob.inventario.util.MensajesError;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Lee configuración desde application.properties (classpath) y aplica overrides
 * desde variables de entorno. La conversión de clave es:
 *   "db.url" → "DB_URL" (mayúsculas, puntos reemplazados por guiones bajos).
 *
 * Uso en producción: new Configuracion()
 * Uso en tests:      new Configuracion(properties, envMap)
 */
public class Configuracion {

    private final Properties props;
    private final Map<String, String> env;

    /** Constructor de producción: classpath + System.getenv(). */
    public Configuracion() {
        this(cargarDesdeClasspath(), System.getenv());
    }

    /** Constructor para tests: acepta properties y mapa de entorno arbitrarios. */
    public Configuracion(Properties props, Map<String, String> env) {
        this.props = props;
        this.env   = env;
    }

    // ── Obligatorias ─────────────────────────────────────────────────

    public String getDbUrl()      { return obtenerObligatorio("db.url"); }
    public String getDbUsuario()  { return obtenerObligatorio("db.usuario"); }
    public String getDbPassword() { return obtenerObligatorio("db.password"); }

    // ── Opcionales con default ────────────────────────────────────────

    public int  getDbPoolSize()             { return obtenerEnteroPositivo("db.pool.size", 10); }
    public long getDbPoolTimeoutMs()        { return obtenerEnteroPositivo("db.pool.timeout.ms", 30000); }
    public int  getSesionExpiracionMinutos(){ return obtenerEnteroPositivo("sesion.expiracion.minutos", 30); }
    public int  getLockoutDuracionMinutos() { return obtenerEnteroPositivo("lockout.duracion.minutos", 15); }
    public int  getLockoutIntentosMax()     { return obtenerEnteroPositivo("lockout.intentos.max", 3); }
    public int  getAuditoriaPaginaTamano()  { return obtenerEnteroPositivo("auditoria.pagina.tamano", 50); }

    // ── Lógica interna ────────────────────────────────────────────────

    private String obtenerObligatorio(String clave) {
        String valor = obtenerOpcional(clave);
        if (valor == null || valor.isBlank()) {
            throw new ConfiguracionException(
                    String.format(MensajesError.CONFIG_FALTANTE_FMT, clave));
        }
        return valor;
    }

    private int obtenerEnteroPositivo(String clave, int valorPorDefecto) {
        String raw = obtenerOpcional(clave);
        if (raw == null || raw.isBlank()) return valorPorDefecto;
        try {
            int n = Integer.parseInt(raw.trim());
            if (n <= 0) {
                throw new ConfiguracionException(
                        String.format(MensajesError.CONFIG_INVALIDA_FMT, clave));
            }
            return n;
        } catch (NumberFormatException e) {
            throw new ConfiguracionException(
                    String.format(MensajesError.CONFIG_INVALIDA_FMT, clave));
        }
    }

    /**
     * Busca el valor con prioridad: variable de entorno > application.properties.
     * Conversión de clave: "db.url" → "DB_URL".
     */
    /**
     * Prioridad: variable de entorno > system property (para tests IT) > application.properties.
     */
    private String obtenerOpcional(String clave) {
        String envClave = clave.toUpperCase().replace('.', '_');
        String envValor = env.get(envClave);
        if (envValor != null) return envValor;
        // System property como fallback — útil en tests IT que no pueden setear env vars reales
        String sysProp = System.getProperty(envClave);
        if (sysProp != null) return sysProp;
        return props.getProperty(clave);
    }

    private static Properties cargarDesdeClasspath() {
        Properties p = new Properties();
        try (InputStream in = Configuracion.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) p.load(in);
        } catch (IOException e) {
            // Si no existe el archivo, las vars obligatorias fallarán en sus getters
        }
        return p;
    }
}
