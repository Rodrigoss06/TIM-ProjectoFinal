package com.grupob.inventario.repository;

import com.grupob.inventario.domain.model.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository {

    void guardar(Producto producto);

    Optional<Producto> buscarPorCodigo(String codigo);

    List<Producto> buscarPorNombre(String parcial);

    List<Producto> buscarPorCategoria(String categoria);

    List<Producto> listarTodos();

    List<Producto> listarActivos();

    boolean existeCodigo(String codigo);

    void eliminar(String codigo);
}
