package com.grupob.inventario.integration;

import com.grupob.inventario.App;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * IT end-to-end: levanta Postgres real vía Testcontainers, arranca App en modo CLI
 * y verifica que login + logout funcionan sin excepciones (RF-INV-006, RF-INV-008).
 *
 * La Configuracion lee DB_URL, DB_USUARIO, DB_PASSWORD como system properties
 * (fallback configurado en Configuracion.obtenerOpcional para tests).
 */
@Testcontainers
@DisplayName("AppArranqueIT — composition root completo con Postgres real")
class AppArranqueIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventario_b")
            .withUsername("inventario")
            .withPassword("inventario_dev");

    @BeforeAll
    static void configurarBD() {
        System.setProperty("DB_URL",      postgres.getJdbcUrl());
        System.setProperty("DB_USUARIO",  postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());
    }

    @AfterAll
    static void limpiarPropiedades() {
        System.clearProperty("DB_URL");
        System.clearProperty("DB_USUARIO");
        System.clearProperty("DB_PASSWORD");
    }

    /**
     * Flujo:
     *  1. BD vacía → Flyway migra el schema → seed crea admin
     *  2. CLI arranca, login como admin/Admin123!
     *  3. Opción 5 (listar) → sin filtros → inventario vacío
     *  4. Opción 7 (logout) → sesión cerrada
     *  5. Login de nuevo → EOF → ConsolaPrincipal sale → App.main retorna
     */
    @Test
    @DisplayName("CLI: BD vacía → Flyway migra → seed admin → login → listar → logout sin excepciones")
    void cliArranque_loginAdmin_listar_logout() {
        // username, password, opción 5 (listar), opción 1 (sin filtros), opción 7 (logout)
        String input = "admin\nAdmin123!\n5\n1\n7\n";

        InputStream originalIn  = System.in;
        PrintStream originalOut = System.out;
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            ByteArrayOutputStream captura = new ByteArrayOutputStream();
            System.setOut(new PrintStream(captura, true, StandardCharsets.UTF_8));

            assertThatCode(() -> App.main(new String[]{"--cli"}))
                    .doesNotThrowAnyException();

            String salida = captura.toString(StandardCharsets.UTF_8);
            assertThat(salida).contains("¡Bienvenido!");
            assertThat(salida).contains("Sesión cerrada.");
            assertThat(salida).contains("¡Hasta luego!");
            // BD vacía → no hay productos
            assertThat(salida).contains(
                    com.grupob.inventario.util.MensajesError.SIN_PRODUCTOS);

        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }
}
