package com.grupob.inventario.service;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.exception.ProductoNoEncontradoException;
import com.grupob.inventario.domain.model.Movimiento;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.persistence.GestorTransacciones;
import com.grupob.inventario.repository.MovimientoRepository;
import com.grupob.inventario.repository.ProductoRepository;
import com.grupob.inventario.security.PermisoChecker;
import com.grupob.inventario.validation.StockValidator;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class InventarioService {

    private final ProductoRepository productoRepo;
    private final MovimientoRepository movimientoRepo;
    private final StockValidator stockValidator;
    private final PermisoChecker permisoChecker;
    private final Clock clock;
    private final GestorTransacciones txManager;
    private final AuditoriaService auditoriaService;

    public InventarioService(ProductoRepository productoRepo, MovimientoRepository movimientoRepo,
                              StockValidator stockValidator, PermisoChecker permisoChecker, Clock clock,
                              GestorTransacciones txManager, AuditoriaService auditoriaService) {
        this.productoRepo     = Objects.requireNonNull(productoRepo);
        this.movimientoRepo   = Objects.requireNonNull(movimientoRepo);
        this.stockValidator   = Objects.requireNonNull(stockValidator);
        this.permisoChecker   = Objects.requireNonNull(permisoChecker);
        this.clock            = Objects.requireNonNull(clock);
        this.txManager        = Objects.requireNonNull(txManager);
        this.auditoriaService = Objects.requireNonNull(auditoriaService);
    }

    public void actualizarStock(String codigo, TipoMovimiento tipo, int cantidad, Rol rolUsuario) {
        permisoChecker.requierePermiso(rolUsuario, PermisoChecker.Accion.ACTUALIZAR_STOCK);
        Objects.requireNonNull(codigo, "codigo no puede ser null");
        Objects.requireNonNull(tipo, "tipo no puede ser null");

        txManager.enTransaccion(() -> {
            Producto producto = productoRepo.buscarPorCodigo(codigo)
                    .filter(Producto::isActivo)
                    .orElseThrow(ProductoNoEncontradoException::new);

            stockValidator.validarMovimiento(cantidad, producto.getStock(), tipo);

            int stockAnterior = producto.getStock();
            if (tipo == TipoMovimiento.ENTRADA) {
                producto.incrementarStock(cantidad);
            } else {
                producto.decrementarStock(cantidad);
            }
            productoRepo.guardar(producto);

            movimientoRepo.registrar(new Movimiento(
                    producto.getCodigo(), tipo, cantidad,
                    stockAnterior, producto.getStock(), Instant.now(clock)));

            auditoriaService.registrar(rolUsuario.name(), TipoEvento.ACTUALIZAR_STOCK,
                    codigo, String.format("stock %d → %d", stockAnterior, producto.getStock()));
        });
    }

    public List<Movimiento> historialDe(String codigo) {
        Objects.requireNonNull(codigo, "codigo no puede ser null");
        return txManager.soloLectura(() -> movimientoRepo.historialDe(codigo));
    }
    public List<Movimiento> consultarMovimientos(String codigoProducto, TipoMovimiento tipo,
                                              Instant desde, Instant hasta, Rol rolUsuario) {
    permisoChecker.requierePermiso(rolUsuario, PermisoChecker.Accion.ACTUALIZAR_STOCK);
    return txManager.soloLectura(() -> movimientoRepo.consultar(codigoProducto, tipo, desde, hasta));
}
}
