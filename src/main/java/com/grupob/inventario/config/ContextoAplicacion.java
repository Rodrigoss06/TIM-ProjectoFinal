package com.grupob.inventario.config;

import com.grupob.inventario.persistence.GestorTransacciones;
import com.grupob.inventario.persistence.TransactionManager;
import com.grupob.inventario.repository.jpa.AuditoriaRepositoryJpa;
import com.grupob.inventario.repository.jpa.MovimientoRepositoryJpa;
import com.grupob.inventario.repository.jpa.ProductoRepositoryJpa;
import com.grupob.inventario.repository.jpa.UsuarioRepositoryJpa;
import com.grupob.inventario.repository.memory.AuditoriaRepositoryEnMemoria;
import com.grupob.inventario.repository.memory.MovimientoRepositoryEnMemoria;
import com.grupob.inventario.repository.memory.ProductoRepositoryEnMemoria;
import com.grupob.inventario.repository.memory.UsuarioRepositoryEnMemoria;
import com.grupob.inventario.security.PasswordHasher;
import com.grupob.inventario.security.PermisoChecker;
import com.grupob.inventario.security.SesionManager;
import com.grupob.inventario.service.AuditoriaService;
import com.grupob.inventario.service.AutenticacionService;
import com.grupob.inventario.service.InventarioService;
import com.grupob.inventario.service.ProductoService;
import com.grupob.inventario.service.UsuarioService;
import com.grupob.inventario.util.Seeder;
import com.grupob.inventario.validation.ProductoValidator;
import com.grupob.inventario.validation.StockValidator;
import com.grupob.inventario.validation.UsuarioValidator;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Supplier;

public class ContextoAplicacion {

    private final Configuracion        configuracion;
    private final Clock                clock;
    private final PasswordHasher       passwordHasher;
    private final PermisoChecker       permisoChecker;
    private final SesionManager        sesionManager;
    private final ProductoValidator    productoValidator;
    private final StockValidator       stockValidator;
    private final UsuarioValidator     usuarioValidator;
    private final ProductoService      productoService;
    private final InventarioService    inventarioService;
    private final AutenticacionService autenticacionService;
    private final UsuarioService       usuarioService;
    private final AuditoriaService     auditoriaService;

    /**
     * Constructor con JPA real — usado por App.java en producción.
     * Recibe el TransactionManager ya inicializado (DataSource + EMF construidos antes).
     */
    public ContextoAplicacion(Configuracion configuracion, TransactionManager txManager) {
        this.configuracion = Objects.requireNonNull(configuracion);
        this.clock         = Clock.systemDefaultZone();

        this.passwordHasher    = new PasswordHasher();
        this.permisoChecker    = new PermisoChecker();
        this.sesionManager     = new SesionManager(clock);
        this.productoValidator = new ProductoValidator();
        this.stockValidator    = new StockValidator();
        this.usuarioValidator  = new UsuarioValidator();

        var productoRepo   = new ProductoRepositoryJpa();
        var movimientoRepo = new MovimientoRepositoryJpa();
        var usuarioRepo    = new UsuarioRepositoryJpa();
        var auditoriaRepo  = new AuditoriaRepositoryJpa();

        this.auditoriaService = new AuditoriaService(auditoriaRepo, permisoChecker, clock,
                configuracion.getAuditoriaPaginaTamano(), txManager);

        this.productoService = new ProductoService(productoRepo, movimientoRepo,
                productoValidator, permisoChecker, clock, txManager, auditoriaService);
        this.inventarioService = new InventarioService(productoRepo, movimientoRepo,
                stockValidator, permisoChecker, clock, txManager, auditoriaService);
        this.autenticacionService = new AutenticacionService(usuarioRepo, passwordHasher,
                sesionManager, clock, txManager, auditoriaService);
        this.usuarioService = new UsuarioService(usuarioRepo, passwordHasher,
                usuarioValidator, permisoChecker, sesionManager, txManager, auditoriaService);
    }

    /**
     * Constructor en memoria (no-op TM) — para tests unitarios y modo sin BD.
     * No hace seed: el caller es responsable de poblar datos si los necesita.
     */
    public ContextoAplicacion(Configuracion configuracion) {
        this.configuracion = Objects.requireNonNull(configuracion);
        this.clock         = Clock.systemDefaultZone();

        GestorTransacciones txNoOp = new GestorTransacciones() {
            @Override public <T> T enTransaccion(Supplier<T> t) { return t.get(); }
            @Override public void enTransaccion(Runnable r)     { r.run(); }
            @Override public <T> T soloLectura(Supplier<T> t)  { return t.get(); }
        };

        this.passwordHasher    = new PasswordHasher();
        this.permisoChecker    = new PermisoChecker();
        this.sesionManager     = new SesionManager(clock);
        this.productoValidator = new ProductoValidator();
        this.stockValidator    = new StockValidator();
        this.usuarioValidator  = new UsuarioValidator();

        var productoRepo   = new ProductoRepositoryEnMemoria();
        var movimientoRepo = new MovimientoRepositoryEnMemoria();
        var usuarioRepo    = new UsuarioRepositoryEnMemoria();
        var auditoriaRepo  = new AuditoriaRepositoryEnMemoria();

        this.auditoriaService = new AuditoriaService(auditoriaRepo, permisoChecker, clock,
                configuracion.getAuditoriaPaginaTamano(), txNoOp);

        this.productoService = new ProductoService(productoRepo, movimientoRepo,
                productoValidator, permisoChecker, clock, txNoOp, auditoriaService);
        this.inventarioService = new InventarioService(productoRepo, movimientoRepo,
                stockValidator, permisoChecker, clock, txNoOp, auditoriaService);
        this.autenticacionService = new AutenticacionService(usuarioRepo, passwordHasher,
                sesionManager, clock, txNoOp, auditoriaService);
        this.usuarioService = new UsuarioService(usuarioRepo, passwordHasher,
                usuarioValidator, permisoChecker, sesionManager, txNoOp, auditoriaService);

        // Datos de prueba seed (sección 7.4 Notion)
        Seeder.sembrar(usuarioRepo, productoRepo, passwordHasher);
    }

    public Configuracion           getConfiguracion()        { return configuracion; }
    public Clock                   getClock()                { return clock; }
    public PasswordHasher          getPasswordHasher()       { return passwordHasher; }
    public PermisoChecker          getPermisoChecker()       { return permisoChecker; }
    public SesionManager           getSesionManager()        { return sesionManager; }
    public ProductoValidator       getProductoValidator()    { return productoValidator; }
    public StockValidator          getStockValidator()       { return stockValidator; }
    public UsuarioValidator        getUsuarioValidator()     { return usuarioValidator; }
    public ProductoService         getProductoService()      { return productoService; }
    public InventarioService       getInventarioService()    { return inventarioService; }
    public AutenticacionService    getAutenticacionService() { return autenticacionService; }
    public UsuarioService          getUsuarioService()       { return usuarioService; }
    public AuditoriaService        getAuditoriaService()     { return auditoriaService; }
}
