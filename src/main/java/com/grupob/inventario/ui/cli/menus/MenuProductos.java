package com.grupob.inventario.ui.cli.menus;

import com.grupob.inventario.domain.enums.EstadoStock;
import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoMovimiento;
import com.grupob.inventario.domain.exception.InventarioException;
import com.grupob.inventario.domain.model.Movimiento;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.service.AutenticacionService;
import com.grupob.inventario.service.FiltroListado;
import com.grupob.inventario.service.InventarioService;
import com.grupob.inventario.service.ProductoService;
import com.grupob.inventario.service.TipoBusqueda;
import com.grupob.inventario.util.MensajesError;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class MenuProductos {

    private final ProductoService productoService;
    private final InventarioService inventarioService;
    private final AutenticacionService autenticacionService;
    private final MenuAdmin menuAdmin;

    public MenuProductos(ProductoService productoService, InventarioService inventarioService,
                          AutenticacionService autenticacionService, MenuAdmin menuAdmin) {
        this.productoService = productoService;
        this.inventarioService = inventarioService;
        this.autenticacionService = autenticacionService;
        this.menuAdmin = menuAdmin;
    }

    /** Retorna true si el usuario eligió cerrar sesión. */
    public boolean mostrar(String token, Rol rol, Scanner scanner) {
        while (true) {
            System.out.println("\n=== Menú Principal ===");
            System.out.println("1. Registrar producto");
            System.out.println("2. Buscar producto");
            System.out.println("3. Actualizar stock");
            System.out.println("4. Eliminar producto");
            System.out.println("5. Listar productos");
            System.out.println("6. Ver historial de movimientos");
            if (rol == Rol.ADMINISTRADOR) {
                System.out.println("8. Gestión de usuarios");
            }
            System.out.println("7. Cerrar sesión");
            System.out.print("Opción: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> registrarProducto(rol, scanner);
                case "2" -> buscarProducto(rol, scanner);
                case "3" -> actualizarStock(rol, scanner);
                case "4" -> eliminarProducto(rol, scanner);
                case "5" -> listarProductos(rol, scanner);
                case "6" -> verHistorial(scanner);
                case "7" -> {
                    autenticacionService.logout(token);
                    System.out.println("Sesión cerrada.");
                    return true;
                }
                case "8" -> {
                    if (rol == Rol.ADMINISTRADOR) {
                        menuAdmin.mostrar(rol, scanner);
                    } else {
                        System.out.println("Opción inválida.");
                    }
                }
                default -> System.out.println("Opción inválida. Ingrese un número del menú.");
            }
        }
    }

    private void registrarProducto(Rol rol, Scanner scanner) {
        try {
            System.out.print("Código: ");
            String codigo = scanner.nextLine().trim();
            System.out.print("Nombre: ");
            String nombre = scanner.nextLine().trim();
            System.out.print("Descripción (opcional): ");
            String descripcion = scanner.nextLine().trim();
            System.out.print("Categoría: ");
            String categoria = scanner.nextLine().trim();
            System.out.print("Precio unitario (ej. 25.50): ");
            BigDecimal precio = parseBigDecimal(scanner.nextLine().trim());
            if (precio == null) return;
            System.out.print("Stock inicial: ");
            int stock = parseEntero(scanner.nextLine().trim());
            if (stock < 0) { System.out.println("Stock inválido."); return; }

            Producto p = new Producto(codigo, nombre, descripcion, categoria, precio, stock);
            productoService.registrar(p, rol);
            System.out.println("Producto registrado correctamente.");
        } catch (InventarioException e) {
            System.out.println(e.getMessage());
        }
    }

    private void buscarProducto(Rol rol, Scanner scanner) {
        System.out.println("Buscar por: 1. Código  2. Nombre  3. Categoría");
        System.out.print("Opción: ");
        String op = scanner.nextLine().trim();
        TipoBusqueda tipo = switch (op) {
            case "1" -> TipoBusqueda.CODIGO;
            case "3" -> TipoBusqueda.CATEGORIA;
            default  -> TipoBusqueda.NOMBRE;
        };

        System.out.print("Criterio de búsqueda (ENTER = todos): ");
        String criterio = scanner.nextLine();

        try {
            List<Producto> resultados = productoService.buscar(criterio, tipo, rol);
            if (resultados.isEmpty()) {
                System.out.println(MensajesError.SIN_RESULTADOS_BUSQUEDA);
            } else {
                imprimirTabla(resultados);
            }
        } catch (InventarioException e) {
            System.out.println(e.getMessage());
        }
    }

    private void actualizarStock(Rol rol, Scanner scanner) {
        System.out.print("Código del producto: ");
        String codigo = scanner.nextLine().trim();
        System.out.println("Tipo: 1. Entrada  2. Salida");
        System.out.print("Opción: ");
        TipoMovimiento tipo = "2".equals(scanner.nextLine().trim())
                ? TipoMovimiento.SALIDA : TipoMovimiento.ENTRADA;
        System.out.print("Cantidad: ");
        int cantidad = parseEntero(scanner.nextLine().trim());
        if (cantidad < 0) { System.out.println("Cantidad inválida."); return; }

        try {
            inventarioService.actualizarStock(codigo, tipo, cantidad, rol);
            productoService.buscar(codigo, TipoBusqueda.CODIGO, rol)
                    .stream().findFirst()
                    .ifPresent(p -> System.out.printf("Stock actualizado. Nuevo stock: %d unidades.%n",
                            p.getStock()));
        } catch (InventarioException e) {
            System.out.println(e.getMessage());
        }
    }

    private void eliminarProducto(Rol rol, Scanner scanner) {
        System.out.print("Código del producto a eliminar: ");
        String codigo = scanner.nextLine().trim();
        if (codigo.isEmpty()) { System.out.println("Código requerido."); return; }

        try {
            productoService.buscar(codigo, TipoBusqueda.CODIGO, rol)
                    .stream().findFirst()
                    .ifPresent(p -> System.out.printf("Producto: [%s] %s — Stock: %d — Precio: %.2f%n",
                            p.getCodigo(), p.getNombre(), p.getStock(),
                            p.getPrecioUnitario().doubleValue()));
        } catch (InventarioException ignored) {}

        if (productoService.tieneMovimientosRecientes(codigo)) {
            System.out.println(MensajesError.MOVIMIENTOS_RECIENTES_ADVERTENCIA);
        }

        System.out.print("¿Confirmar eliminación? (s/n): ");
        if (!"s".equalsIgnoreCase(scanner.nextLine().trim())) {
            System.out.println("Operación cancelada.");
            return;
        }

        try {
            productoService.eliminar(codigo, rol);
            System.out.println("Producto eliminado correctamente.");
        } catch (InventarioException e) {
            System.out.println(e.getMessage());
        }
    }

    private void listarProductos(Rol rol, Scanner scanner) {
        System.out.println("1. Sin filtros  2. Con filtros");
        System.out.print("Opción: ");
        boolean conFiltros = "2".equals(scanner.nextLine().trim());

        FiltroListado filtro;
        if (conFiltros) {
            System.out.print("Categoría (ENTER = todas): ");
            String cat = scanner.nextLine().trim();
            System.out.print("Precio mínimo (ENTER = sin límite): ");
            BigDecimal pMin = parseBigDecimal(scanner.nextLine().trim());
            System.out.print("Precio máximo (ENTER = sin límite): ");
            BigDecimal pMax = parseBigDecimal(scanner.nextLine().trim());
            System.out.println("Estado stock: 0=Todos  1=Sin stock  2=Bajo  3=Normal");
            System.out.print("Opción: ");
            EstadoStock estado = parseEstadoStock(scanner.nextLine().trim());
            try {
                filtro = new FiltroListado(cat.isEmpty() ? null : cat, pMin, pMax, estado);
            } catch (InventarioException e) {
                System.out.println(e.getMessage());
                return;
            }
        } else {
            filtro = new FiltroListado(null, null, null, null);
        }

        try {
            List<Producto> lista = productoService.listar(filtro, rol);
            if (lista.isEmpty()) {
                System.out.println(conFiltros ? MensajesError.SIN_RESULTADOS_FILTRO : MensajesError.SIN_PRODUCTOS);
            } else {
                imprimirTabla(lista);
            }
        } catch (InventarioException e) {
            System.out.println(e.getMessage());
        }
    }

    private void verHistorial(Scanner scanner) {
        System.out.print("Código del producto: ");
        String codigo = scanner.nextLine().trim();

        List<Movimiento> movimientos = inventarioService.historialDe(codigo);
        if (movimientos.isEmpty()) {
            System.out.println("Sin movimientos registrados para ese código.");
            return;
        }
        System.out.printf("%-22s %-10s %10s %12s %12s%n", "FECHA", "TIPO", "CANTIDAD", "ANT.", "NUEVO");
        System.out.println("-".repeat(70));
        for (Movimiento m : movimientos) {
            System.out.printf("%-22s %-10s %10d %12d %12d%n",
                    m.fecha().toString().replace('T', ' ').substring(0, 19),
                    m.tipo(), m.cantidad(), m.stockAnterior(), m.stockNuevo());
        }
    }

    private void imprimirTabla(List<Producto> productos) {
        String fmt = "%-10s %-24s %-12s %10s %8s %12s%n";
        System.out.printf(fmt, "CÓDIGO", "NOMBRE", "CATEGORÍA", "PRECIO", "STOCK", "ESTADO");
        System.out.println("-".repeat(82));
        for (Producto p : productos) {
            String estado = switch (EstadoStock.desde(p.getStock())) {
                case SIN_STOCK -> "[SIN STOCK]";
                case BAJO      -> "[BAJO]";
                case NORMAL    -> "";
            };
            System.out.printf(fmt, p.getCodigo(), truncar(p.getNombre(), 24),
                    truncar(p.getCategoria(), 12), p.getPrecioUnitario().toPlainString(),
                    p.getStock(), estado);
        }
        System.out.println("-".repeat(82));
        System.out.printf("Total: %d productos%n", productos.size());
    }

    private BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return new BigDecimal(s); } catch (NumberFormatException e) {
            System.out.println("Valor numérico inválido, se ignorará ese filtro."); return null;
        }
    }

    private int parseEntero(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) {
            System.out.println("Debe ingresar un número entero."); return -1;
        }
    }

    private EstadoStock parseEstadoStock(String s) {
        return switch (s.trim()) {
            case "1" -> EstadoStock.SIN_STOCK;
            case "2" -> EstadoStock.BAJO;
            case "3" -> EstadoStock.NORMAL;
            default  -> null;
        };
    }

    private String truncar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
