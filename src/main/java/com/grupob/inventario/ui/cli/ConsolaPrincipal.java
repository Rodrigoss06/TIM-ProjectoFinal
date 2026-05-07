package com.grupob.inventario.ui.cli;

import com.grupob.inventario.config.ContextoAplicacion;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.service.AutenticacionService;
import com.grupob.inventario.service.InventarioService;
import com.grupob.inventario.service.ProductoService;
import com.grupob.inventario.service.UsuarioService;
import com.grupob.inventario.ui.cli.menus.MenuAdmin;
import com.grupob.inventario.ui.cli.menus.MenuLogin;
import com.grupob.inventario.ui.cli.menus.MenuProductos;
import com.grupob.inventario.util.MensajesError;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ConsolaPrincipal {

    private final AutenticacionService autenticacionService;
    private final MenuLogin            menuLogin;
    private final MenuProductos        menuProductos;

    /** Constructor para App.java (composition root). */
    public ConsolaPrincipal(ContextoAplicacion contexto) {
        this(contexto.getAutenticacionService(),
             contexto.getProductoService(),
             contexto.getInventarioService(),
             contexto.getUsuarioService());
    }

    /** Constructor para tests (servicios individuales). */
    public ConsolaPrincipal(AutenticacionService autenticacion,
                             ProductoService productos,
                             InventarioService inventario,
                             UsuarioService usuario) {
        this.autenticacionService = autenticacion;
        MenuAdmin menuAdmin = new MenuAdmin(usuario);
        this.menuProductos = new MenuProductos(productos, inventario, autenticacion, menuAdmin);
        this.menuLogin     = new MenuLogin(autenticacion);
    }

    public void iniciar() {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   SISTEMA DE INVENTARIO — GRUPO B    ║");
        System.out.println("╚══════════════════════════════════════╝");

        while (true) {
            try {
                String token  = menuLogin.mostrar(scanner);
                Usuario usuario = autenticacionService.usuarioActual(token);

                boolean logout = false;
                while (!logout) {
                    try {
                        logout = menuProductos.mostrar(token, usuario.getRol(), scanner);
                    } catch (InventarioException e) {
                        System.out.println(e.getMessage());
                        if (MensajesError.SESION_EXPIRADA.equals(e.getMessage())) logout = true;
                    } catch (Exception e) {
                        System.out.println("Error inesperado: " + e.getMessage());
                    }
                }
            } catch (NoSuchElementException e) {
                break;
            } catch (InventarioException e) {
                System.out.println(e.getMessage());
            } catch (Exception e) {
                System.out.println("Error inesperado: " + e.getMessage());
            }
        }

        System.out.println("¡Hasta luego!");
    }
}
