package com.grupob.inventario.repository.memory;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.repository.UsuarioRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UsuarioRepositoryEnMemoria implements UsuarioRepository {

    private final Map<String, Usuario> mapa = new ConcurrentHashMap<>();

    @Override
    public void guardar(Usuario usuario) {
        Objects.requireNonNull(usuario);
        mapa.put(usuario.getUsername(), usuario);
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        Objects.requireNonNull(username);
        return Optional.ofNullable(mapa.get(username));
    }

    @Override
    public List<Usuario> listarTodos() {
        return List.copyOf(mapa.values().stream().collect(Collectors.toList()));
    }

    @Override
    public List<Usuario> listarAdministradoresActivos() {
        return List.copyOf(mapa.values().stream()
                .filter(u -> u.isActivo() && u.getRol() == Rol.ADMINISTRADOR)
                .collect(Collectors.toList()));
    }

    @Override
    public boolean existeUsername(String username) {
        Objects.requireNonNull(username);
        return mapa.containsKey(username);
    }
}
