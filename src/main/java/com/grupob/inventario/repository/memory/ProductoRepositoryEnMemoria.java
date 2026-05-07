package com.grupob.inventario.repository.memory;

import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.repository.ProductoRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Decisión de diseño (sección 8.1 Notion): los códigos de producto son
 * case-insensitive. "P001" y "p001" se consideran el mismo código.
 * La clave del mapa se normaliza a mayúsculas; el código original del
 * Producto se conserva tal como fue creado.
 * No hay inyección SQL posible al usar HashMap (sin base de datos relacional),
 * pero los strings se almacenan tal cual sin ningún tipo de evaluación.
 */
public class ProductoRepositoryEnMemoria implements ProductoRepository {

    private final Map<String, Producto> mapa = new ConcurrentHashMap<>();

    private String clave(String codigo) {
        return Objects.requireNonNull(codigo).trim().toUpperCase();
    }

    @Override
    public void guardar(Producto producto) {
        Objects.requireNonNull(producto);
        mapa.put(clave(producto.getCodigo()), producto);
    }

    @Override
    public Optional<Producto> buscarPorCodigo(String codigo) {
        return Optional.ofNullable(mapa.get(clave(codigo)));
    }

    @Override
    public List<Producto> buscarPorNombre(String parcial) {
        String termino = parcial == null ? "" : parcial.trim().toLowerCase();
        return List.copyOf(
            mapa
                .values()
                .stream()
                .filter(Producto::isActivo)
                .filter(p -> p.getNombre().toLowerCase().contains(termino))
                .sorted(Comparator.comparing(p -> p.getNombre().toLowerCase()))
                .collect(Collectors.toList())
        );
    }

    @Override
    public List<Producto> buscarPorCategoria(String categoria) {
        Objects.requireNonNull(categoria);
        return List.copyOf(
            mapa
                .values()
                .stream()
                .filter(Producto::isActivo)
                .filter(p -> p.getCategoria().equalsIgnoreCase(categoria))
                .sorted(Comparator.comparing(p -> p.getNombre().toLowerCase()))
                .collect(Collectors.toList())
        );
    }

    @Override
    public List<Producto> listarTodos() {
        return List.copyOf(
            mapa
                .values()
                .stream()
                .sorted(Comparator.comparing(p -> p.getNombre().toLowerCase()))
                .collect(Collectors.toList())
        );
    }

    @Override
    public List<Producto> listarActivos() {
        return List.copyOf(
            mapa
                .values()
                .stream()
                .filter(Producto::isActivo)
                .sorted(Comparator.comparing(p -> p.getNombre().toLowerCase()))
                .collect(Collectors.toList())
        );
    }

    @Override
    public boolean existeCodigo(String codigo) {
        return mapa.containsKey(clave(codigo));
    }

    @Override
    public void eliminar(String codigo) {
        buscarPorCodigo(codigo).ifPresent(Producto::eliminar);
    }

    @Override
    public List<Producto> buscarCombinada(
        String codigo,
        String nombre,
        String categoria
    ) {
        String codFiltro = (codigo != null && !codigo.isBlank())
            ? codigo.trim().toUpperCase()
            : null;
        String nomFiltro = (nombre != null && !nombre.isBlank())
            ? nombre.trim().toLowerCase()
            : null;
        String catFiltro = (categoria != null && !categoria.isBlank())
            ? categoria.trim().toLowerCase()
            : null;

        return mapa
            .values()
            .stream()
            .filter(Producto::isActivo)
            .filter(
                p ->
                    codFiltro == null ||
                    p.getCodigo().toUpperCase().contains(codFiltro)
            )
            .filter(
                p ->
                    nomFiltro == null ||
                    p.getNombre().toLowerCase().contains(nomFiltro)
            )
            .filter(
                p ->
                    catFiltro == null ||
                    p.getCategoria().toLowerCase().equals(catFiltro)
            )
            .sorted(Comparator.comparing(p -> p.getNombre().toLowerCase()))
            .toList();
    }
}
