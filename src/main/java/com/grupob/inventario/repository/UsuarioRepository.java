package com.grupob.inventario.repository;

import com.grupob.inventario.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {

    void guardar(Usuario usuario);

    Optional<Usuario> buscarPorUsername(String username);

    List<Usuario> listarTodos();

    List<Usuario> listarAdministradoresActivos();

    boolean existeUsername(String username);
}
