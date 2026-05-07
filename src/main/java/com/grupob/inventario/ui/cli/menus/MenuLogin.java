package com.grupob.inventario.ui.cli.menus;

import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.service.AutenticacionService;

import java.util.Arrays;
import java.util.Scanner;

public class MenuLogin {

    private final AutenticacionService autenticacionService;

    public MenuLogin(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    public String mostrar(Scanner scanner) {
        while (true) {
            System.out.println("\n--- Iniciar Sesión ---");
            System.out.print("Usuario: ");
            String username = scanner.nextLine().trim();

            String password = leerPassword(scanner);

            try {
                String token = autenticacionService.login(username, password);
                System.out.println("¡Bienvenido!");
                return token;
            } catch (InventarioException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private String leerPassword(Scanner scanner) {
        java.io.Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword("Contraseña: ");
            if (pwd == null) return "";
            String result = new String(pwd);
            Arrays.fill(pwd, '\0');
            return result;
        }
        System.out.print("Contraseña: ");
        return scanner.nextLine();
    }
}
