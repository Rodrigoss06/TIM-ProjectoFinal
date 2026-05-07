package com.grupob.inventario.repository.jpa;

import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.persistence.TransactionManager;
import com.grupob.inventario.repository.ProductoRepository;
import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ProductoRepositoryJpa implements ProductoRepository {

    @Override
    public void guardar(Producto producto) {
        Objects.requireNonNull(producto);
        EntityManager em = TransactionManager.actual();
        if (em.find(Producto.class, producto.getCodigo()) == null) {
            em.persist(producto);
        } else {
            em.merge(producto);
        }
    }

    @Override
    public Optional<Producto> buscarPorCodigo(String codigo) {
        Objects.requireNonNull(codigo);
        EntityManager em = TransactionManager.actual();
        // Los códigos se almacenan en mayúsculas; la clave ya está normalizada
        return Optional.ofNullable(
            em.find(Producto.class, codigo.trim().toUpperCase())
        );
    }

    @Override
    public List<Producto> buscarPorNombre(String parcial) {
        EntityManager em = TransactionManager.actual();
        String patron =
            "%" + (parcial == null ? "" : parcial.trim().toLowerCase()) + "%";
        return List.copyOf(
            em
                .createQuery(
                    "SELECT p FROM Producto p " +
                        "WHERE p.activo = true AND LOWER(p.nombre) LIKE :patron " +
                        "ORDER BY LOWER(p.nombre)",
                    Producto.class
                )
                .setParameter("patron", patron)
                .getResultList()
        );
    }

    @Override
    public List<Producto> buscarPorCategoria(String categoria) {
        Objects.requireNonNull(categoria);
        EntityManager em = TransactionManager.actual();
        return List.copyOf(
            em
                .createQuery(
                    "SELECT p FROM Producto p " +
                        "WHERE p.activo = true AND LOWER(p.categoria) = LOWER(:categoria) " +
                        "ORDER BY LOWER(p.nombre)",
                    Producto.class
                )
                .setParameter("categoria", categoria)
                .getResultList()
        );
    }

    @Override
    public List<Producto> listarTodos() {
        EntityManager em = TransactionManager.actual();
        return List.copyOf(
            em
                .createQuery(
                    "SELECT p FROM Producto p ORDER BY LOWER(p.nombre)",
                    Producto.class
                )
                .getResultList()
        );
    }

    @Override
    public List<Producto> listarActivos() {
        EntityManager em = TransactionManager.actual();
        return List.copyOf(
            em
                .createQuery(
                    "SELECT p FROM Producto p WHERE p.activo = true ORDER BY LOWER(p.nombre)",
                    Producto.class
                )
                .getResultList()
        );
    }

    @Override
    public boolean existeCodigo(String codigo) {
        Objects.requireNonNull(codigo);
        EntityManager em = TransactionManager.actual();
        Long count = em
            .createQuery(
                "SELECT COUNT(p) FROM Producto p WHERE UPPER(p.codigo) = :codigo",
                Long.class
            )
            .setParameter("codigo", codigo.trim().toUpperCase())
            .getSingleResult();
        return count > 0;
    }

    @Override
    public void eliminar(String codigo) {
        Objects.requireNonNull(codigo);
        EntityManager em = TransactionManager.actual();
        Producto p = em.find(Producto.class, codigo.trim().toUpperCase());
        if (p != null) p.eliminar();
        // Como entidad gestionada, el cambio se persiste en el commit
    }

    @Override
    public List<Producto> buscarCombinada(
        String codigo,
        String nombre,
        String categoria
    ) {
        EntityManager em = TransactionManager.actual();
        StringBuilder jpql = new StringBuilder(
            "SELECT p FROM Producto p WHERE p.activo = true"
        );

        boolean tieneCodigo = codigo != null && !codigo.isBlank();
        boolean tieneNombre = nombre != null && !nombre.isBlank();
        boolean tieneCategoria = categoria != null && !categoria.isBlank();

        if (tieneCodigo) jpql.append(" AND UPPER(p.codigo) LIKE :codigo");
        if (tieneNombre) jpql.append(" AND LOWER(p.nombre) LIKE :nombre");
        if (tieneCategoria) jpql.append(
            " AND LOWER(p.categoria) = LOWER(:categoria)"
        );
        jpql.append(" ORDER BY LOWER(p.nombre)");

        var query = em.createQuery(jpql.toString(), Producto.class);
        if (tieneCodigo) query.setParameter(
            "codigo",
            "%" + codigo.trim().toUpperCase() + "%"
        );
        if (tieneNombre) query.setParameter(
            "nombre",
            "%" + nombre.trim().toLowerCase() + "%"
        );
        if (tieneCategoria) query.setParameter("categoria", categoria.trim());
        return List.copyOf(query.getResultList());
    }
}
