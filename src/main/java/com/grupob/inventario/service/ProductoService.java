package com.grupob.inventario.service;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.exception.ProductoDuplicadoException;
import com.grupob.inventario.domain.exception.ProductoNoEncontradoException;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.persistence.GestorTransacciones;
import com.grupob.inventario.repository.MovimientoRepository;
import com.grupob.inventario.repository.ProductoRepository;
import com.grupob.inventario.security.PermisoChecker;
import com.grupob.inventario.validation.ProductoValidator;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class ProductoService {

    private static final Duration VENTANA_RECIENTE = Duration.ofDays(7);

    private final ProductoRepository productoRepo;
    private final MovimientoRepository movimientoRepo;
    private final ProductoValidator validator;
    private final PermisoChecker permisoChecker;
    private final Clock clock;
    private final GestorTransacciones txManager;
    private final AuditoriaService auditoriaService;

    public ProductoService(ProductoRepository productoRepo, MovimientoRepository movimientoRepo,
                           ProductoValidator validator, PermisoChecker permisoChecker, Clock clock,
                           GestorTransacciones txManager, AuditoriaService auditoriaService) {
        this.productoRepo    = Objects.requireNonNull(productoRepo);
        this.movimientoRepo  = Objects.requireNonNull(movimientoRepo);
        this.validator       = Objects.requireNonNull(validator);
        this.permisoChecker  = Objects.requireNonNull(permisoChecker);
        this.clock           = Objects.requireNonNull(clock);
        this.txManager       = Objects.requireNonNull(txManager);
        this.auditoriaService = Objects.requireNonNull(auditoriaService);
    }

    public void registrar(Producto producto, Rol rolUsuario) {
        permisoChecker.requierePermiso(rolUsuario, PermisoChecker.Accion.REGISTRAR_PRODUCTO);
        Objects.requireNonNull(producto, "producto no puede ser null");
        validator.validarRegistro(producto);

        txManager.enTransaccion(() -> {
            if (productoRepo.existeCodigo(producto.getCodigo())) {
                throw new ProductoDuplicadoException();
            }
            productoRepo.guardar(producto);
            auditoriaService.registrar(rolUsuario.name(), TipoEvento.CREAR_PRODUCTO,
                    producto.getCodigo(), "alta inicial");
        });
    }

    public List<Producto> buscar(String criterio, TipoBusqueda tipo, Rol rolUsuario) {
        permisoChecker.requierePermiso(rolUsuario, PermisoChecker.Accion.BUSCAR_PRODUCTO);
        Objects.requireNonNull(tipo, "tipo no puede ser null");

        if (criterio == null || criterio.isBlank()) {
            return txManager.soloLectura(() -> productoRepo.listarActivos());
        }

        String termino = criterio.trim();
        return txManager.soloLectura(() -> switch (tipo) {
            case CODIGO -> productoRepo.buscarPorCodigo(termino)
                    .filter(Producto::isActivo)
                    .map(p -> List.of(p))
                    .orElse(List.of());
            case NOMBRE    -> productoRepo.buscarPorNombre(termino);
            case CATEGORIA -> productoRepo.buscarPorCategoria(termino);
        });
    }

    public void eliminar(String codigo, Rol rolUsuario) {
        permisoChecker.requierePermiso(rolUsuario, PermisoChecker.Accion.ELIMINAR_PRODUCTO);
        Objects.requireNonNull(codigo, "codigo no puede ser null");

        txManager.enTransaccion(() -> {
            Producto producto = productoRepo.buscarPorCodigo(codigo)
                    .filter(Producto::isActivo)
                    .orElseThrow(ProductoNoEncontradoException::new);
            producto.eliminar();
            productoRepo.guardar(producto);
            auditoriaService.registrar(rolUsuario.name(), TipoEvento.ELIMINAR_PRODUCTO,
                    codigo, "eliminación lógica");
        });
    }

    public List<Producto> listar(FiltroListado filtro, Rol rolUsuario) {
        permisoChecker.requierePermiso(rolUsuario, PermisoChecker.Accion.LISTAR_PRODUCTO);
        Objects.requireNonNull(filtro, "filtro no puede ser null");
        return txManager.soloLectura(() -> filtro.aplicarA(productoRepo.listarActivos()));
    }

    /**
     * Actualiza los campos mutables del producto (nombre, descripción, categoría, precio).
     * NO modifica código ni stock — para stock usar InventarioService.actualizarStock.
     */
    public void actualizar(Producto producto, Rol rolUsuario) {
        permisoChecker.requierePermiso(rolUsuario, PermisoChecker.Accion.REGISTRAR_PRODUCTO);
        Objects.requireNonNull(producto);
        validator.validarActualizacion(producto);

        txManager.enTransaccion(() -> {
            Producto existente = productoRepo.buscarPorCodigo(producto.getCodigo())
                    .filter(Producto::isActivo)
                    .orElseThrow(ProductoNoEncontradoException::new);
            existente.setNombre(producto.getNombre());
            existente.setDescripcion(producto.getDescripcion());
            existente.setCategoria(producto.getCategoria());
            existente.setPrecioUnitario(producto.getPrecioUnitario());
            productoRepo.guardar(existente);
            auditoriaService.registrar(rolUsuario.name(), TipoEvento.ACTUALIZAR_PRODUCTO,
                    producto.getCodigo(), "actualización de datos");
        });
    }

    public boolean tieneMovimientosRecientes(String codigo) {
        Objects.requireNonNull(codigo, "codigo no puede ser null");
        return txManager.soloLectura(() ->
                movimientoRepo.tieneMovimientosRecientes(codigo, VENTANA_RECIENTE, clock));
    }
}
