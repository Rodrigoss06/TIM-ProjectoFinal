package com.grupob.inventario.repository.jpa;

import com.grupob.inventario.domain.model.EventoAuditoria;
import com.grupob.inventario.persistence.TransactionManager;
import com.grupob.inventario.repository.AuditoriaRepository;
import com.grupob.inventario.repository.FiltroAuditoria;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AuditoriaRepositoryJpa implements AuditoriaRepository {

    @Override
    public void registrar(EventoAuditoria evento) {
        Objects.requireNonNull(evento);
        TransactionManager.actual().persist(evento);
    }

    @Override
    public List<EventoAuditoria> consultar(FiltroAuditoria filtro, int pagina, int tamanoPagina) {
        Objects.requireNonNull(filtro);
        EntityManager em = TransactionManager.actual();

        // JPQL dinámico — filtros se combinan con AND (sección 2 RF-INV-009 Notion)
        StringBuilder jpql = new StringBuilder(
                "SELECT e FROM EventoAuditoria e WHERE 1=1");
        Map<String, Object> params = new LinkedHashMap<>();

        if (filtro.desde() != null) {
            jpql.append(" AND e.fecha >= :desde");
            params.put("desde", filtro.desde());
        }
        if (filtro.hasta() != null) {
            jpql.append(" AND e.fecha <= :hasta");
            params.put("hasta", filtro.hasta());
        }
        if (filtro.username() != null && !filtro.username().isBlank()) {
            jpql.append(" AND e.username = :username");
            params.put("username", filtro.username());
        }
        if (filtro.tipoEvento() != null) {
            jpql.append(" AND e.tipoEvento = :tipoEvento");
            params.put("tipoEvento", filtro.tipoEvento());
        }
        jpql.append(" ORDER BY e.fecha DESC");

        TypedQuery<EventoAuditoria> query =
                em.createQuery(jpql.toString(), EventoAuditoria.class);
        params.forEach(query::setParameter);
        query.setFirstResult(pagina * tamanoPagina);
        query.setMaxResults(tamanoPagina);

        return List.copyOf(query.getResultList());
    }
}
