package com.grupob.inventario.repository.jpa;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.persistence.TransactionManager;
import com.grupob.inventario.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class UsuarioRepositoryJpa implements UsuarioRepository {

    @Override
    public void guardar(Usuario usuario) {
        Objects.requireNonNull(usuario);
        EntityManager em = TransactionManager.actual();
        if (em.find(Usuario.class, usuario.getUsername()) == null) {
            em.persist(usuario);
        } else {
            em.merge(usuario);
        }
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        Objects.requireNonNull(username);
        return Optional.ofNullable(
            TransactionManager.actual().find(Usuario.class, username));
    }

    @Override
    public List<Usuario> listarTodos() {
        return List.copyOf(
            TransactionManager.actual()
                .createQuery("SELECT u FROM Usuario u", Usuario.class)
                .getResultList());
    }

    @Override
    public List<Usuario> listarAdministradoresActivos() {
        return List.copyOf(
            TransactionManager.actual()
                .createQuery(
                    "SELECT u FROM Usuario u WHERE u.rol = :rol AND u.activo = true",
                    Usuario.class)
                .setParameter("rol", Rol.ADMINISTRADOR)
                .getResultList());
    }

    @Override
    public boolean existeUsername(String username) {
        Objects.requireNonNull(username);
        Long count = TransactionManager.actual()
            .createQuery("SELECT COUNT(u) FROM Usuario u WHERE u.username = :username", Long.class)
            .setParameter("username", username)
            .getSingleResult();
        return count > 0;
    }
}
