package com.grupob.inventario;

import com.grupob.inventario.config.Configuracion;
import com.grupob.inventario.config.ContextoAplicacion;
import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.exception.ConfiguracionException;
import com.grupob.inventario.domain.exception.PersistenciaException;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.persistence.DataSourceFactory;
import com.grupob.inventario.persistence.EntityManagerFactoryProvider;
import com.grupob.inventario.persistence.FlywayMigrator;
import com.grupob.inventario.persistence.TransactionManager;
import com.grupob.inventario.repository.jpa.UsuarioRepositoryJpa;
import com.grupob.inventario.security.PasswordHasher;
import com.grupob.inventario.ui.cli.ConsolaPrincipal;
import com.grupob.inventario.ui.fx.AppFx;
import com.grupob.inventario.util.MensajesError;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import javafx.application.Application;

import java.util.Arrays;

/**
 * Composition root real de la aplicación.
 *
 * Flujo:
 *  1. Parsear args (--cli activa modo consola; default = JavaFX)
 *  2. Leer Configuracion (falla rápido si falta variable obligatoria)
 *  3. Crear DataSource + validar conexión
 *  4. Correr migraciones Flyway
 *  5. Crear EntityManagerFactory + TransactionManager
 *  6. Seed admin si la BD está vacía
 *  7. Ensamblar ContextoAplicacion con repos JPA
 *  8. Arrancar CLI o FX según flag
 *  9. Shutdown hook cierra EMF y DataSource ordenadamente
 */
public class App {

    public static void main(String[] args) {
        boolean modoFx = !Arrays.asList(args).contains("--cli");

        // ── 1. Configuración ─────────────────────────────────────────────
        Configuracion config;
        try {
            config = new Configuracion();
        } catch (ConfiguracionException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        // ── 2. DataSource ────────────────────────────────────────────────
        HikariDataSource ds;
        try {
            ds = DataSourceFactory.crear(config);
        } catch (PersistenciaException e) {
            System.err.println(MensajesError.ERROR_CONEXION_BD);
            System.exit(2);
            return;
        }

        // ── 3. Flyway ────────────────────────────────────────────────────
        try {
            FlywayMigrator.migrar(ds);
        } catch (PersistenciaException e) {
            System.err.println(e.getMessage());
            ds.close();
            System.exit(3);
            return;
        }

        // ── 4. JPA ───────────────────────────────────────────────────────
        EntityManagerFactory emf;
        try {
            emf = EntityManagerFactoryProvider.crear(ds, config);
        } catch (PersistenciaException e) {
            System.err.println(e.getMessage());
            ds.close();
            System.exit(4);
            return;
        }

        TransactionManager txManager = new TransactionManager(emf);

        // ── 5. Seed admin si BD vacía ────────────────────────────────────
        try {
            txManager.enTransaccion(() -> {
                UsuarioRepositoryJpa usuarioRepo = new UsuarioRepositoryJpa();
                if (usuarioRepo.listarTodos().isEmpty()) {
                    PasswordHasher hasher = new PasswordHasher();
                    String hash = hasher.hashear("Admin123!");
                    usuarioRepo.guardar(new Usuario("admin", hash, Rol.ADMINISTRADOR));
                    System.out.println("✓ Admin inicial creado: admin / Admin123!");
                }
            });
        } catch (Exception e) {
            System.err.println("Advertencia: no se pudo verificar el seed inicial — " + e.getMessage());
        }

        // ── 6. Contexto (con repos JPA) ──────────────────────────────────
        ContextoAplicacion contexto = new ContextoAplicacion(config, txManager);

        // ── 7. Shutdown hook ─────────────────────────────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { emf.close(); } catch (Exception ignored) {}
            try { ds.close();  } catch (Exception ignored) {}
        }, "shutdown-hook"));

        // ── 8. Arrancar UI ───────────────────────────────────────────────
        if (!modoFx) {
            new ConsolaPrincipal(contexto).iniciar();
        } else {
            AppFx.setContexto(contexto);
            Application.launch(AppFx.class, args);
        }
    }
}
