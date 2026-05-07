package com.grupob.inventario.ui.fx.util;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.model.Usuario;

/**
 * Singleton estático de sesión para la UI JavaFX.
 * Workaround necesario porque JavaFX crea instancias de Application
 * internamente sin soporte de DI nativo.
 */
public final class EstadoSesion {

    private static volatile String  token;
    private static volatile Usuario usuarioActual;

    private EstadoSesion() {}

    public static void iniciar(String tk, Usuario u) {
        token         = tk;
        usuarioActual = u;
    }

    public static void cerrar() {
        token         = null;
        usuarioActual = null;
    }

    public static boolean estaActivo() { return token != null; }

    public static String  getToken()         { return token; }
    public static Usuario getUsuarioActual() { return usuarioActual; }

    public static Rol getRol() {
        return usuarioActual != null ? usuarioActual.getRol() : null;
    }
}
