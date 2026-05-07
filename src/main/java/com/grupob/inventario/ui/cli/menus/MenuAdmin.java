package com.grupob.inventario.ui.cli.menus;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.service.UsuarioService;

import java.util.List;
import java.util.Scanner;

public class MenuAdmin {

    private final UsuarioService usuarioService;

    public MenuAdmin(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public void mostrar(Rol rolEjecutor, Scanner scanner) {
        while (true) {
            System.out.println("\n=== Gestión de Usuarios ===");
            System.out.println("1. Crear usuario");
            System.out.println("2. Listar usuarios");
            System.out.println("3. Cambiar rol de usuario");
            System.out.println("4. Desactivar usuario");
            System.out.println("5. Volver");
            System.out.print("Opción: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> crearUsuario(rolEjecutor, scanner);
                case "2" -> listarUsuarios(rolEjecutor);
                case "3" -> cambiarRol(rolEjecutor, scanner);
                case "4" -> desactivarUsuario(rolEjecutor, scanner);
                case "5" -> { return; }
                default  -> System.out.println("Opción inválida.");
            }
        }
    }

    private void crearUsuario(Rol rolEjecutor, Scanner scanner) {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Contraseña (mín. 8 caracteres): ");
        String password = scanner.nextLine();
        System.out.println("Rol: 1. Administrador  2. Gestor de inventario");
        System.out.print("Opción: ");
        Rol rol = "1".equals(scanner.nextLine().trim()) ? Rol.ADMINISTRADOR : Rol.GESTOR_INVENTARIO;

        try {
            usuarioService.crearUsuario(username, password, rol, rolEjecutor);
            System.out.println("Usuario creado correctamente.");
        } catch (InventarioException e) {
            System.out.println(e.getMessage());
        }
    }

    private void listarUsuarios(Rol rolEjecutor) {
        try {
            List<Usuario> usuarios = usuarioService.listar(rolEjecutor);
            if (usuarios.isEmpty()) {
                System.out.println("No hay usuarios registrados.");
                return;
            }
            System.out.printf("%-15s %-20s %-6s%n", "USERNAME", "ROL", "ACTIVO");
            System.out.println("-".repeat(45));
            for (Usuario u : usuarios) {
                System.out.printf("%-15s %-20s %-6s%n",
                        u.getUsername(), u.getRol(), u.isActivo() ? "Sí" : "No");
            }
        } catch (InventarioException e) {
            System.out.println(e.getMessage());
        }
    }

    private void cambiarRol(Rol rolEjecutor, Scanner scanner) {
        System.out.print("Username del usuario: ");
        String username = scanner.nextLine().trim();
        System.out.println("Nuevo rol: 1. Administrador  2. Gestor de inventario");
        System.out.print("Opción: ");
        Rol nuevoRol = "1".equals(scanner.nextLine().trim()) ? Rol.ADMINISTRADOR : Rol.GESTOR_INVENTARIO;

        try {
            usuarioService.cambiarRol(username, nuevoRol, rolEjecutor);
            System.out.println("Rol actualizado. La sesión activa del usuario ha sido invalidada.");
        } catch (InventarioException e) {
            System.out.println(e.getMessage());
        }
    }

    private void desactivarUsuario(Rol rolEjecutor, Scanner scanner) {
        System.out.print("Username del usuario a desactivar: ");
        String username = scanner.nextLine().trim();
        System.out.print("¿Confirmar desactivación de '" + username + "'? (s/n): ");
        if (!"s".equalsIgnoreCase(scanner.nextLine().trim())) {
            System.out.println("Operación cancelada.");
            return;
        }
        try {
            usuarioService.desactivar(username, rolEjecutor);
            System.out.println("Usuario desactivado.");
        } catch (InventarioException e) {
            System.out.println(e.getMessage());
        }
    }
}
